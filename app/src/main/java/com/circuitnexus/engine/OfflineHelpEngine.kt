package com.circuitnexus.engine

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationWarning
import com.circuitnexus.model.WarningSeverity

/**
 * Motor de ayuda instantánea sin internet
 * Analiza el circuito y da consejos claros en español cotidiano
 */
class OfflineHelpEngine {

    /**
     * Genera consejos contextuales basados en el estado actual del circuito
     */
    fun analyzeCircuit(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        warnings: List<SimulationWarning>
    ): List<HelpCard> {
        val cards = mutableListOf<HelpCard>()

        // 1. Convertir warnings del motor en tarjetas de ayuda
        for (warning in warnings) {
            cards.add(HelpCard(
                type = when (warning.severity) {
                    WarningSeverity.ERROR -> HelpType.DANGER
                    WarningSeverity.WARNING -> HelpType.CAUTION
                    WarningSeverity.INFO -> HelpType.TIP
                },
                title = getFriendlyTitle(warning),
                message = warning.message,
                actionText = warning.suggestion,
                relatedComponentId = warning.componentId,
                relatedWireId = warning.wireId
            ))
        }

        // 2. Análisis estructural del circuito
        cards.addAll(analyzeStructure(components, wires))

        // 3. Consejos por tipo de componente presente
        cards.addAll(analyzeComponents(components, wires))

        // 4. Patrones comunes detectados
        cards.addAll(detectPatterns(components, wires))

        return cards.distinctBy { it.message }.take(5) // Máximo 5 tarjetas
    }

    private fun analyzeStructure(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<HelpCard> {
        val cards = mutableListOf<HelpCard>()

        if (components.isEmpty()) {
            cards.add(HelpCard(
                type = HelpType.WELCOME,
                title = "¡Bienvenido a tu Mesa de Trabajo!",
                message = "Arrastra piezas del cajón de abajo para empezar tu primer circuito.",
                actionText = "Abre el cajón de piezas 👇"
            ))
            return cards
        }

        val hasBattery = components.any { it.type == ComponentType.BATTERY }
        val hasGround = components.any { it.type == ComponentType.GROUND }
        val hasLoad = components.any { it.type in setOf(ComponentType.LED, ComponentType.RESISTOR, ComponentType.CAPACITOR, ComponentType.TIMER_555) }
        val wireCount = wires.size

        // Circuito sin fuente de energía
        if (!hasBattery && hasLoad) {
            cards.add(HelpCard(
                type = HelpType.TIP,
                title = "Falta energía",
                message = "Tienes piezas que necesitan energía pero no hay ninguna Batería conectada.",
                actionText = "Añade una Batería del cajón de piezas"
            ))
        }

        // Circuito sin referencia (tierra)
        if (hasBattery && !hasGround && wireCount > 0) {
            cards.add(HelpCard(
                type = HelpType.TIP,
                title = "Falta el retorno",
                message = "La Batería necesita un camino de vuelta (Negativo/Tierra) para que fluya la energía.",
                actionText = "Conecta el Negativo (−) de la Batería a una Tierra"
            ))
        }

        // Batería sola sin nada conectado
        if (hasBattery && !hasLoad && wireCount == 0) {
            cards.add(HelpCard(
                type = HelpType.TIP,
                title = "Batería solitaria",
                message = "Tu Batería está lista pero no hace nada sola. Conéctala a algo.",
                actionText = "Prueba con una Luz LED y una Resistencia"
            ))
        }

        // Muchos componentes pero pocos cables
        if (components.size > 3 && wireCount < components.size - 1) {
            cards.add(HelpCard(
                type = HelpType.TIP,
                title = "Piezas sueltas",
                message = "Tienes ${components.size} piezas pero solo $wireCount cables. Algunas no están conectadas.",
                actionText = "Une los puntos de conexión tocándolos"
            ))
        }

        return cards
    }

    private fun analyzeComponents(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<HelpCard> {
        val cards = mutableListOf<HelpCard>()

        // LED sin resistencia
        val leds = components.filter { it.type == ComponentType.LED }
        for (led in leds) {
            val connectedWires = wires.filter { it.fromComponentId == led.id || it.toComponentId == led.id }
            val hasResistor = connectedWires.any { wire ->
                val otherId = if (wire.fromComponentId == led.id) wire.toComponentId else wire.fromComponentId
                components.firstOrNull { it.id == otherId }?.type == ComponentType.RESISTOR
            }
            val hasBatteryDirect = connectedWires.any { wire ->
                val otherId = if (wire.fromComponentId == led.id) wire.toComponentId else wire.fromComponentId
                components.firstOrNull { it.id == otherId }?.type == ComponentType.BATTERY
            }

            if (hasBatteryDirect && !hasResistor) {
                cards.add(HelpCard(
                    type = HelpType.DANGER,
                    title = "LED en peligro",
                    message = "Esta Luz LED está conectada directo a la Batería sin protección. Se quemará en segundos.",
                    actionText = "Pon una Resistencia de 330 Ω entre la Batería y el LED",
                    relatedComponentId = led.id
                ))
            } else if (!hasResistor && connectedWires.isNotEmpty()) {
                cards.add(HelpCard(
                    type = HelpType.CAUTION,
                    title = "LED sin freno",
                    message = "Esta Luz LED no tiene Resistencia limitando la energía. Podría durar menos.",
                    actionText = "Añade una Resistencia de 220-470 Ω en serie",
                    relatedComponentId = led.id
                ))
            }
        }

        // Capacitor electrolítico polaridad
        val caps = components.filter { it.type == ComponentType.CAPACITOR }
        for (cap in caps) {
            val connectedWires = wires.filter { it.fromComponentId == cap.id || it.toComponentId == cap.id }
            if (connectedWires.size >= 2) {
                cards.add(HelpCard(
                    type = HelpType.TIP,
                    title = "Ojo con el Capacitor",
                    message = "Los capacitores redondos (electrolíticos) tienen positivo y negativo. Conéctalos bien o pueden hincharse.",
                    actionText = "El lado largo o marcado (+) va al voltaje más alto",
                    relatedComponentId = cap.id
                ))
            }
        }

        // 555 sin alimentación
        val timers = components.filter { it.type == ComponentType.TIMER_555 }
        for (timer in timers) {
            val connectedWires = wires.filter { it.fromComponentId == timer.id || it.toComponentId == timer.id }
            val hasVcc = connectedWires.any { wire ->
                val pin = if (wire.fromComponentId == timer.id) wire.fromPinIndex else wire.toPinIndex
                pin == 7 // Pin 8 = VCC (índice 7)
            }
            val hasGnd = connectedWires.any { wire ->
                val pin = if (wire.fromComponentId == timer.id) wire.fromPinIndex else wire.toPinIndex
                pin == 0 // Pin 1 = GND (índice 0)
            }

            if (!hasVcc || !hasGnd) {
                cards.add(HelpCard(
                    type = HelpType.TIP,
                    title = "El 555 necesita alimentación",
                    message = "El Chip Temporizador 555 necesita energía en su patita 8 (VCC) y tierra en la 1 (GND).",
                    actionText = "Conecta Batería (+) al pin 8 y (−) al pin 1",
                    relatedComponentId = timer.id
                ))
            }
        }

        return cards
    }

    private fun detectPatterns(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): List<HelpCard> {
        val cards = mutableListOf<HelpCard>()

        // Detectar divisor de voltaje
        val resistors = components.filter { it.type == ComponentType.RESISTOR }
        if (resistors.size >= 2) {
            // Ver si dos resistencias en serie entre VCC y GND
            val r1 = resistors[0]
            val r2 = resistors[1]
            val r1Wires = wires.filter { it.fromComponentId == r1.id || it.toComponentId == r1.id }
            val r2Wires = wires.filter { it.fromComponentId == r2.id || it.toComponentId == r2.id }

            val sharedConnection = r1Wires.any { w1 ->
                r2Wires.any { w2 ->
                    (w1.fromComponentId == w2.fromComponentId || w1.fromComponentId == w2.toComponentId ||
                     w1.toComponentId == w2.fromComponentId || w1.toComponentId == w2.toComponentId) &&
                    w1.fromComponentId != r1.id && w1.toComponentId != r1.id
                }
            }

            if (sharedConnection) {
                cards.add(HelpCard(
                    type = HelpType.INFO,
                    title = "¡Divisor de voltaje detectado!",
                    message = "Dos Resistencias en serie reparten el voltaje. Útil para bajar voltaje a componentes sensibles.",
                    actionText = "El punto medio tiene voltaje = Batería × (R2 / (R1+R2))"
                ))
            }
        }

        // Detectar RC (resistencia + capacitor)
        val hasR = resistors.isNotEmpty()
        val hasC = components.any { it.type == ComponentType.CAPACITOR }
        if (hasR && hasC) {
            cards.add(HelpCard(
                type = HelpType.INFO,
                title = "Circuito RC detectado",
                message = "Resistencia + Capacitor crean temporización: el capacitor se carga despacio y luego suelta la energía.",
                actionText = "Tiempo ≈ Resistencia (Ω) × Capacitor (F) en segundos"
            ))
        }

        // Detectar 555 en astable (con R y C)
        val has555 = components.any { it.type == ComponentType.TIMER_555 }
        if (has555 && hasR && hasC) {
            cards.add(HelpCard(
                type = HelpType.INFO,
                title = "¡Generador de ritmos!",
                message = "El 555 con Resistencia y Capacitor crea una señal que sube y baja (onda cuadrada). Perfecto para hacer parpadear LEDs.",
                actionText = "Frecuencia ≈ 1.44 / ((R1+2×R2) × C)"
            ))
        }

        return cards
    }

    private fun getFriendlyTitle(warning: SimulationWarning): String {
        return when (warning.severity) {
            WarningSeverity.ERROR -> "¡Peligro!"
            WarningSeverity.WARNING -> "¡Cuidado!"
            WarningSeverity.INFO -> "Consejo"
        }
    }

    /**
     * Genera explicación simple de qué hace el circuito completo
     */
    fun explainCircuitFunction(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): String {
        val types = components.map { it.type }.toSet()

        return when {
            types == setOf(ComponentType.BATTERY, ComponentType.LED, ComponentType.RESISTOR) ->
                "Enciende una Luz LED de forma segura. La Resistencia protege al LED de recibir demasiada energía."
            types == setOf(ComponentType.BATTERY, ComponentType.LED, ComponentType.RESISTOR, ComponentType.SWITCH) ->
                "Enciende y apaga una Luz LED con un Botón. Como una linterna casera."
            types.contains(ComponentType.TIMER_555) && types.contains(ComponentType.CAPACITOR) ->
                "Crea un ritmo que hace parpadear una Luz LED automáticamente. ¡Como una luz de Navidad!"
            types.contains(ComponentType.CAPACITOR) && types.contains(ComponentType.RESISTOR) ->
                "Hace que algo ocurra con retardo: se enciende despacio, se apaga despacio, o crea un temporizador."
            types.contains(ComponentType.SWITCH) && types.contains(ComponentType.LED) ->
                "Controlas manualmente cuándo pasa la energía. Tú decides cuándo brilla la luz."
            types.size == 1 && types.contains(ComponentType.BATTERY) ->
                "Solo tienes una Batería. Añade piezas para que la energía haga algo útil."
            types.size == 1 && types.contains(ComponentType.LED) ->
                "Una Luz LED sola no brilla. Necesita energía de una Batería y una Resistencia de protección."
            else ->
                "Tu circuito tiene ${components.size} piezas conectadas con ${wires.size} cables. ¡Experimenta y verás qué pasa!"
        }
    }
}

data class HelpCard(
    val type: HelpType,
    val title: String,
    val message: String,
    val actionText: String,
    val relatedComponentId: Long? = null,
    val relatedWireId: Long? = null
)

enum class HelpType {
    WELCOME,    // Verde - Bienvenida/Éxito
    TIP,        // Cian - Consejo útil
    CAUTION,    // Ámbar - Precaución suave
    DANGER,     // Coral - Peligro real
    INFO        // Azul - Información técnica
}