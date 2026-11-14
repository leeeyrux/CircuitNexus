package com.circuitnexus.engine

import kotlin.math.roundToInt

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.Measurement
import com.circuitnexus.model.SimulationState
import com.circuitnexus.model.SimulationWarning
import com.circuitnexus.model.WarningSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor de simulación rápido usando Análisis Nodal Modificado (MNA) simplificado
 * Calcula voltajes en nodos y corrientes en tiempo real sin bloquear el hilo principal
 */
class CircuitEngine {

    /**
     * Ejecuta la simulación completa del circuito
     */
    suspend fun simulate(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): SimulationState = withContext(Dispatchers.Default) {
        // Construir grafo de nodos
        val nodeMap = buildNodeMap(components, wires)
        val nodeCount = nodeMap.size

        if (nodeCount == 0) {
            return SimulationState(isRunning = true)
        }

        // Matriz de conductancia G (nodeCount x nodeCount)
        // Vector de corriente I (nodeCount)
        val G = Array(nodeCount) { DoubleArray(nodeCount) { 0.0 } }
        val I = DoubleArray(nodeCount) { 0.0 }

        // Rellenar matriz con componentes
        stampComponents(components, wires, nodeMap, G, I)

        // Resolver sistema lineal G * V = I usando eliminación gaussiana
        val voltages = solveLinearSystem(G, I)

        // Calcular corrientes en componentes y cables
        val componentCurrents = calculateComponentCurrents(components, wires, nodeMap, voltages)
        val wireCurrents = calculateWireCurrents(wires, nodeMap, voltages)

        // Detectar advertencias
        val warnings = detectWarnings(components, wires, nodeMap, voltages, componentCurrents)

        // Potencia total
        val power = componentCurrents.values.sumOf { it * it } // Simplificado

        SimulationState(
            isRunning = true,
            nodeVoltages = voltages.mapValues { (k, v) -> k to v.toFloat() },
            componentCurrents = componentCurrents.mapValues { (k, v) -> k to v.toFloat() },
            wireCurrents = wireCurrents.mapValues { (k, v) -> k to v.toFloat() },
            powerConsumption = power.toFloat(),
            warnings = warnings
        )
    }

    /**
     * Construye mapa de nodos únicos a partir de conexiones
     * Nodo 0 = Tierra/GND (referencia)
     */
    private fun buildNodeMap(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): Map<Int, Int> { // (componentId, pinIndex) -> nodeIndex
        val nodeAssignments = mutableMapOf<Pair<Long, Int>, Int>()
        var nextNodeIndex = 1 // 0 reservado para GND

        // Primero: asignar nodo 0 a todos los pines de GROUND conectados
        val groundComponents = components.filter { it.type == ComponentType.GROUND }
        for (gc in groundComponents) {
            for (pin in 0 until CircuitWire.getPinCount(ComponentType.GROUND)) {
                nodeAssignments[gc.id to pin] = 0
            }
        }

        // Union-Find para agrupar nodos conectados por cables
        val uf = UnionFind()

        // Registrar todas las conexiones de cables
        for (wire in wires) {
            uf.union(wire.fromComponentId to wire.fromPinIndex, wire.toComponentId to wire.toPinIndex)
        }

        // Asignar índice de nodo a cada grupo de unión
        val groupToNode = mutableMapOf<Any, Int>()

        for (comp in components) {
            val pinCount = CircuitWire.getPinCount(comp.type)
            for (pin in 0 until pinCount) {
                val key = comp.id to pin
                if (key in nodeAssignments) continue // Ya asignado (GROUND)

                val root = uf.find(key)
                val nodeIndex = groupToNode.getOrPut(root) {
                    val idx = nextNodeIndex
                    nextNodeIndex++
                    idx
                }
                nodeAssignments[key] = nodeIndex
            }
        }

        return nodeAssignments
    }

    /**
     * Rellena la matriz de conductancia G y vector I con sellos de componentes
     */
    private fun stampComponents(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        nodeMap: Map<Pair<Long, Int>, Int>,
        G: Array<DoubleArray>,
        I: DoubleArray
    ) {
        for (comp in components) {
            val pinCount = CircuitWire.getPinCount(comp.type)
            val pins = (0 until pinCount).mapNotNull { pin ->
                nodeMap[comp.id to pin]
            }

            when (comp.type) {
                ComponentType.BATTERY -> {
                    // Fuente de voltaje: V+ - V- = Vbat
                    // Usamos MNA: añadimos ecuación para corriente de la fuente
                    if (pins.size >= 2) {
                        val vPlus = pins[0]
                        val vMinus = pins[1]
                        val voltage = comp.properties.voltageVolts ?: 9.0
                        stampVoltageSource(vPlus, vMinus, voltage.toDouble(), G, I, components.size)
                    }
                }
                ComponentType.RESISTOR -> {
                    if (pins.size >= 2) {
                        val r = (comp.properties.resistanceOhms ?: 330.0).toDouble()
                        val g = if (r > 0) 1.0 / r else 1e9 // Cortocircuito = conductancia muy alta
                        stampResistor(pins[0], pins[1], g, G)
                    }
                }
                ComponentType.LED -> {
                    if (pins.size >= 2) {
                        // LED modelado como fuente de voltaje (Vf) + resistencia pequeña en serie
                        val vf = (comp.properties.ledForwardVoltage ?: 2.0).toDouble()
                        val rSeries = 0.1 // 0.1 ohm resistencia serie
                        val g = 1.0 / rSeries
                        stampVoltageSourceWithResistance(pins[0], pins[1], vf, g, G, I)
                    }
                }
                ComponentType.CAPACITOR -> {
                    if (pins.size >= 2) {
                        // En DC estable: capacitor = circuito abierto (ninguna corriente)
                        // En transitorio: necesitaríamos integración numérica
                        // Por ahora: resistencia muy alta (circuito abierto en DC)
                        val g = 1e-9
                        stampResistor(pins[0], pins[1], g, G)
                    }
                }
                ComponentType.SWITCH -> {
                    if (pins.size >= 2) {
                        val isOpen = comp.properties.isNormallyOpen
                        val g = if (isOpen) 1e-9 else 1e9 // Abierto = casi 0, Cerrado = casi infinito
                        stampResistor(pins[0], pins[1], g, G)
                    }
                }
                ComponentType.TIMER_555 -> {
                    // Simplificado: el 555 en astable genera onda cuadrada en pin 3 (salida)
                    // Pin 1 = GND (ya en nodo 0), Pin 8 = VCC
                    // Para simulación DC: salida alta o baja según configuración
                    // Modelo completo requeriría simulación transitoria
                }
                ComponentType.GROUND -> {
                    // Ya manejado en nodeMap (nodo 0)
                }
                ComponentType.POTENTIOMETER -> {
                    if (pins.size >= 2) {
                        val total = (comp.properties.resistanceOhms ?: 10000.0).toDouble()
                        val frac = (comp.properties.potPosition ?: 0.5f).toDouble()
                        val r = (total * frac).coerceAtLeast(1.0)
                        stampResistor(pins[0], pins[1], 1.0 / r, G)
                    }
                }
                ComponentType.THERMISTOR, ComponentType.LDR, ComponentType.VARISTOR,
                ComponentType.MOTOR_DC, ComponentType.SOLENOID, ComponentType.BUZZER -> {
                    if (pins.size >= 2) {
                        val r = (comp.properties.resistanceOhms ?: 1000.0).toDouble().coerceAtLeast(0.5)
                        stampResistor(pins[0], pins[1], 1.0 / r, G)
                    }
                }
                ComponentType.INDUCTOR -> {
                    if (pins.size >= 2) {
                        // En corriente continua la bobina es casi un cable
                        stampResistor(pins[0], pins[1], 1e3, G)
                    }
                }
                ComponentType.DIODE, ComponentType.SCHOTTKY_DIODE, ComponentType.ZENER_DIODE -> {
                    if (pins.size >= 2) {
                        val vf = (comp.properties.forwardVoltage ?: 0.7).toDouble()
                        stampVoltageSourceWithResistance(pins[0], pins[1], vf, 10.0, G, I)
                    }
                }
                ComponentType.REGULATOR_LINEAR, ComponentType.REGULATOR_SWITCHING -> {
                    if (pins.size >= 2) {
                        // Entrada -> Salida con caída fija pequeña
                        stampResistor(pins[0], pins[1], 0.05, G)
                    }
                }
                else -> {
                    // Piezas sin modelo DC (pantallas, conectores, sensores digitales)
                }
            }
        }
    }

    private fun stampResistor(nodeA: Int, nodeB: Int, g: Double, G: Array<DoubleArray>) {
        if (nodeA > 0 && nodeB > 0) {
            G[nodeA][nodeA] += g
            G[nodeB][nodeB] += g
            G[nodeA][nodeB] -= g
            G[nodeB][nodeA] -= g
        } else if (nodeA > 0) { // nodeB == 0 (GND)
            G[nodeA][nodeA] += g
        } else if (nodeB > 0) { // nodeA == 0 (GND)
            G[nodeB][nodeB] += g
        }
    }

    private fun stampVoltageSource(
        nodePos: Int, nodeNeg: Int, voltage: Double,
        G: Array<DoubleArray>, I: DoubleArray, componentCount: Int
    ) {
        // MNA simplificado: para DC, fijamos voltaje entre nodos
        // En implementación completa, añadiríamos variable de corriente de la fuente
        // Aquí aproximamos: si uno es GND, fijamos el otro al voltaje
        if (nodeNeg == 0 && nodePos > 0) {
            // Fijar nodo positivo al voltaje: G[node][node] = 1, I[node] = voltage
            G[nodePos][nodePos] = 1.0
            I[nodePos] = voltage
            // Ceros en fila y columna excepto diagonal
            for (i in G.indices) {
                if (i != nodePos) {
                    G[nodePos][i] = 0.0
                    G[i][nodePos] = 0.0
                }
            }
        } else if (nodePos == 0 && nodeNeg > 0) {
            G[nodeNeg][nodeNeg] = 1.0
            I[nodeNeg] = -voltage
            for (i in G.indices) {
                if (i != nodeNeg) {
                    G[nodeNeg][i] = 0.0
                    G[i][nodeNeg] = 0.0
                }
            }
        } else if (nodePos > 0 && nodeNeg > 0) {
            // Diferencia de voltaje fija - simplificación: fuente ideal entre dos nodos no-GND
            // Añadimos ecuación: Vpos - Vneg = voltage
            // Esto requiere expandir la matriz (MNA completo)
            // Simplificación: conectar con resistencia muy baja y fuente en serie
            stampResistor(nodePos, nodeNeg, 1e6, G)
            I[nodePos] += voltage * 1e6
            I[nodeNeg] -= voltage * 1e6
        }
    }

    private fun stampVoltageSourceWithResistance(
        nodePos: Int, nodeNeg: Int, vf: Double, g: Double,
        G: Array<DoubleArray>, I: DoubleArray
    ) {
        // LED: Vf en serie con resistencia
        // Ecuación: I = g * (Vpos - Vneg - Vf)
        // -> g*Vpos - g*Vneg = I + g*Vf
        if (nodePos > 0 && nodeNeg > 0) {
            G[nodePos][nodePos] += g
            G[nodeNeg][nodeNeg] += g
            G[nodePos][nodeNeg] -= g
            G[nodeNeg][nodePos] -= g
            I[nodePos] += g * vf
            I[nodeNeg] -= g * vf
        } else if (nodePos > 0) { // nodeNeg == 0
            G[nodePos][nodePos] += g
            I[nodePos] += g * vf
        } else if (nodeNeg > 0) { // nodePos == 0
            G[nodeNeg][nodeNeg] += g
            I[nodeNeg] -= g * vf
        }
    }

    /**
     * Resuelve sistema lineal G * V = I por eliminación gaussiana con pivoteo parcial
     */
    private fun solveLinearSystem(G: Array<DoubleArray>, I: DoubleArray): Map<Int, Double> {
        val n = G.size
        if (n == 0) return emptyMap()

        // Copiar matrices para no modificar originales
        val A = G.map { it.copyOf() }.toTypedArray()
        val b = I.copyOf()

        // Eliminación gaussiana
        for (col in 0 until n) {
            // Pivoteo parcial
            var maxRow = col
            var maxVal = kotlin.math.abs(A[col][col])
            for (row in col + 1 until n) {
                val val = kotlin.math.abs(A[row][col])
                if (val > maxVal) {
                    maxVal = val
                    maxRow = row
                }
            }

            if (maxVal < 1e-12) continue // Columna casi cero, saltar

            // Intercambiar filas
            if (maxRow != col) {
                val tempRow = A[col]
                A[col] = A[maxRow]
                A[maxRow] = tempRow
                val tempB = b[col]
                b[col] = b[maxRow]
                b[maxRow] = tempB
            }

            // Normalizar fila pivote
            val pivot = A[col][col]
            for (j in col until n) {
                A[col][j] /= pivot
            }
            b[col] /= pivot

            // Eliminar otras filas
            for (row in 0 until n) {
                if (row != col) {
                    val factor = A[row][col]
                    if (kotlin.math.abs(factor) > 1e-12) {
                        for (j in col until n) {
                            A[row][j] -= factor * A[col][j]
                        }
                        b[row] -= factor * b[col]
                    }
                }
            }
        }

        // Extraer solución
        return b.indices.associate { it to b[it] }
    }

    private fun calculateComponentCurrents(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        nodeMap: Map<Pair<Long, Int>, Int>,
        voltages: Map<Int, Double>
    ): Map<Long, Double> {
        val currents = mutableMapOf<Long, Double>()

        for (comp in components) {
            val pinCount = CircuitWire.getPinCount(comp.type)
            var current = 0.0

            when (comp.type) {
                ComponentType.RESISTOR -> {
                    if (pinCount >= 2) {
                        val v1 = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0
                        val v2 = voltages[nodeMap[comp.id to 1] ?: 0] ?: 0.0
                        val r = (comp.properties.resistanceOhms ?: 330.0).toDouble()
                        if (r > 0) current = (v1 - v2) / r
                    }
                }
                ComponentType.LED -> {
                    if (pinCount >= 2) {
                        val v1 = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0
                        val v2 = voltages[nodeMap[comp.id to 1] ?: 0] ?: 0.0
                        val vf = (comp.properties.ledForwardVoltage ?: 2.0).toDouble()
                        val rSeries = 0.1
                        val vDiff = v1 - v2 - vf
                        if (vDiff > 0) current = vDiff / rSeries
                    }
                }
                ComponentType.BATTERY -> {
                    // Corriente suministrada por la batería (aproximación)
                    if (pinCount >= 2) {
                        val v1 = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0
                        val v2 = voltages[nodeMap[comp.id to 1] ?: 0] ?: 0.0
                        current = kotlin.math.abs(v1 - v2) / 1000.0 // Estimación
                    }
                }
                else -> current = 0.0
            }

            currents[comp.id] = current
        }

        return currents
    }

    private fun calculateWireCurrents(
        wires: List<CircuitWire>,
        nodeMap: Map<Pair<Long, Int>, Int>,
        voltages: Map<Int, Double>
    ): Map<Long, Double> {
        val currents = mutableMapOf<Long, Double>()

        for (wire in wires) {
            val nodeFrom = nodeMap[wire.fromComponentId to wire.fromPinIndex] ?: 0
            val nodeTo = nodeMap[wire.toComponentId to wire.toPinIndex] ?: 0
            val vFrom = voltages[nodeFrom] ?: 0.0
            val vTo = voltages[nodeTo] ?: 0.0

            // Corriente en el cable = diferencia de voltaje / resistencia del cable (muy baja)
            val wireResistance = 0.01 // 10 milliohm
            val current = if (wireResistance > 0) (vFrom - vTo) / wireResistance else 0.0

            currents[wire.id] = current
        }

        return currents
    }

    private fun detectWarnings(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        nodeMap: Map<Pair<Long, Int>, Int>,
        voltages: Map<Int, Double>,
        componentCurrents: Map<Long, Double>
    ): List<SimulationWarning> {
        val warnings = mutableListOf<SimulationWarning>()

        // Verificar cada componente
        for (comp in components) {
            when (comp.type) {
                ComponentType.LED -> {
                    val current = componentCurrents[comp.id] ?: 0.0
                    val vAnode = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0
                    val vCathode = voltages[nodeMap[comp.id to 1] ?: 0] ?: 0.0
                    val vLed = vAnode - vCathode
                    val vf = (comp.properties.ledForwardVoltage ?: 2.0).toDouble()

                    // LED sin resistencia limitadora (corriente > 25mA)
                    if (current > 0.025) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.ERROR,
                            message = "¡Cuidado! A esta Luz LED le llega demasiada energía (${(current * 1000).roundToInt()} mA). Se puede quemar.",
                            componentId = comp.id,
                            suggestion = "Pon una Resistencia de 330 Ω antes del LED para protegerlo."
                        ))
                    }
                    // LED con voltaje excesivo
                    else if (vLed > vf + 2.0) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.WARNING,
                            message = "Esta Luz LED recibe ${vLed.roundToInt()}V, más de lo que aguanta (${vf.roundToInt()}V).",
                            componentId = comp.id,
                            suggestion = "Añade una Resistencia en serie para bajar el voltaje."
                        ))
                    }
                    // LED apagado pero conectado
                    else if (vLed < vf && current < 0.001) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.INFO,
                            message = "La Luz LED está apagada. Necesita al menos ${vf.roundToInt()}V para encender.",
                            componentId = comp.id,
                            suggestion = "Revisa que la Batería esté conectada y el Interruptor cerrado."
                        ))
                    }
                }

                ComponentType.BATTERY -> {
                    val current = componentCurrents[comp.id] ?: 0.0
                    val voltage = comp.properties.voltageVolts ?: 9.0

                    // Cortocircuito: batería sin carga significativa
                    if (current > voltage / 1.0) { // Menos de 1 ohm de carga
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.ERROR,
                            message = "¡Peligro! La Batería está conectada casi en cortocircuito. Se calentará y agotará rápido.",
                            componentId = comp.id,
                            suggestion = "Siempre pon algo que use la energía (Resistencia, LED, etc.) entre positivo y negativo."
                        ))
                    }
                }

                ComponentType.CAPACITOR -> {
                    val vPos = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0
                    val vNeg = voltages[nodeMap[comp.id to 1] ?: 0] ?: 0.0
                    val vCap = kotlin.math.abs(vPos - vNeg)
                    val maxVoltage = 25.0 // Asumir 25V máx para electrolíticos comunes

                    if (vCap > maxVoltage) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.ERROR,
                            message = "¡Cuidado! Este Capacitor tiene ${vCap.roundToInt()}V, supera su límite (${maxVoltage.roundToInt()}V). Puede explotar.",
                            componentId = comp.id,
                            suggestion = "Usa un capacitor de mayor voltaje o reduce el voltaje del circuito."
                        ))
                    }
                    // Polaridad invertida en electrolítico
                    if (vPos < vNeg) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.WARNING,
                            message = "El Capacitor está conectado al revés (polaridad invertida).",
                            componentId = comp.id,
                            suggestion = "Invierte las conexiones: el lado positivo (+) al voltaje más alto."
                        ))
                    }
                }

                ComponentType.TIMER_555 -> {
                    val vVcc = voltages[nodeMap[comp.id to 7] ?: 0] ?: 0.0 // Pin 8 = VCC
                    val vGnd = voltages[nodeMap[comp.id to 0] ?: 0] ?: 0.0 // Pin 1 = GND

                    if (vVcc - vGnd < 4.5) {
                        warnings.add(SimulationWarning(
                            severity = WarningSeverity.WARNING,
                            message = "El Chip 555 tiene solo ${(vVcc - vGnd).roundToInt()}V. Necesita al menos 5V para funcionar bien.",
                            componentId = comp.id,
                            suggestion = "Conecta una Batería de 9V o 5V al pin 8 (VCC) y pin 1 (Tierra)."
                        ))
                    }
                }
                else -> { /* Sin advertencias específicas */ }
            }
        }

        // Verificar cables sueltos (conectados solo por un lado)
        val connectionCount = mutableMapOf<Long, Int>()
        for (wire in wires) {
            connectionCount[wire.fromComponentId] = connectionCount.getOrDefault(wire.fromComponentId, 0) + 1
            connectionCount[wire.toComponentId] = connectionCount.getOrDefault(wire.toComponentId, 0) + 1
        }

        for (comp in components) {
            val expectedPins = CircuitWire.getPinCount(comp.type)
            val actualConnections = connectionCount[comp.id] ?: 0
            if (actualConnections == 0 && components.size > 1) {
                warnings.add(SimulationWarning(
                    severity = WarningSeverity.INFO,
                    message = "La ${comp.type.displayName.toLowerCase()} no está conectada a nada.",
                    componentId = comp.id,
                    suggestion = "Arrastra un cable desde sus puntos de conexión para unirla al circuito."
                ))
            } else if (actualConnections < expectedPins && comp.type != ComponentType.GROUND) {
                warnings.add(SimulationWarning(
                    severity = WarningSeverity.INFO,
                    message = "La ${comp.type.displayName.toLowerCase()} tiene conexiones sueltas (${actualConnections} de $expectedPins).",
                    componentId = comp.id,
                    suggestion = "Conecta todos sus puntos para que funcione correctamente."
                ))
            }
        }

        return warnings
    }

    /**
     * Obtiene medición en un punto específico (nodo)
     */
    suspend fun measureAtNode(
        nodeIndex: Int,
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): Measurement = withContext(Dispatchers.Default) {
        val state = simulate(components, wires)
        val voltage = state.nodeVoltages[nodeIndex] ?: 0f
        // Corriente aproximada sumando cables conectados a este nodo
        var current = 0f
        for (wire in wires) {
            // Simplificado
        }
        return Measurement(voltage = voltage, current = current, power = voltage * current)
    }

    companion object {
        // Instancia singleton para uso fácil
        @Volatile private var INSTANCE: CircuitEngine? = null

        fun getInstance(): CircuitEngine = INSTANCE ?: synchronized(this) {
            INSTANCE ?: CircuitEngine().also { INSTANCE = it }
        }
    }
}

/**
 * Union-Find (Disjoint Set) para agrupar nodos conectados
 */
class UnionFind {
    private val parent = mutableMapOf<Any, Any>()
    private val rank = mutableMapOf<Any, Int>()

    fun find(x: Any): Any {
        if (x !in parent) {
            parent[x] = x
            rank[x] = 0
            return x
        }
        if (parent[x] != x) {
            parent[x] = find(parent[x]!!)
        }
        return parent[x]!!
    }

    fun union(x: Any, y: Any) {
        val rootX = find(x)
        val rootY = find(y)
        if (rootX == rootY) return

        val rankX = rank[rootX] ?: 0
        val rankY = rank[rootY] ?: 0

        when {
            rankX < rankY -> parent[rootX] = rootY
            rankX > rankY -> parent[rootY] = rootX
            else -> {
                parent[rootY] = rootX
                rank[rootX] = rankX + 1
            }
        }
    }
}
