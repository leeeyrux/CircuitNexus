package com.circuitnexus.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class ComponentPosition(
    val x: Float,
    val y: Float,
    val rotation: Float = 0f
)

/**
 * Propiedades ajustables de cada pieza.
 * Todo con nombres claros; el usuario ve "Voltaje", "Resistencia", no "Vf", "hFE"...
 */
@Serializable
data class ComponentProperties(
    // Pasivos
    val resistanceOhms: Float? = null,          // Resistencia, potenciómetro, sensor
    val capacitanceFarads: Float? = null,       // Capacitor
    val inductanceHenries: Float? = null,       // Bobina
    val potPosition: Float? = null,             // 0..1 posición de la perilla
    val turnsRatio: Float? = null,              // Transformador

    // Energía
    val voltageVolts: Float? = null,            // Batería / fuente
    val currentAmps: Float? = null,             // Fuente de corriente

    // Semiconductores
    val ledColor: String? = null,               // "red", "green", "blue", "yellow", "white"
    val ledForwardVoltage: Float? = null,       // Voltaje de encendido del LED
    val forwardVoltage: Float? = null,          // Vf de diodos
    val currentGain: Float? = null,             // Ganancia de un transistor
    val thresholdVoltage: Float? = null,        // Umbral de un interruptor electrónico
    val breakdownVoltage: Float? = null,        // Voltaje de ruptura (Zener)

    // Control
    val isNormallyOpen: Boolean = true,         // Interruptor / relé
    val coilResistance: Float? = null,          // Resistencia del relé

    // Chips
    val timerMode: String? = null,              // "astable", "monostable"
    val frequencyHz: Float? = null,             // Frecuencia de oscilación
    val dutyCycle: Float? = null,               // Ciclo de trabajo
    val regulationVolts: Float? = null,         // Voltaje regulado
    val gain: Float? = null,                    // Ganancia del amplificador
    val logicFunction: String? = null,          // "AND", "OR", "NOT", "NAND"...

    // Pantallas / salidas
    val text: String? = null,                   // Texto a mostrar en pantalla
    val displayDigits: Int? = null,             // 4, 8 o 10 dígitos
    val brightness: Float? = null,              // 0..1
    val pwmDuty: Float? = null,                 // 0..1 control de brillo/velocidad

    // Sensores (valor simulado que el usuario puede mover)
    val sensorValue: Float? = null              // Valor del sensor (0..1)
)

@Entity(tableName = "circuit_components")
@Serializable
data class CircuitComponent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val circuitId: Long,
    val type: ComponentType,
    val position: ComponentPosition,
    val properties: ComponentProperties = ComponentProperties(),
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createDefault(type: ComponentType, circuitId: Long, x: Float, y: Float): CircuitComponent {
            return CircuitComponent(
                circuitId = circuitId,
                type = type,
                position = ComponentPosition(x, y),
                properties = defaultProperties(type),
                label = type.displayName
            )
        }

        fun defaultProperties(type: ComponentType): ComponentProperties = when (type) {
            // Pasivos
            ComponentType.RESISTOR -> ComponentProperties(resistanceOhms = 330f)
            ComponentType.POTENTIOMETER -> ComponentProperties(resistanceOhms = 10000f, potPosition = 0.5f)
            ComponentType.CAPACITOR -> ComponentProperties(capacitanceFarads = 100e-6f)
            ComponentType.INDUCTOR -> ComponentProperties(inductanceHenries = 10e-3f)
            ComponentType.TRANSFORMER -> ComponentProperties(turnsRatio = 1f)
            ComponentType.THERMISTOR -> ComponentProperties(resistanceOhms = 10000f, sensorValue = 0.5f)
            ComponentType.LDR -> ComponentProperties(resistanceOhms = 10000f, sensorValue = 0.5f)
            ComponentType.VARISTOR -> ComponentProperties(resistanceOhms = 1e6f, breakdownVoltage = 18f)

            // Energía
            ComponentType.BATTERY -> ComponentProperties(voltageVolts = 9f)
            ComponentType.CURRENT_SOURCE -> ComponentProperties(currentAmps = 0.02f)

            // Semiconductores
            ComponentType.LED -> ComponentProperties(ledColor = "red", ledForwardVoltage = 2.0f)
            ComponentType.DIODE -> ComponentProperties(forwardVoltage = 0.7f)
            ComponentType.ZENER_DIODE -> ComponentProperties(forwardVoltage = 0.7f, breakdownVoltage = 5.1f)
            ComponentType.SCHOTTKY_DIODE -> ComponentProperties(forwardVoltage = 0.3f)
            ComponentType.BJT_NPN -> ComponentProperties(currentGain = 100f, forwardVoltage = 0.7f)
            ComponentType.BJT_PNP -> ComponentProperties(currentGain = 100f, forwardVoltage = 0.7f)
            ComponentType.MOSFET_N -> ComponentProperties(thresholdVoltage = 2.0f, currentGain = 1f)
            ComponentType.MOSFET_P -> ComponentProperties(thresholdVoltage = -2.0f, currentGain = 1f)
            ComponentType.JFET_N -> ComponentProperties(thresholdVoltage = -2.0f, currentGain = 1f)
            ComponentType.IGBT -> ComponentProperties(thresholdVoltage = 5.0f, currentGain = 10f)

            // Control
            ComponentType.SWITCH -> ComponentProperties(isNormallyOpen = true)
            ComponentType.RELAY -> ComponentProperties(isNormallyOpen = true, coilResistance = 400f)

            // Chips
            ComponentType.TIMER_555 -> ComponentProperties(
                timerMode = "astable", frequencyHz = 1f, dutyCycle = 0.5f
            )
            ComponentType.OPAMP -> ComponentProperties(gain = 100000f)
            ComponentType.COMPARATOR -> ComponentProperties(gain = 100000f)
            ComponentType.REGULATOR_LINEAR -> ComponentProperties(regulationVolts = 5f)
            ComponentType.REGULATOR_SWITCHING -> ComponentProperties(regulationVolts = 3.3f)
            ComponentType.LOGIC_GATE -> ComponentProperties(logicFunction = "AND")
            ComponentType.COUNTER_IC -> ComponentProperties(frequencyHz = 1f)
            ComponentType.SHIFT_REGISTER -> ComponentProperties(frequencyHz = 2f)

            // Salidas
            ComponentType.MOTOR_DC -> ComponentProperties(resistanceOhms = 12f, pwmDuty = 1f)
            ComponentType.SERVO -> ComponentProperties(pwmDuty = 0.5f)
            ComponentType.STEPPER -> ComponentProperties(frequencyHz = 10f)
            ComponentType.BUZZER -> ComponentProperties(frequencyHz = 440f, voltageVolts = 5f)
            ComponentType.SOLENOID -> ComponentProperties(resistanceOhms = 40f)

            // Sensores
            ComponentType.SENSOR_TEMP -> ComponentProperties(sensorValue = 0.5f, voltageVolts = 0.5f)
            ComponentType.SENSOR_DISTANCE -> ComponentProperties(sensorValue = 0.5f)
            ComponentType.SENSOR_MOTION -> ComponentProperties(sensorValue = 0f)

            // Pantallas
            ComponentType.DISPLAY_7SEG_4 -> ComponentProperties(displayDigits = 4, text = "0000", brightness = 1f)
            ComponentType.DISPLAY_7SEG_8 -> ComponentProperties(displayDigits = 8, text = "00000000", brightness = 1f)
            ComponentType.DISPLAY_7SEG_10 -> ComponentProperties(displayDigits = 10, text = "0000000000", brightness = 1f)
            ComponentType.LCD_16X2 -> ComponentProperties(text = "CircuitNexus", brightness = 1f)
            ComponentType.LCD_16X2_EDITABLE -> ComponentProperties(displayDigits = 16, text = "", brightness = 1f)
            ComponentType.DISPLAY_7SEG_BCD -> ComponentProperties(displayDigits = 4, text = "0000", brightness = 1f)
            ComponentType.OLED_SSD1306 -> ComponentProperties(text = "Hola!", brightness = 1f)
            ComponentType.LED_STRIP_WS2812 -> ComponentProperties(ledColor = "rainbow", brightness = 0.8f, resistanceOhms = 330f)

            // Lógica 74HC
            ComponentType.LOGIC_AND -> ComponentProperties(logicFunction = "AND")
            ComponentType.LOGIC_OR -> ComponentProperties(logicFunction = "OR")
            ComponentType.LOGIC_NOT -> ComponentProperties(logicFunction = "NOT")
            ComponentType.LOGIC_NAND -> ComponentProperties(logicFunction = "NAND")
            ComponentType.LOGIC_NOR -> ComponentProperties(logicFunction = "NOR")
            ComponentType.LOGIC_XOR -> ComponentProperties(logicFunction = "XOR")

            // Salidas avanzadas
            ComponentType.SERVO -> ComponentProperties(pwmDuty = 0.5f, servoAngle = 90f)
            ComponentType.RELAY -> ComponentProperties(isNormallyOpen = true, coilResistance = 400f)
            ComponentType.TRANSISTOR_SWITCH -> ComponentProperties(thresholdVoltage = 2.0f, currentGain = 1f)

            // Scratch blocks
            ComponentType.SCRATCH_EVENT -> ComponentProperties()
            ComponentType.SCRATCH_LOOP -> ComponentProperties()
            ComponentType.SCRATCH_IF -> ComponentProperties()
            ComponentType.SCRATCH_WAIT -> ComponentProperties()
            ComponentType.SCRATCH_SET_PIN -> ComponentProperties()
            ComponentType.SCRATCH_READ_PIN -> ComponentProperties()
            ComponentType.SCRATCH_MATH -> ComponentProperties()

            // Pantallas BCD y editable
            ComponentType.DISPLAY_7SEG_BCD -> ComponentProperties(displayDigits = 4, text = "0000", brightness = 1f)
            ComponentType.LCD_16X2_EDITABLE -> ComponentProperties(displayDigits = 16, text = "", brightness = 1f)
            ComponentType.DISPLAY_7SEG_4 -> ComponentProperties(displayDigits = 4, text = "0000", brightness = 1f)
            ComponentType.DISPLAY_7SEG_8 -> ComponentProperties(displayDigits = 8, text = "00000000", brightness = 1f)
            ComponentType.DISPLAY_7SEG_10 -> ComponentProperties(displayDigits = 10, text = "0000000000", brightness = 1f)
            ComponentType.LCD_16X2 -> ComponentProperties(text = "CircuitNexus", brightness = 1f)
            ComponentType.OLED_SSD1306 -> ComponentProperties(text = "Hola!", brightness = 1f)
            ComponentType.LED_STRIP_WS2812 -> ComponentProperties(ledColor = "rainbow", brightness = 0.8f, resistanceOhms = 330f)

            // Conectores
            else -> ComponentProperties()
        }
    }
}
