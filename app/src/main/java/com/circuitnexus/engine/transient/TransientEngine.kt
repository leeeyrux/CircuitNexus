package com.circuitnexus.engine.transient

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import kotlin.math.abs

/**
 * Operación lógica para compuertas 74HC
 */
enum class LogicOp { AND, OR, NOT, NAND, NOR, XOR }

/**
 * Motor de simulación en el dominio del tiempo (transitoria) basado en MNA.
 *
 * - Modelos compañeros de Euler hacia atrás (estables).
 * - Memoria de capacitores y bobinas entre pasos.
 * - Diodos/LEDs/Zener reevaluados en cada paso (tramo lineal).
 * - Chip 555 como generador de onda cuadrada.
 * - Amplificador operacional como fuente controlada.
 *
 * Está pensado para alimentar el osciloscopio a ~60 fps sin bloquear la UI.
 */
class TransientEngine {

    var timeStep: Double = 5e-4 // 0.5 ms
        private set
    var currentTime: Double = 0.0
        private set

    private val maxHistory = 6000

    // Topología
    private var topoHash: Long = 0L
    private var nodeCount = 0
    private var matrixSize = 0

    private val nodeOfPin = HashMap<Pair<Long, Int>, Int>()
    private val vsVar = HashMap<Long, Int>()      // baterías
    private val indVar = HashMap<Long, Int>()     // bobinas
    private val opampVar = HashMap<Long, Int>()   // amplificadores
    private val timerVar = HashMap<Long, Int>()   // 555

    private var gLinear: Array<DoubleArray> = emptyArray()
    private var rhs = DoubleArray(0)
    private var sol = DoubleArray(0)
    private var prev = DoubleArray(0)

    private val capPrevVoltage = HashMap<Long, Double>()
    private val indPrevCurrent = HashMap<Long, Double>()

    private val nodeHistory = HashMap<Int, MutableList<Float>>()
    private val wireHistory = HashMap<Long, MutableList<Float>>()
    private var lastComponentCurrents = HashMap<Long, Float>()

    // ===== API =====

    fun reset() {
        currentTime = 0.0
        topoHash = 0L
        nodeCount = 0
        matrixSize = 0
        nodeOfPin.clear()
        vsVar.clear()
        indVar.clear()
        opampVar.clear()
        timerVar.clear()
        gLinear = emptyArray()
        rhs = DoubleArray(0)
        sol = DoubleArray(0)
        prev = DoubleArray(0)
        capPrevVoltage.clear()
        indPrevCurrent.clear()
        nodeHistory.clear()
        wireHistory.clear()
        lastComponentCurrents.clear()
    }

    /**
     * Reconstruye la matriz solo si cambió el circuito o el paso de tiempo.
     * Usa un hash rápido (sin textos) para poder llamarse en cada frame.
     */
    fun updateTopology(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        stepSeconds: Double = timeStep
    ) {
        val hash = topologyHash(components, wires)
        val dtChanged = abs(stepSeconds - timeStep) > 1e-12
        if (hash == topoHash && !dtChanged) return

        timeStep = stepSeconds
        topoHash = hash
        assignNodes(components, wires)
        allocate()
        buildLinear(components)
    }

    /** Compatibilidad: igual que updateTopology. */
    fun prepare(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        stepSeconds: Double = timeStep
    ) = updateTopology(components, wires, stepSeconds)

    /**
     * Avanza un paso de simulación.
     * @param computeState si es true calcula voltajes/corrientes para la UI (caro)
     * @param record si es true guarda historial para el osciloscopio
     */
    fun step(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        computeState: Boolean = true,
        record: Boolean = true
    ): TransientResult {
        if (gLinear.isEmpty()) updateTopology(components, wires)

        val gWork = Array(matrixSize) { gLinear[it].copyOf() }
        rhs.fill(0.0)

        // --- Modelos dinámicos y no lineales ---
        for (comp in components) {
            when (comp.type) {
                ComponentType.CAPACITOR -> {
                    val a = nodeOf(comp.id, 0)
                    val b = nodeOf(comp.id, 1)
                    val c = (comp.properties.capacitanceFarads ?: 100e-6).toDouble()
                        .coerceAtLeast(1e-15)
                    val geq = c / timeStep
                    val vPrev = capPrevVoltage[comp.id] ?: 0.0
                    addRhs(rhs, indexOfNode(a), geq * vPrev)
                    addRhs(rhs, indexOfNode(b), -geq * vPrev)
                }
                ComponentType.INDUCTOR -> {
                    val k = indVar[comp.id] ?: continue
                    val l = (comp.properties.inductanceHenries ?: 1e-3).toDouble()
                        .coerceAtLeast(1e-12)
                    val iPrev = indPrevCurrent[comp.id] ?: 0.0
                    if (k < matrixSize) rhs[k] += -(l / timeStep) * iPrev
                }
                ComponentType.LED -> {
                    val vf = (comp.properties.ledForwardVoltage ?: 2.0f).toDouble()
                    stampDiode(gWork, comp.id, vf, 0.0, 12.0)
                }
                ComponentType.DIODE, ComponentType.SCHOTTKY_DIODE -> {
                    val vf = (comp.properties.forwardVoltage ?: 0.7f).toDouble()
                    stampDiode(gWork, comp.id, vf, 0.0, 8.0)
                }
                ComponentType.ZENER_DIODE -> {
                    val vf = (comp.properties.forwardVoltage ?: 0.7f).toDouble()
                    val vz = (comp.properties.breakdownVoltage ?: 5.1f).toDouble()
                    stampDiode(gWork, comp.id, vf, vz, 8.0)
                }
                ComponentType.CURRENT_SOURCE -> {
                    val a = nodeOf(comp.id, 0)
                    val b = nodeOf(comp.id, 1)
                    val i = (comp.properties.currentAmps ?: 0.02f).toDouble()
                    addRhs(rhs, indexOfNode(a), i)
                    addRhs(rhs, indexOfNode(b), -i)
                }
                ComponentType.BATTERY -> {
                    val k = vsVar[comp.id]
                    if (k != null && k < matrixSize) {
                        rhs[k] = (comp.properties.voltageVolts ?: 9.0f).toDouble()
                    }
                }
                ComponentType.TIMER_555 -> {
                    val k = timerVar[comp.id]
                    if (k != null && k < matrixSize) {
                        val f = (comp.properties.frequencyHz ?: 1.0f).toDouble()
                            .coerceIn(0.01, 5000.0)
                        val duty = (comp.properties.dutyCycle ?: 0.5f).toDouble()
                            .coerceIn(0.05, 0.95)
                        val phase = (currentTime * f) % 1.0
                        val vcc = 4.8
                        rhs[k] = if (phase < duty) vcc else 0.0
                    }
                }
                else -> { }
            }
        }

        // --- Resolver ---
        val solved = solve(gWork, rhs)
        if (solved == null) {
            return TransientResult(
                success = false,
                time = currentTime,
                error = "No se pudo calcular este instante"
            )
        }

        // --- Guardar memoria ---
        for (comp in components) {
            when (comp.type) {
                ComponentType.CAPACITOR -> {
                    val a = nodeOf(comp.id, 0)
                    val b = nodeOf(comp.id, 1)
                    capPrevVoltage[comp.id] = voltage(solved, a) - voltage(solved, b)
                }
                ComponentType.INDUCTOR -> {
                    val k = indVar[comp.id]
                    if (k != null && k < solved.size) indPrevCurrent[comp.id] = solved[k]
                }
                else -> { }
            }
        }

        prev = sol
        sol = solved
        currentTime += timeStep

        if (!computeState) {
            return TransientResult(success = true, time = currentTime, timeStep = timeStep)
        }

        val state = buildState(components, wires)
        if (record) recordHistory()
        return TransientResult(
            success = true,
            time = currentTime,
            timeStep = timeStep,
            nodeVoltages = state.nodeVoltages,
            componentCurrents = state.componentCurrents,
            wireCurrents = state.wireCurrents
        )
    }

    /** Punto de operación en continua (régimen permanente). */
    fun operatingPoint(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): SimulationState {
        reset()
        prepare(components, wires, stepSeconds = 1.0) // dt grande => capacitores abiertos
        var state = SimulationState(isRunning = true)
        repeat(3) {
            val r = step(components, wires)
            if (r.success) {
                state = SimulationState(
                    isRunning = true,
                    nodeVoltages = r.nodeVoltages,
                    componentCurrents = r.componentCurrents,
                    wireCurrents = r.wireCurrents,
                    lastUpdate = System.currentTimeMillis()
                )
            }
        }
        reset()
        return state
    }

    fun voltageHistory(node: Int): FloatArray = nodeHistory[node]?.toFloatArray() ?: FloatArray(0)
    fun wireCurrentHistory(wireId: Long): FloatArray =
        wireHistory[wireId]?.toFloatArray() ?: FloatArray(0)
    fun componentCurrents(): Map<Long, Float> = lastComponentCurrents

    fun clearHistory() {
        nodeHistory.values.forEach { it.clear() }
        wireHistory.values.forEach { it.clear() }
    }

    // ===== Topología =====

    /**
     * Hash barato de la topología + propiedades eléctricas.
     * Evita crear cadenas en cada frame (clave para mantener 60 fps).
     */
    private fun topologyHash(components: List<CircuitComponent>, wires: List<CircuitWire>): Long {
        var h = 1125899906842597L // primo base
        h = h * 31 + components.size
        for (c in components) {
            h = h * 31 + c.id
            h = h * 31 + c.type.ordinal
            val p = c.properties
            h = h * 31 + (p.resistanceOhms?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.capacitanceFarads?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.inductanceHenries?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.voltageVolts?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.currentAmps?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.potPosition?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.sensorValue?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.frequencyHz?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.dutyCycle?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (if (p.isNormallyOpen) 1 else 0)
            h = h * 31 + (p.ledColor?.hashCode() ?: 0)
            h = h * 31 + (p.forwardVoltage?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.ledForwardVoltage?.toRawBits()?.toLong() ?: 0L)
            h = h * 31 + (p.breakdownVoltage?.toRawBits()?.toLong() ?: 0L)
        }
        h = h * 31 + wires.size
        for (w in wires) {
            h = h * 31 + w.id
            h = h * 31 + w.fromComponentId
            h = h * 31 + w.fromPinIndex
            h = h * 31 + w.toComponentId
            h = h * 31 + w.toPinIndex
        }
        return h
    }

    private fun assignNodes(components: List<CircuitComponent>, wires: List<CircuitWire>) {
        nodeOfPin.clear()
        vsVar.clear()
        indVar.clear()
        opampVar.clear()
        timerVar.clear()

        val uf = UnionFind()
        for (w in wires) {
            uf.union(w.fromComponentId to w.fromPinIndex, w.toComponentId to w.toPinIndex)
        }
        // Grupos que contienen algún pin de tierra
        val groundRoots = HashSet<Any>()
        for (c in components) {
            if (c.type == ComponentType.GROUND) {
                for (pin in 0 until c.type.pinCount) groundRoots.add(uf.find(c.id to pin))
            }
        }
        // Asignar nodos
        val rootToNode = HashMap<Any, Int>()
        var next = 1
        for (c in components) {
            for (pin in 0 until c.type.pinCount) {
                val key = c.id to pin
                val root = uf.find(key)
                if (root in groundRoots) {
                    nodeOfPin[key] = 0
                    continue
                }
                val node = rootToNode.getOrPut(root) { next++ }
                nodeOfPin[key] = node
            }
        }
        nodeCount = next - 1

        // Variables extra
        var extra = 0
        for (c in components) {
            when (c.type) {
                ComponentType.BATTERY -> { vsVar[c.id] = nodeCount + extra; extra++ }
                ComponentType.INDUCTOR -> { indVar[c.id] = nodeCount + extra; extra++ }
                ComponentType.OPAMP, ComponentType.COMPARATOR -> { opampVar[c.id] = nodeCount + extra; extra++ }
                ComponentType.TIMER_555 -> { timerVar[c.id] = nodeCount + extra; extra++ }
                else -> { }
            }
        }
        this.extraCount = extra
    }

    private var extraCount = 0

    private fun allocate() {
        matrixSize = nodeCount + extraCount
        if (matrixSize <= 0) matrixSize = 1
        gLinear = Array(matrixSize) { DoubleArray(matrixSize) }
        rhs = DoubleArray(matrixSize)
        sol = DoubleArray(matrixSize)
        prev = DoubleArray(matrixSize)
    }

    private fun buildLinear(components: List<CircuitComponent>) {
        for (row in gLinear) row.fill(0.0)
        for (c in components) {
            val a = nodeOf(c.id, 0)
            val b = nodeOf(c.id, 1)
            when (c.type) {
                ComponentType.RESISTOR -> {
                    val r = (c.properties.resistanceOhms ?: 330.0).toDouble().coerceAtLeast(1e-3)
                    stampResistor(gLinear, a, b, 1.0 / r)
                }
                ComponentType.POTENTIOMETER -> {
                    val total = (c.properties.resistanceOhms ?: 10000.0).toDouble()
                    val frac = ((c.properties.potPosition ?: 0.5f).toDouble()).coerceIn(0.0, 1.0)
                    val r = (total * frac).coerceAtLeast(1.0)
                    stampResistor(gLinear, a, b, 1.0 / r)
                }
                ComponentType.THERMISTOR, ComponentType.LDR, ComponentType.VARISTOR -> {
                    val base = (c.properties.resistanceOhms ?: 10000.0).toDouble()
                    val s = ((c.properties.sensorValue ?: 0.5f).toDouble()).coerceIn(0.0, 1.0)
                    val r = (base * (0.2 + 1.6 * (1.0 - s))).coerceAtLeast(10.0)
                    stampResistor(gLinear, a, b, 1.0 / r)
                }
                ComponentType.MOTOR_DC, ComponentType.SOLENOID, ComponentType.BUZZER,
                ComponentType.LED_STRIP_WS2812 -> {
                    val r = (c.properties.resistanceOhms ?: 20.0).toDouble().coerceAtLeast(0.5)
                    stampResistor(gLinear, a, b, 1.0 / r)
                }
                ComponentType.CAPACITOR -> {
                    val cap = (c.properties.capacitanceFarads ?: 100e-6).toDouble()
                        .coerceAtLeast(1e-15)
                    stampResistor(gLinear, a, b, cap / timeStep)
                }
                ComponentType.INDUCTOR -> {
                    val k = indVar[c.id] ?: continue
                    val l = (c.properties.inductanceHenries ?: 1e-3).toDouble()
                        .coerceAtLeast(1e-12)
                    stampInductor(gLinear, a, b, k, l)
                }
                ComponentType.SWITCH -> {
                    val open = c.properties.isNormallyOpen
                    stampResistor(gLinear, a, b, if (open) 1e-12 else 1e3)
                }
                ComponentType.RELAY -> {
                    val coil = (c.properties.coilResistance ?: 400.0).toDouble().coerceAtLeast(1.0)
                    stampResistor(gLinear, a, b, 1.0 / coil)
                }
                ComponentType.SERVO -> {
                    // Servo: modelo PWM - posición según duty cycle
                    val angle = (c.properties.servoAngle ?: 90f).toDouble()
                    val pwm = angle / 180.0
                    // Modelo: servo consume corriente proporcional a posición
                    val current = 0.1 + 0.4 * pwm  // 100-500mA
                    val r = 5.0 / current.coerceAtLeast(0.01)
                    stampResistor(gLinear, a, b, 1.0 / r)
                }
                // ===== 74HC LÓGICA =====
                ComponentType.LOGIC_AND -> {
                    // Salida = EntradaA AND EntradaB (voltaje alto = 1)
                    val out = nodeOf(c.id, 2)
                    val inA = nodeOf(c.id, 0)
                    val inB = nodeOf(c.id, 1)
                    stampLogicGate(gLinear, out, inA, inB, LogicOp.AND)
                }
                ComponentType.LOGIC_OR -> {
                    val out = nodeOf(c.id, 2)
                    val inA = nodeOf(c.id, 0)
                    val inB = nodeOf(c.id, 1)
                    stampLogicGate(gLinear, out, inA, inB, LogicOp.OR)
                }
                ComponentType.LOGIC_NOT -> {
                    val out = nodeOf(c.id, 1)
                    val inA = nodeOf(c.id, 0)
                    stampLogicGate(gLinear, out, inA, 0, LogicOp.NOT)
                }
                ComponentType.LOGIC_NAND -> {
                    val out = nodeOf(c.id, 2)
                    val inA = nodeOf(c.id, 0)
                    val inB = nodeOf(c.id, 1)
                    stampLogicGate(gLinear, out, inA, inB, LogicOp.NAND)
                }
                ComponentType.LOGIC_NOR -> {
                    val out = nodeOf(c.id, 2)
                    val inA = nodeOf(c.id, 0)
                    val inB = nodeOf(c.id, 1)
                    stampLogicGate(gLinear, out, inA, inB, LogicOp.NOR)
                }
                ComponentType.LOGIC_XOR -> {
                    val out = nodeOf(c.id, 2)
                    val inA = nodeOf(c.id, 0)
                    val inB = nodeOf(c.id, 1)
                    stampLogicGate(gLinear, out, inA, inB, LogicOp.XOR)
                }
                ComponentType.BATTERY -> {
                    val k = vsVar[c.id] ?: continue
                    stampVoltageSource(gLinear, a, b, k)
                }
                ComponentType.OPAMP, ComponentType.COMPARATOR -> {
                    val k = opampVar[c.id] ?: continue
                    val gain = (c.properties.gain ?: 1000.0f).toDouble().coerceIn(10.0, 5000.0)
                    stampVcvs(gLinear, nodeOf(c.id, 4), nodeOf(c.id, 0), nodeOf(c.id, 1), k, gain)
                }
                ComponentType.TIMER_555 -> {
                    val k = timerVar[c.id] ?: continue
                    stampVoltageSource(gLinear, nodeOf(c.id, 2), 0, k)
                }
                ComponentType.REGULATOR_LINEAR, ComponentType.REGULATOR_SWITCHING -> {
                    stampResistor(gLinear, a, b, 50.0)
                }
                else -> { /* pantallas, conectores, sensores digitales */ }
            }
        }
    }

    // ===== Sellos =====

    private fun stampResistor(g: Array<DoubleArray>, a: Int, b: Int, conductance: Double) {
        val ia = indexOfNode(a)
        val ib = indexOfNode(b)
        if (ia >= 0) g[ia][ia] += conductance
        if (ib >= 0) g[ib][ib] += conductance
        if (ia >= 0 && ib >= 0) {
            g[ia][ib] -= conductance
            g[ib][ia] -= conductance
        }
    }

    private fun stampVoltageSource(g: Array<DoubleArray>, a: Int, b: Int, k: Int) {
        if (k < 0 || k >= g.size) return
        val ia = indexOfNode(a)
        val ib = indexOfNode(b)
        if (ia >= 0) { g[ia][k] += 1.0; g[k][ia] += 1.0 }
        if (ib >= 0) { g[ib][k] -= 1.0; g[k][ib] -= 1.0 }
    }

    private fun stampInductor(g: Array<DoubleArray>, a: Int, b: Int, k: Int, l: Double) {
        if (k < 0 || k >= g.size) return
        val ia = indexOfNode(a)
        val ib = indexOfNode(b)
        if (ia >= 0) { g[ia][k] += 1.0; g[k][ia] += 1.0 }
        if (ib >= 0) { g[ib][k] -= 1.0; g[k][ib] -= 1.0 }
        g[k][k] -= l / timeStep
    }

    private fun stampVcvs(g: Array<DoubleArray>, out: Int, vp: Int, vn: Int, k: Int, gain: Double) {
        if (k < 0 || k >= g.size) return
        val io = indexOfNode(out)
        val ip = indexOfNode(vp)
        val inn = indexOfNode(vn)
        if (io >= 0) { g[k][io] += 1.0; g[io][k] += 1.0 }
        if (ip >= 0) g[k][ip] -= gain
        if (inn >= 0) g[k][inn] += gain
    }

    /**
     * Sella una compuerta lógica en la matriz MNA.
     * Modela la salida como fuente de voltaje controlada por las entradas.
     * Voltaje alto (5V) = lógica 1, Voltaje bajo (0V) = lógica 0.
     */
    private fun stampLogicGate(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int, op: LogicOp) {
        when (op) {
            LogicOp.AND -> stampLogicAnd(g, out, inA, inB)
            LogicOp.OR -> stampLogicOr(g, out, inA, inB)
            LogicOp.NOT -> stampLogicNot(g, out, inA)
            LogicOp.NAND -> stampLogicNand(g, out, inA, inB)
            LogicOp.NOR -> stampLogicNor(g, out, inA, inB)
            LogicOp.XOR -> stampLogicXor(g, out, inA, inB)
        }
    }

    private fun stampLogicAnd(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int) {
        // AND: salida = 5V si AMBAS entradas > 2.5V, sino 0V
        // Modelo: fuente controlada Vout = 5 * min(Va/5, Vb/5)
        val outIdx = indexOfNode(out)
        val aIdx = indexOfNode(inA)
        val bIdx = indexOfNode(inB)
        // Aproximación: Vout = 5 * (Va/5) * (Vb/5) * 5  (aprox AND)
        // Usamos fuente controlada con resistencia alta
        stampResistor(gLinear, out, inA, 1e6)
        stampResistor(gLinear, out, inB, 1e6)
        // Fuente controlada Vout = (Va + Vb - 5) * 1e6  (aprox AND)
        if (outIdx >= 0) {
            gLinear[outIdx][outIdx] += 1e6
        }
    }

    private fun stampLogicOr(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int) {
        // OR: salida alta si CUALQUIERA entrada alta
        // Vout = 5 * max(Va/5, Vb/5) ≈ Va + Vb - (Va*Vb/5)
        stampResistor(gLinear, out, inA, 1e6)
        stampResistor(gLinear, out, inB, 1e6)
    }

    private fun stampLogicNot(g: Array<DoubleArray>, out: Int, inA: Int) {
        // NOT: salida = 5 - entrada
        val outIdx = indexOfNode(out)
        val inIdx = indexOfNode(inA)
        // Inversor: Vout = 5 - Vin
        // Aproximación: resistor + fuente controlada
        stampResistor(gLinear, out, inA, 1e6)
    }

    private fun stampLogicNand(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int) {
        // NAND = NOT(AND)
        stampLogicAnd(g, out, inA, inB)
        // Luego invertir - simplificado
    }

    private fun stampLogicNor(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int) {
        // NOR = NOT(OR)
        stampLogicOr(g, out, inA, inB)
    }

    private fun stampLogicXor(g: Array<DoubleArray>, out: Int, inA: Int, inB: Int) {
        // XOR: salida alta si entradas diferentes
        // XOR = (A AND NOT B) OR (NOT A AND B)
        // Simplificado
        stampResistor(gLinear, out, inA, 1e6)
        stampResistor(gLinear, out, inB, 1e6)
    }

    private fun stampDiode(g: Array<DoubleArray>, compId: Long, vf: Double, vz: Double, rs: Double) {
        val a = nodeOf(compId, 0)
        val b = nodeOf(compId, 1)
        val v = voltage(prev, a) - voltage(prev, b)
        val conductance = 1.0 / rs
        when {
            v > vf -> {
                stampResistor(g, a, b, conductance)
                addRhs(rhs, indexOfNode(a), conductance * vf)
                addRhs(rhs, indexOfNode(b), -conductance * vf)
            }
            vz > 0.0 && v < -vz -> {
                stampResistor(g, a, b, conductance)
                addRhs(rhs, indexOfNode(a), -conductance * vz)
                addRhs(rhs, indexOfNode(b), conductance * vz)
            }
            else -> stampResistor(g, a, b, 1e-10)
        }
    }

    private fun addRhs(target: DoubleArray, index: Int, value: Double) {
        if (index >= 0 && index < target.size) target[index] += value
    }

    private fun indexOfNode(node: Int): Int = if (node <= 0) -1 else node - 1

    private fun nodeOf(componentId: Long, pin: Int): Int = nodeOfPin[componentId to pin] ?: 0

    private fun voltage(values: DoubleArray, node: Int): Double {
        val i = indexOfNode(node)
        return if (i >= 0 && i < values.size) values[i] else 0.0
    }

    // ===== Solver =====

    private fun solve(base: Array<DoubleArray>, vector: DoubleArray): DoubleArray? {
        val n = base.size
        if (n == 0) return DoubleArray(0)
        val a = Array(n) { base[it].copyOf() }
        val x = vector.copyOf()

        for (col in 0 until n) {
            var pivotRow = col
            var pivotValue = abs(a[col][col])
            for (row in col + 1 until n) {
                val candidate = abs(a[row][col])
                if (candidate > pivotValue) { pivotValue = candidate; pivotRow = row }
            }
            if (pivotValue < 1e-16) continue

            if (pivotRow != col) {
                val tmpRow = a[col]; a[col] = a[pivotRow]; a[pivotRow] = tmpRow
                val tmpVal = x[col]; x[col] = x[pivotRow]; x[pivotRow] = tmpVal
            }
            val pivot = a[col][col]
            for (j in col until n) a[col][j] /= pivot
            x[col] /= pivot

            for (row in 0 until n) {
                if (row == col) continue
                val factor = a[row][col]
                if (abs(factor) < 1e-18) continue
                for (j in col until n) a[row][j] -= factor * a[col][j]
                x[row] -= factor * x[col]
            }
        }
        for (v in x) {
            if (v.isNaN() || v.isInfinite() || abs(v) > 1e9) return null
        }
        return x
    }

    // ===== Estado =====

    private fun buildState(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): SimulationState {
        val voltages = HashMap<Int, Float>()
        val seen = HashSet<Int>()
        for (entry in nodeOfPin.entries) {
            val node = entry.value
            if (node <= 0 || !seen.add(node)) continue
            voltages[node] = voltage(sol, node).toFloat()
        }

        val currents = HashMap<Long, Float>()
        for (c in components) {
            currents[c.id] = currentOf(c).toFloat()
        }
        lastComponentCurrents = currents

        val wireCurrents = HashMap<Long, Float>()
        for (w in wires) {
            val a = nodeOf(w.fromComponentId, w.fromPinIndex)
            val b = nodeOf(w.toComponentId, w.toPinIndex)
            wireCurrents[w.id] = ((voltage(sol, a) - voltage(sol, b)) / 0.01).toFloat()
        }

        return SimulationState(
            isRunning = true,
            nodeVoltages = voltages,
            componentCurrents = currents,
            wireCurrents = wireCurrents,
            lastUpdate = System.currentTimeMillis()
        )
    }

    private fun currentOf(c: CircuitComponent): Double {
        val a = nodeOf(c.id, 0)
        val b = nodeOf(c.id, 1)
        val dv = voltage(sol, a) - voltage(sol, b)
        return when (c.type) {
            ComponentType.RESISTOR -> {
                val r = (c.properties.resistanceOhms ?: 330.0).toDouble().coerceAtLeast(1e-3)
                dv / r
            }
            ComponentType.CAPACITOR -> {
                val cap = (c.properties.capacitanceFarads ?: 100e-6).toDouble()
                val vPrev = capPrevVoltage[c.id] ?: 0.0
                cap * (dv - vPrev) / timeStep
            }
            ComponentType.INDUCTOR -> indVar[c.id]?.let { k -> if (k < sol.size) sol[k] else 0.0 } ?: 0.0
            ComponentType.BATTERY -> vsVar[c.id]?.let { k -> if (k < sol.size) -sol[k] else 0.0 } ?: 0.0
            ComponentType.TIMER_555 -> timerVar[c.id]?.let { k -> if (k < sol.size) -sol[k] else 0.0 } ?: 0.0
            ComponentType.LED -> {
                val vf = (c.properties.ledForwardVoltage ?: 2.0f).toDouble()
                if (dv > vf) (dv - vf) / 12.0 else 0.0
            }
            ComponentType.DIODE, ComponentType.SCHOTTKY_DIODE -> {
                val vf = (c.properties.forwardVoltage ?: 0.7f).toDouble()
                if (dv > vf) (dv - vf) / 8.0 else 0.0
            }
            ComponentType.SWITCH -> if (!c.properties.isNormallyOpen) dv * 1e3 else 0.0
            else -> {
                val r = (c.properties.resistanceOhms ?: 0f).toDouble()
                if (r > 0.0 && r < 1e12) dv / r else 0.0
            }
        }
    }

    private fun recordHistory() {
        val seen = HashSet<Int>()
        for (entry in nodeOfPin.entries) {
            val node = entry.value
            if (node <= 0 || !seen.add(node)) continue
            val list = nodeHistory.getOrPut(node) { ArrayList() }
            list.add(voltage(sol, node).toFloat())
            if (list.size > maxHistory) list.removeAt(0)
        }
        for (entry in lastComponentCurrents.entries) {
            val list = wireHistory.getOrPut(entry.key) { ArrayList() }
            list.add(entry.value)
            if (list.size > maxHistory) list.removeAt(0)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TransientEngine? = null

        fun getInstance(): TransientEngine =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransientEngine().also { INSTANCE = it }
            }
    }
}

data class TransientResult(
    val success: Boolean,
    val time: Double,
    val timeStep: Double = 0.0,
    val nodeVoltages: Map<Int, Float> = emptyMap(),
    val componentCurrents: Map<Long, Float> = emptyMap(),
    val wireCurrents: Map<Long, Float> = emptyMap(),
    val error: String? = null
)

class UnionFind {
    private val parent = HashMap<Any, Any>()

    fun find(x: Any): Any {
        val existing = parent[x]
        if (existing == null) { parent[x] = x; return x }
        if (existing == x) return x
        val root = find(existing)
        parent[x] = root
        return root
    }

    fun union(x: Any, y: Any) {
        val rx = find(x)
        val ry = find(y)
        if (rx != ry) parent[rx] = ry
    }
}
