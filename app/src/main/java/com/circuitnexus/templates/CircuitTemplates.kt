package com.circuitnexus.templates

import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.ComponentPosition
import com.circuitnexus.model.ComponentProperties

/**
 * Plantillas de circuitos famosos listos para usar
 * Cada plantilla incluye componentes pre-posicionados y conexiones
 */
data class CircuitTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val difficulty: Difficulty,
    val estimatedTimeMinutes: Int,
    val components: List<TemplateComponent>,
    val wires: List<TemplateWire>,
    val learningGoals: List<String>,
    val thumbnailEmoji: String
) {
    fun buildCircuit(baseName: String = name): Circuit {
        return Circuit.createNew(baseName).copy(
            description = description
        )
    }
    
    fun buildComponents(circuitId: Long): List<CircuitComponent> {
        return components.map { tc ->
            CircuitComponent(
                circuitId = circuitId,
                type = tc.type,
                position = tc.position,
                properties = tc.properties,
                label = tc.customLabel ?: tc.type.displayName
            )
        }
    }
    
    fun buildWires(circuitId: Long, componentIds: Map<String, Long>): List<CircuitWire> {
        return wires.map { tw ->
            CircuitWire(
                circuitId = circuitId,
                fromComponentId = componentIds[tw.fromKey]!!,
                fromPinIndex = tw.fromPin,
                toComponentId = componentIds[tw.toKey]!!,
                toPinIndex = tw.toPin
            )
        }
    }
}

data class TemplateComponent(
    val key: String, // Identificador único en la plantilla
    val type: ComponentType,
    val position: ComponentPosition,
    val properties: ComponentProperties = ComponentProperties(),
    val customLabel: String? = null
)

data class TemplateWire(
    val fromKey: String,
    val fromPin: Int,
    val toKey: String,
    val toPin: Int
)

enum class TemplateCategory(val title: String, val icon: String) {
    BASICS("Fundamentos", "🌱"),
    BLINK("Parpadeo y Ritmos", "⚡"),
    SENSORS("Sensores", "👁️"),
    POWER("Fuentes y Carga", "🔋"),
    LOGIC("Lógica Digital", "🧠"),
    FUN("Proyectos Divertidos", "🎮")
}

enum class Difficulty(val label: String, val color: Int) {
    BEGINNER("Principiante", 0xFF00E676),      // Esmeralda
    INTERMEDIATE("Intermedio", 0xFFFF9100),    // Ámbar
    ADVANCED("Avanzado", 0xFF00B0FF),          // Cian
    EXPERT("Experto", 0xFF9C27B0)              // Púrpura
}

object CircuitTemplates {
    
    val ALL_TEMPLATES = listOf(
        // === FUNDAMENTOS ===
        CircuitTemplate(
            id = "basic_flashlight",
            name = "Linterna Simple",
            description = "El circuito más básico: enciende y apaga una luz con un botón.",
            category = TemplateCategory.BASICS,
            difficulty = Difficulty.BEGINNER,
            estimatedTimeMinutes = 3,
            learningGoals = listOf(
                "Cómo fluye la energía en un circuito cerrado",
                "Por qué el LED necesita una resistencia",
                "Cómo el interruptor abre/cierra el camino"
            ),
            thumbnailEmoji = "🔦",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 200f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("sw", ComponentType.SWITCH, ComponentPosition(300f, 200f), ComponentProperties(isNormallyOpen = true)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(500f, 200f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(700f, 200f), ComponentProperties(ledColor = "white", ledForwardVoltage = 3.0f)),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(700f, 400f))
            ),
            wires = listOf(
                TemplateWire("bat", 0, "sw", 0),      // Bat(+) → Switch(in)
                TemplateWire("sw", 1, "r1", 0),       // Switch(out) → R(in)
                TemplateWire("r1", 1, "led", 0),      // R(out) → LED(+)
                TemplateWire("led", 1, "gnd", 0),     // LED(-) → GND
                TemplateWire("bat", 1, "gnd", 0)      // Bat(-) → GND
            )
        ),
        
        CircuitTemplate(
            id = "basic_voltage_divider",
            name = "Divisor de Voltaje",
            description = "Dos resistencias reparten el voltaje. Útil para bajar 9V a 3.3V para sensores.",
            category = TemplateCategory.BASICS,
            difficulty = Difficulty.BEGINNER,
            estimatedTimeMinutes = 5,
            learningGoals = listOf(
                "Ley de Ohm en práctica: V = I × R",
                "Cómo dos resistencias en serie dividen el voltaje",
                "Fórmula: Vout = Vin × (R2 / (R1 + R2))"
            ),
            thumbnailEmoji = "⚖️",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 200f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(300f, 100f), ComponentProperties(resistanceOhms = 10000f), "R1 (10kΩ)"),
                TemplateComponent("r2", ComponentType.RESISTOR, ComponentPosition(300f, 300f), ComponentProperties(resistanceOhms = 4700f), "R2 (4.7kΩ)"),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(300f, 400f))
            ),
            wires = listOf(
                TemplateWire("bat", 0, "r1", 0),
                TemplateWire("r1", 1, "r2", 0),
                TemplateWire("r2", 1, "gnd", 0),
                TemplateWire("bat", 1, "gnd", 0)
            )
        ),
        
        // === PARPADEO Y RITMOS ===
        CircuitTemplate(
            id = "blink_555_astable",
            name = "LED Parpadeante (555 Astable)",
            description = "El clásico: chip 555 en modo astable hace parpadear un LED automáticamente. Cambia R y C para cambiar la velocidad.",
            category = TemplateCategory.BLINK,
            difficulty = Difficulty.BEGINNER,
            estimatedTimeMinutes = 10,
            learningGoals = listOf(
                "Modo astable del 555: oscilador continuo",
                "Fórmula frecuencia: f = 1.44 / ((R1 + 2×R2) × C)",
                "Pin 3 = salida, Pin 2/6 = disparo/umbral, Pin 7 = descarga"
            ),
            thumbnailEmoji = "💡",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 300f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("ic555", ComponentType.TIMER_555, ComponentPosition(350f, 300f), ComponentProperties(timerMode = "astable", frequencyHz = 1f, dutyCycle = 0.5f)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(350f, 100f), ComponentProperties(resistanceOhms = 10000f), "R1 (10kΩ)"),
                TemplateComponent("r2", ComponentType.RESISTOR, ComponentPosition(350f, 500f), ComponentProperties(resistanceOhms = 10000f), "R2 (10kΩ)"),
                TemplateComponent("c1", ComponentType.CAPACITOR, ComponentPosition(550f, 500f), ComponentProperties(capacitanceFarads = 10e-6f), "C (10µF)"),
                TemplateComponent("rled", ComponentType.RESISTOR, ComponentPosition(550f, 150f), ComponentProperties(resistanceOhms = 330f), "R-LED (330Ω)"),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(700f, 150f), ComponentProperties(ledColor = "red", ledForwardVoltage = 2.0f)),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(350f, 550f))
            ),
            wires = listOf(
                // Alimentación 555
                TemplateWire("bat", 0, "ic555", 7),    // Bat(+) → VCC (pin 8)
                TemplateWire("bat", 1, "gnd", 0),      // Bat(-) → GND
                TemplateWire("ic555", 0, "gnd", 0),    // GND (pin 1) → GND
                TemplateWire("ic555", 3, "ic555", 7),  // Reset (pin 4) → VCC
                
                // Red RC
                TemplateWire("ic555", 7, "r1", 0),     // VCC → R1
                TemplateWire("r1", 1, "ic555", 6),     // R1 → Pin 7 (Descarga)
                TemplateWire("ic555", 6, "r2", 0),     // Pin 7 → R2
                TemplateWire("r2", 1, "ic555", 5),     // R2 → Pin 6 (Umbral)
                TemplateWire("ic555", 5, "ic555", 1),  // Pin 6 → Pin 2 (Disparo)
                TemplateWire("ic555", 1, "c1", 0),     // Pin 2 → Capacitor(+)
                TemplateWire("c1", 1, "gnd", 0),       // Capacitor(-) → GND
                TemplateWire("ic555", 4, "gnd", 0),    // Pin 5 (Control) → GND (opcional, estabiliza)
                
                // LED en salida
                TemplateWire("ic555", 2, "rled", 0),   // Pin 3 (Salida) → R-LED
                TemplateWire("rled", 1, "led", 0),     // R-LED → LED(+)
                TemplateWire("led", 1, "gnd", 0)       // LED(-) → GND
            )
        ),
        
        CircuitTemplate(
            id = "blink_555_fast",
            name = "Parpadeo Rápido (555 + Pequeño C)",
            description = "Versión rápida del parpadeo: capacitor pequeño = alta frecuencia. ¡Parece luz continua!",
            category = TemplateCategory.BLINK,
            difficulty = Difficulty.INTERMEDIATE,
            estimatedTimeMinutes = 8,
            learningGoals = listOf(
                "Capacitor pequeño = carga/descarga rápida = alta frecuencia",
                "Persistencia visual: >50Hz parece luz fija",
                "Duty cycle: tiempo encendido vs apagado"
            ),
            thumbnailEmoji = "⚡",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 300f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("ic555", ComponentType.TIMER_555, ComponentPosition(350f, 300f), ComponentProperties(timerMode = "astable", frequencyHz = 100f, dutyCycle = 0.5f)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(350f, 100f), ComponentProperties(resistanceOhms = 1000f), "R1 (1kΩ)"),
                TemplateComponent("r2", ComponentType.RESISTOR, ComponentPosition(350f, 500f), ComponentProperties(resistanceOhms = 1000f), "R2 (1kΩ)"),
                TemplateComponent("c1", ComponentType.CAPACITOR, ComponentPosition(550f, 500f), ComponentProperties(capacitanceFarads = 100e-9f), "C (100nF)"),
                TemplateComponent("rled", ComponentType.RESISTOR, ComponentPosition(550f, 150f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(700f, 150f), ComponentProperties(ledColor = "blue", ledForwardVoltage = 3.0f)),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(350f, 550f))
            ),
            wires = listOf(
                TemplateWire("bat", 0, "ic555", 7),
                TemplateWire("bat", 1, "gnd", 0),
                TemplateWire("ic555", 0, "gnd", 0),
                TemplateWire("ic555", 3, "ic555", 7),
                TemplateWire("ic555", 7, "r1", 0),
                TemplateWire("r1", 1, "ic555", 6),
                TemplateWire("ic555", 6, "r2", 0),
                TemplateWire("r2", 1, "ic555", 5),
                TemplateWire("ic555", 5, "ic555", 1),
                TemplateWire("ic555", 1, "c1", 0),
                TemplateWire("c1", 1, "gnd", 0),
                TemplateWire("ic555", 4, "gnd", 0),
                TemplateWire("ic555", 2, "rled", 0),
                TemplateWire("rled", 1, "led", 0),
                TemplateWire("led", 1, "gnd", 0)
            )
        ),
        
        // === SENSORES ===
        CircuitTemplate(
            id = "sensor_light_ldr",
            name = "Sensor de Luz (LDR + LED)",
            description = "El LED se enciende cuando hay poca luz. Usa un divisor de voltaje con LDR (simulado con resistencia variable).",
            category = TemplateCategory.SENSORS,
            difficulty = Difficulty.INTERMEDIATE,
            estimatedTimeMinutes = 10,
            learningGoals = listOf(
                "LDR: resistencia que cambia con la luz",
                "Divisor de voltaje como sensor",
                "Umbral de disparo del 555 o transistor"
            ),
            thumbnailEmoji = "🌙",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 200f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("ldr", ComponentType.RESISTOR, ComponentPosition(300f, 100f), ComponentProperties(resistanceOhms = 10000f), "LDR (simulado 10kΩ)"),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(300f, 300f), ComponentProperties(resistanceOhms = 10000f), "R fija (10kΩ)"),
                TemplateComponent("ic555", ComponentType.TIMER_555, ComponentPosition(500f, 200f), ComponentProperties(timerMode = "monostable")),
                TemplateComponent("rled", ComponentType.RESISTOR, ComponentPosition(700f, 100f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(850f, 100f), ComponentProperties(ledColor = "white")),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(300f, 400f))
            ),
            wires = listOf(
                TemplateWire("bat", 0, "ldr", 0),
                TemplateWire("ldr", 1, "r1", 0),
                TemplateWire("r1", 1, "gnd", 0),
                TemplateWire("ldr", 1, "ic555", 1), // Punto medio → Pin 2 (Disparo)
                TemplateWire("bat", 1, "gnd", 0),
                TemplateWire("ic555", 0, "gnd", 0),
                TemplateWire("ic555", 3, "bat", 0), // Reset → VCC
                TemplateWire("ic555", 7, "bat", 0), // VCC
                TemplateWire("ic555", 2, "rled", 0), // Salida → R-LED
                TemplateWire("rled", 1, "led", 0),
                TemplateWire("led", 1, "gnd", 0)
            )
        ),
        
        // === FUENTES Y CARGA ===
        CircuitTemplate(
            id = "power_capacitor_charge",
            name = "Carga/Descarga de Capacitor",
            description = "Pulsa el botón: el capacitor se carga lento (LED se enciende suave). Suelta: se descarga por el LED (se apaga suave).",
            category = TemplateCategory.POWER,
            difficulty = Difficulty.BEGINNER,
            estimatedTimeMinutes = 5,
            learningGoals = listOf(
                "Capacitor = depósito de energía",
                "Tiempo de carga: τ = R × C (tau)",
                "5τ = carga completa (~99%)"
            ),
            thumbnailEmoji = "🔋",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 200f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("sw", ComponentType.SWITCH, ComponentPosition(300f, 100f), ComponentProperties(isNormallyOpen = true)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(300f, 300f), ComponentProperties(resistanceOhms = 10000f), "R carga (10kΩ)"),
                TemplateComponent("cap", ComponentType.CAPACITOR, ComponentPosition(500f, 300f), ComponentProperties(capacitanceFarads = 470e-6f), "C (470µF)"),
                TemplateComponent("rled", ComponentType.RESISTOR, ComponentPosition(500f, 100f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(700f, 100f), ComponentProperties(ledColor = "green", ledForwardVoltage = 2.1f)),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(500f, 400f))
            ),
            wires = listOf(
                // Ruta de carga (botón pulsado)
                TemplateWire("bat", 0, "sw", 0),
                TemplateWire("sw", 1, "r1", 0),
                TemplateWire("r1", 1, "cap", 0),
                TemplateWire("cap", 1, "gnd", 0),
                
                // Ruta de descarga (LED siempre conectado al capacitor)
                TemplateWire("cap", 0, "rled", 0),
                TemplateWire("rled", 1, "led", 0),
                TemplateWire("led", 1, "gnd", 0),
                
                TemplateWire("bat", 1, "gnd", 0)
            )
        ),
        
        // === PROYECTOS DIVERTIDOS ===
        CircuitTemplate(
            id = "fun_knight_rider",
            name = "Knight Rider (Escáner LED)",
            description = "Múltiples LEDs que se encienden en secuencia como el coche fantástico. Requiere 555 + contador (simulado con múltiples 555).",
            category = TemplateCategory.FUN,
            difficulty = Difficulty.ADVANCED,
            estimatedTimeMinutes = 20,
            learningGoals = listOf(
                "Secuenciación de salidas",
                "Múltiples 555 sincronizados",
                "Efecto visual con retardos RC"
            ),
            thumbnailEmoji = "🚗",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 300f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("ic555_1", ComponentType.TIMER_555, ComponentPosition(300f, 100f), ComponentProperties(timerMode = "astable", frequencyHz = 2f)),
                TemplateComponent("ic555_2", ComponentType.TIMER_555, ComponentPosition(300f, 300f), ComponentProperties(timerMode = "astable", frequencyHz = 2f)),
                TemplateComponent("ic555_3", ComponentType.TIMER_555, ComponentPosition(300f, 500f), ComponentProperties(timerMode = "astable", frequencyHz = 2f)),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(300f, 50f), ComponentProperties(resistanceOhms = 4700f)),
                TemplateComponent("c1", ComponentType.CAPACITOR, ComponentPosition(500f, 50f), ComponentProperties(capacitanceFarads = 10e-6f)),
                TemplateComponent("r2", ComponentType.RESISTOR, ComponentPosition(300f, 250f), ComponentProperties(resistanceOhms = 4700f)),
                TemplateComponent("c2", ComponentType.CAPACITOR, ComponentPosition(500f, 250f), ComponentProperties(capacitanceFarads = 10e-6f)),
                TemplateComponent("r3", ComponentType.RESISTOR, ComponentPosition(300f, 450f), ComponentProperties(resistanceOhms = 4700f)),
                TemplateComponent("c3", ComponentType.CAPACITOR, ComponentPosition(500f, 450f), ComponentProperties(capacitanceFarads = 10e-6f)),
                TemplateComponent("rled1", ComponentType.RESISTOR, ComponentPosition(650f, 100f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led1", ComponentType.LED, ComponentPosition(800f, 100f), ComponentProperties(ledColor = "red")),
                TemplateComponent("rled2", ComponentType.RESISTOR, ComponentPosition(650f, 300f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led2", ComponentType.LED, ComponentPosition(800f, 300f), ComponentProperties(ledColor = "red")),
                TemplateComponent("rled3", ComponentType.RESISTOR, ComponentPosition(650f, 500f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led3", ComponentType.LED, ComponentPosition(800f, 500f), ComponentProperties(ledColor = "red")),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(300f, 600f))
            ),
            wires = listOf(
                // Alimentación común
                TemplateWire("bat", 0, "ic555_1", 7),
                TemplateWire("bat", 0, "ic555_2", 7),
                TemplateWire("bat", 0, "ic555_3", 7),
                TemplateWire("bat", 1, "gnd", 0),
                TemplateWire("ic555_1", 0, "gnd", 0),
                TemplateWire("ic555_2", 0, "gnd", 0),
                TemplateWire("ic555_3", 0, "gnd", 0),
                TemplateWire("ic555_1", 3, "ic555_1", 7),
                TemplateWire("ic555_2", 3, "ic555_2", 7),
                TemplateWire("ic555_3", 3, "ic555_3", 7),
                
                // RC para cada 555 (desfasados)
                TemplateWire("ic555_1", 7, "r1", 0),
                TemplateWire("r1", 1, "ic555_1", 6),
                TemplateWire("ic555_1", 6, "ic555_1", 5),
                TemplateWire("ic555_1", 5, "ic555_1", 1),
                TemplateWire("ic555_1", 1, "c1", 0),
                TemplateWire("c1", 1, "gnd", 0),
                
                TemplateWire("ic555_2", 7, "r2", 0),
                TemplateWire("r2", 1, "ic555_2", 6),
                TemplateWire("ic555_2", 6, "ic555_2", 5),
                TemplateWire("ic555_2", 5, "ic555_2", 1),
                TemplateWire("ic555_2", 1, "c2", 0),
                TemplateWire("c2", 1, "gnd", 0),
                
                TemplateWire("ic555_3", 7, "r3", 0),
                TemplateWire("r3", 1, "ic555_3", 6),
                TemplateWire("ic555_3", 6, "ic555_3", 5),
                TemplateWire("ic555_3", 5, "ic555_3", 1),
                TemplateWire("ic555_3", 1, "c3", 0),
                TemplateWire("c3", 1, "gnd", 0),
                
                // LEDs en salidas
                TemplateWire("ic555_1", 2, "rled1", 0),
                TemplateWire("rled1", 1, "led1", 0),
                TemplateWire("led1", 1, "gnd", 0),
                
                TemplateWire("ic555_2", 2, "rled2", 0),
                TemplateWire("rled2", 1, "led2", 0),
                TemplateWire("led2", 1, "gnd", 0),
                
                TemplateWire("ic555_3", 2, "rled3", 0),
                TemplateWire("rled3", 1, "led3", 0),
                TemplateWire("led3", 1, "gnd", 0)
            )
        ),
        
        CircuitTemplate(
            id = "fun_touch_switch",
            name = "Interruptor Táctil (555 Monoestable)",
            description = "Tocas dos cables expuestos: el 555 en monoestable enciende el LED por un tiempo fijo. ¡Como interruptor de escalera!",
            category = TemplateCategory.FUN,
            difficulty = Difficulty.INTERMEDIATE,
            estimatedTimeMinutes = 10,
            learningGoals = listOf(
                "Modo monoestable: un pulso por disparo",
                "Tiempo = 1.1 × R × C",
                "Aplicación real: temporizador de escalera"
            ),
            thumbnailEmoji = "👆",
            components = listOf(
                TemplateComponent("bat", ComponentType.BATTERY, ComponentPosition(100f, 300f), ComponentProperties(voltageVolts = 9f)),
                TemplateComponent("ic555", ComponentType.TIMER_555, ComponentPosition(350f, 300f), ComponentProperties(timerMode = "monostable")),
                TemplateComponent("r1", ComponentType.RESISTOR, ComponentPosition(350f, 100f), ComponentProperties(resistanceOhms = 100000f), "R (100kΩ)"),
                TemplateComponent("c1", ComponentType.CAPACITOR, ComponentPosition(550f, 100f), ComponentProperties(capacitanceFarads = 10e-6f), "C (10µF)"),
                TemplateComponent("touch1", ComponentType.RESISTOR, ComponentPosition(350f, 450f), ComponentProperties(resistanceOhms = 10000000f), "Toque 1 (10MΩ piel)"),
                TemplateComponent("touch2", ComponentType.RESISTOR, ComponentPosition(350f, 550f), ComponentProperties(resistanceOhms = 10000000f), "Toque 2 (10MΩ piel)"),
                TemplateComponent("rled", ComponentType.RESISTOR, ComponentPosition(550f, 450f), ComponentProperties(resistanceOhms = 330f)),
                TemplateComponent("led", ComponentType.LED, ComponentPosition(700f, 450f), ComponentProperties(ledColor = "blue", ledForwardVoltage = 3.0f)),
                TemplateComponent("gnd", ComponentType.GROUND, ComponentPosition(350f, 600f))
            ),
            wires = listOf(
                TemplateWire("bat", 0, "ic555", 7),
                TemplateWire("bat", 1, "gnd", 0),
                TemplateWire("ic555", 0, "gnd", 0),
                TemplateWire("ic555", 3, "ic555", 7),
                
                // RC de temporización
                TemplateWire("ic555", 7, "r1", 0),
                TemplateWire("r1", 1, "ic555", 6),
                TemplateWire("ic555", 6, "ic555", 5),
                TemplateWire("ic555", 5, "c1", 0),
                TemplateWire("c1", 1, "gnd", 0),
                TemplateWire("ic555", 4, "gnd", 0),
                
                // Disparo por toque (Pin 2 a GND a través de piel)
                TemplateWire("ic555", 1, "touch1", 0),
                TemplateWire("touch1", 1, "touch2", 0),
                TemplateWire("touch2", 1, "gnd", 0),
                
                // LED en salida
                TemplateWire("ic555", 2, "rled", 0),
                TemplateWire("rled", 1, "led", 0),
                TemplateWire("led", 1, "gnd", 0)
            )
        )
    )
    
    // Helpers
    fun getById(id: String): CircuitTemplate? = ALL_TEMPLATES.find { it.id == id }
    fun getByCategory(category: TemplateCategory): List<CircuitTemplate> = ALL_TEMPLATES.filter { it.category == category }
    fun getByDifficulty(difficulty: Difficulty): List<CircuitTemplate> = ALL_TEMPLATES.filter { it.difficulty == difficulty }
}