package com.circuitnexus.engine

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import com.circuitnexus.model.WarningSeverity

/**
 * Revisión automática del circuito ("Revisar mi Circuito").
 *
 * Detecta errores típicos de principiante ANTES de que simule:
 *  - Pines sin conectar
 *  - Cortocircuitos entre positivo y negativo
 *  - LEDs sin resistencia
 *  - Chips sin alimentación
 *  - Capacitores al revés
 *  - Salidas de chip conectadas entre sí
 *
 * Siempre habla en español cotidiano, sin jerga.
 */
object DrcEngine {

    data class DrcIssue(
        val severity: WarningSeverity,
        val title: String,
        val message: String,
        val suggestion: String,
        val componentId: Long? = null,
        val wireId: Long? = null
    )

    data class DrcReport(
        val issues: List<DrcIssue>,
        val score: Int, // 0..100
        val summary: String
    ) {
        val errors: List<DrcIssue> get() = issues.filter { it.severity == WarningSeverity.ERROR }
        val warnings: List<DrcIssue> get() = issues.filter { it.severity == WarningSeverity.WARNING }
        val tips: List<DrcIssue> get() = issues.filter { it.severity == WarningSeverity.INFO }
        val isClean: Boolean get() = errors.isEmpty() && warnings.isEmpty()
    }

    fun review(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        state: SimulationState?
    ): DrcReport {
        val issues = mutableListOf<DrcIssue>()

        if (components.isEmpty()) {
            return DrcReport(
                issues = listOf(
                    DrcIssue(
                        WarningSeverity.INFO,
                        "Mesa vacía",
                        "Todavía no has puesto ninguna pieza.",
                        "Arrastra una Batería, una Resistencia y una Luz LED para empezar."
                    )
                ),
                score = 100,
                summary = "Aún no hay nada que revisar."
            )
        }

        issues += checkUnconnectedPins(components, wires)
        issues += checkShortCircuit(components, wires)
        issues += checkLedsWithoutResistor(components, wires)
        issues += checkChipsPower(components, wires)
        issues += checkPolarity(components, wires)
        issues += checkBatteries(components, wires)
        issues += state?.let { checkRuntime(it, components) } ?: emptyList()

        val errors = issues.count { it.severity == WarningSeverity.ERROR }
        val warnings = issues.count { it.severity == WarningSeverity.WARNING }
        var score = 100 - errors * 25 - warnings * 10
        score = score.coerceIn(0, 100)

        val summary = when {
            errors > 0 -> "Hay ${errors} problema(s) importante(s) que conviene arreglar."
            warnings > 0 -> "Se ve bien, pero hay ${warnings} detalle(s) que puedes mejorar."
            else -> "¡Todo se ve correcto! Tu circuito está listo."
        }

        return DrcReport(issues, score, summary)
    }

    // ===== Comprobaciones =====

    private fun checkUnconnectedPins(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        val connections = HashMap<Long, MutableSet<Int>>()

        for (wire in wires) {
            connections.getOrPut(wire.fromComponentId) { mutableSetOf() }.add(wire.fromPinIndex)
            connections.getOrPut(wire.toComponentId) { mutableSetOf() }.add(wire.toPinIndex)
        }

        for (comp in components) {
            if (comp.type == ComponentType.GROUND || comp.type == ComponentType.WIRE) continue
            val connected = connections[comp.id]?.size ?: 0
            val total = comp.type.pinCount
            if (connected == 0) {
                issues += DrcIssue(
                    WarningSeverity.INFO,
                    "${comp.type.displayName} sin conectar",
                    "Esta pieza no toca ningún cable.",
                    "Arrastra un cable desde uno de sus puntos de conexión.",
                    comp.id
                )
            } else if (connected < total) {
                issues += DrcIssue(
                    WarningSeverity.INFO,
                    "${comp.type.displayName} a medias",
                    "Tiene ${connected} de ${total} puntos conectados.",
                    "Conecta los puntos que faltan para que funcione bien.",
                    comp.id
                )
            }
        }
        return issues
    }

    private fun checkShortCircuit(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        for (battery in components.filter { it.type == ComponentType.BATTERY }) {
            val positiveConnected = wires.filter {
                (it.fromComponentId == battery.id && it.fromPinIndex == 0) ||
                (it.toComponentId == battery.id && it.toPinIndex == 0)
            }
            val negativeConnected = wires.filter {
                (it.fromComponentId == battery.id && it.fromPinIndex == 1) ||
                (it.toComponentId == battery.id && it.toPinIndex == 1)
            }
            if (positiveConnected.isEmpty() || negativeConnected.isEmpty()) continue

            // ¿Los dos lados llegan a los mismos dos nodos sin carga en medio?
            val posNodes = endpoints(positiveConnected, battery.id)
            val negNodes = endpoints(negativeConnected, battery.id)
            if (posNodes.intersect(negNodes).isNotEmpty()) {
                issues += DrcIssue(
                    WarningSeverity.ERROR,
                    "¡Cortocircuito!",
                    "El positivo y el negativo de la Batería se están tocando directamente.",
                    "Pon algo que use la energía (una Resistencia, una Luz LED...) entre los dos.",
                    battery.id
                )
            }
        }
        return issues
    }

    private fun endpoints(wires: List<CircuitWire>, excludeId: Long): Set<Long> {
        val set = mutableSetOf<Long>()
        for (wire in wires) {
            if (wire.fromComponentId != excludeId) set.add(wire.fromComponentId)
            if (wire.toComponentId != excludeId) set.add(wire.toComponentId)
        }
        return set
    }

    private fun checkLedsWithoutResistor(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        for (led in components.filter { it.type == ComponentType.LED }) {
            val neighbors = neighborsOf(led.id, wires, components)
            val hasResistor = neighbors.any {
                it.type == ComponentType.RESISTOR || it.type == ComponentType.POTENTIOMETER
            }
            val hasBattery = neighbors.any { it.type == ComponentType.BATTERY }
            if (hasBattery && !hasResistor) {
                issues += DrcIssue(
                    WarningSeverity.ERROR,
                    "La Luz LED se puede quemar",
                    "Está conectada directo a la Batería, sin nada que frene la energía.",
                    "Pon una Resistencia de 330 Ω entre la Batería y el LED.",
                    led.id
                )
            } else if (!hasResistor && neighbors.isNotEmpty()) {
                issues += DrcIssue(
                    WarningSeverity.WARNING,
                    "LED sin protección",
                    "A esta Luz LED le falta una Resistencia que limite la energía.",
                    "Añade una Resistencia de 220–470 Ω en serie.",
                    led.id
                )
            }
        }
        return issues
    }

    private fun checkChipsPower(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        for (chip in components.filter {
            it.type == ComponentType.TIMER_555 || it.type == ComponentType.OPAMP ||
            it.type == ComponentType.COMPARATOR || it.type == ComponentType.COUNTER_IC ||
            it.type == ComponentType.SHIFT_REGISTER
        }) {
            val pins = wiredPins(chip.id, wires)
            if (pins.isEmpty()) continue
            val powered = pins.any { it >= chip.type.pinCount - 1 } || pins.size >= 2
            if (!powered) {
                issues += DrcIssue(
                    WarningSeverity.WARNING,
                    "${chip.type.displayName} sin energía",
                    "Este chip necesita recibir energía para trabajar.",
                    "Conecta los pines de energía a la Batería y a Tierra.",
                    chip.id
                )
            }
        }
        return issues
    }

    private fun checkPolarity(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        for (cap in components.filter { it.type == ComponentType.CAPACITOR }) {
            val pins = wiredPins(cap.id, wires)
            if (pins.contains(0) && !pins.contains(1) && pins.size > 1) {
                issues += DrcIssue(
                    WarningSeverity.INFO,
                    "Ojo con el Capacitor",
                    "Los capacitores redondos tienen lado positivo y negativo.",
                    "El lado marcado (+) debe ir hacia donde llega más energía.",
                    cap.id
                )
            }
        }
        return issues
    }

    private fun checkBatteries(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        val batteries = components.filter { it.type == ComponentType.BATTERY }
        if (batteries.size > 1) {
            issues += DrcIssue(
                WarningSeverity.WARNING,
                "Varias Baterías",
                "Hay ${batteries.size} baterías en el circuito. Si no están bien conectadas pueden pelearse.",
                "Conéctalas en serie (positivo con negativo) o usa solo una."
            )
        }
        for (battery in batteries) {
            val positive = wires.any {
                (it.fromComponentId == battery.id && it.fromPinIndex == 0) ||
                (it.toComponentId == battery.id && it.toPinIndex == 0)
            }
            val negative = wires.any {
                (it.fromComponentId == battery.id && it.fromPinIndex == 1) ||
                (it.toComponentId == battery.id && it.toPinIndex == 1)
            }
            if (positive != negative) {
                issues += DrcIssue(
                    WarningSeverity.WARNING,
                    "Batería incompleta",
                    "Solo está conectado un lado de la Batería.",
                    "Conecta el positivo y el negativo para que la energía circule.",
                    battery.id
                )
            }
        }
        return issues
    }

    private fun checkRuntime(
        state: SimulationState,
        components: List<CircuitComponent>
    ): List<DrcIssue> {
        val issues = mutableListOf<DrcIssue>()
        for (comp in components) {
            val current = state.componentCurrents[comp.id] ?: 0f
            if (comp.type == ComponentType.LED && current > 0.03f) {
                issues += DrcIssue(
                    WarningSeverity.WARNING,
                    "LED muy exigido",
                    "Pasan ${"%.0f".format(current * 1000)} mA por esta Luz LED (lo normal son 10–20 mA).",
                    "Aumenta el valor de la Resistencia para que dure más.",
                    comp.id
                )
            }
            if (comp.type == ComponentType.RESISTOR && current * current * (comp.properties.resistanceOhms ?: 1f) > 0.25f) {
                issues += DrcIssue(
                    WarningSeverity.INFO,
                    "Resistencia caliente",
                    "Esta Resistencia disipa mucha energía y se va a calentar.",
                    "Usa una Resistencia de mayor potencia o cambia los valores.",
                    comp.id
                )
            }
        }
        return issues
    }

    // ===== Utilidades =====

    private fun wiredPins(componentId: Long, wires: List<CircuitWire>): Set<Int> {
        val pins = mutableSetOf<Int>()
        for (wire in wires) {
            if (wire.fromComponentId == componentId) pins.add(wire.fromPinIndex)
            if (wire.toComponentId == componentId) pins.add(wire.toPinIndex)
        }
        return pins
    }

    private fun neighborsOf(
        componentId: Long,
        wires: List<CircuitWire>,
        components: List<CircuitComponent>
    ): List<CircuitComponent> {
        val ids = mutableSetOf<Long>()
        for (wire in wires) {
            if (wire.fromComponentId == componentId) ids.add(wire.toComponentId)
            if (wire.toComponentId == componentId) ids.add(wire.fromComponentId)
        }
        return components.filter { it.id in ids }
    }
}
