package com.circuitnexus.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Conexión entre dos pines de componentes.
 * Amigable: "Cable que une dos piezas"
 */
@Entity(
    tableName = "circuit_wires",
    foreignKeys = [
        ForeignKey(
            entity = CircuitComponent::class,
            parentColumns = ["id"],
            childColumns = ["fromComponentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CircuitComponent::class,
            parentColumns = ["id"],
            childColumns = ["toComponentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fromComponentId"),
        Index("toComponentId")
    ]
)
@Serializable
data class CircuitWire(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val circuitId: Long,
    val fromComponentId: Long,
    val fromPinIndex: Int,
    val toComponentId: Long,
    val toPinIndex: Int,
    val color: String = "#00E676",
    val isAnimated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {

        fun getPinCount(type: ComponentType): Int = type.pinCount

        /**
         * Nombres de pines en lenguaje cotidiano.
         * Nunca "VCC", "GND", "SDA"...
         */
        fun getPinNames(type: ComponentType): List<String> = when (type) {
            ComponentType.BATTERY -> listOf("Positivo (+)", "Negativo (−)")
            ComponentType.GROUND -> listOf("Tierra")
            ComponentType.CURRENT_SOURCE -> listOf("Sale energía", "Vuelve energía")
            ComponentType.RESISTOR -> listOf("Entrada", "Salida")
            ComponentType.POTENTIOMETER -> listOf("Entrada", "Salida regulada", "Tierra")
            ComponentType.CAPACITOR -> listOf("Positivo (+)", "Negativo (−)")
            ComponentType.INDUCTOR -> listOf("Entrada", "Salida")
            ComponentType.TRANSFORMER -> listOf("Entrada +", "Entrada −", "Salida +", "Salida −")
            ComponentType.THERMISTOR, ComponentType.LDR, ComponentType.VARISTOR -> listOf("Entrada", "Salida")
            ComponentType.LED -> listOf("Ánodo (+)", "Cátodo (−)")
            ComponentType.DIODE, ComponentType.SCHOTTKY_DIODE -> listOf("Ánodo (+)", "Cátodo (−)")
            ComponentType.ZENER_DIODE -> listOf("Ánodo (+)", "Cátodo (−)")
            ComponentType.BJT_NPN -> listOf("Colector", "Base", "Emisor")
            ComponentType.BJT_PNP -> listOf("Colector", "Base", "Emisor")
            ComponentType.MOSFET_N, ComponentType.MOSFET_P -> listOf("Drenaje", "Puerta", "Fuente")
            ComponentType.JFET_N -> listOf("Drenaje", "Puerta", "Fuente")
            ComponentType.IGBT -> listOf("Colector", "Puerta", "Emisor")
            ComponentType.SWITCH -> listOf("Entrada", "Salida")
            ComponentType.RELAY -> listOf("Bobina +", "Bobina −", "Contacto común", "Contacto abierto")
            ComponentType.TIMER_555 -> listOf(
                "Tierra", "Disparo", "Salida", "Reinicio",
                "Control", "Umbral", "Descarga", "Energía del chip"
            )
            ComponentType.OPAMP, ComponentType.COMPARATOR -> listOf(
                "Entrada +", "Entrada −", "Energía +", "Energía −", "Salida"
            )
            ComponentType.REGULATOR_LINEAR -> listOf("Entrada", "Salida", "Tierra")
            ComponentType.REGULATOR_SWITCHING -> listOf("Entrada", "Salida", "Tierra", "Control")
            ComponentType.LOGIC_GATE -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.COUNTER_IC -> listOf(
                "Energía +", "Tierra", "Reloj", "Reinicio",
                "Salida A", "Salida B", "Salida C", "Salida D"
            )
            ComponentType.SHIFT_REGISTER -> listOf(
                "Energía +", "Tierra", "Datos", "Reloj",
                "Pestillo", "Salida A", "Salida B", "Salida C"
            )
            ComponentType.MOTOR_DC, ComponentType.BUZZER, ComponentType.SOLENOID -> listOf("Positivo (+)", "Negativo (−)")
            ComponentType.SERVO -> listOf("Energía +", "Tierra", "Señal")
            ComponentType.STEPPER -> listOf("Bobina 1", "Bobina 2", "Bobina 3", "Bobina 4")
            ComponentType.SENSOR_TEMP -> listOf("Energía +", "Tierra", "Señal")
            ComponentType.SENSOR_DISTANCE -> listOf("Energía +", "Tierra", "Disparo", "Eco")
            ComponentType.SENSOR_MOTION -> listOf("Energía +", "Tierra", "Señal")
            ComponentType.DISPLAY_7SEG_4, ComponentType.DISPLAY_7SEG_8 -> listOf(
                "A", "B", "C", "D", "E", "F", "G", "Punto"
            )
            ComponentType.DISPLAY_7SEG_10 -> listOf(
                "A", "B", "C", "D", "E", "F", "G", "Punto", "Energía +", "Tierra"
            )
            // ===== 74HC LÓGICA =====
            ComponentType.LOGIC_AND, ComponentType.LOGIC_OR, ComponentType.LOGIC_XOR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_OR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_NOT -> listOf("Entrada", "Salida")
            ComponentType.LOGIC_NAND, ComponentType.LOGIC_NOR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_XOR -> listOf("Entrada A", "Entrada B", "Salida")
            // ===== SALIDAS AVANZADAS =====
            ComponentType.SERVO -> listOf("Energía +", "Tierra", "Señal PWM")
            ComponentType.RELAY -> listOf("Bobina +", "Bobina −", "Contacto común", "Contacto abierto")
            ComponentType.TRANSISTOR_SWITCH -> listOf("Colector/Drenaje", "Base/Puerta", "Emisor/Fuente")
            // ===== PANTALLAS =====
            ComponentType.LCD_16X2 -> listOf(
                "Tierra", "Energía +", "Contraste", "Datos 0", "Datos 1", "Datos 2", "Datos 3",
                "Datos 4", "Datos 5", "Datos 6", "Datos 7", "Elegir", "Leer/Escribir", "Habilitar",
                "Luz +", "Luz −"
            )
            ComponentType.OLED_SSD1306 -> listOf("Tierra", "Energía +", "Datos", "Reloj")
            ComponentType.LED_STRIP_WS2812 -> listOf("Energía +", "Tierra", "Datos")
            ComponentType.HEADER, ComponentType.TERMINAL_BLOCK, ComponentType.JACK -> listOf("Punto 1", "Punto 2")
            ComponentType.USB_CONNECTOR -> listOf("Energía +", "Tierra", "Datos +", "Datos −")
            // ===== 74HC LÓGICA =====
            ComponentType.LOGIC_AND, ComponentType.LOGIC_OR, ComponentType.LOGIC_XOR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_OR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_NOT -> listOf("Entrada", "Salida")
            ComponentType.LOGIC_NAND, ComponentType.LOGIC_NOR -> listOf("Entrada A", "Entrada B", "Salida")
            ComponentType.LOGIC_XOR -> listOf("Entrada A", "Entrada B", "Salida")
            // ===== SALIDAS AVANZADAS =====
            ComponentType.SERVO -> listOf("Energía +", "Tierra", "Señal PWM")
            ComponentType.RELAY -> listOf("Bobina +", "Bobina −", "Contacto común", "Contacto abierto")
            ComponentType.TRANSISTOR_SWITCH -> listOf("Colector/Drenaje", "Base/Puerta", "Emisor/Fuente")
            // ===== PANTALLAS =====
            ComponentType.LCD_16X2_EDITABLE -> listOf(
                "Tierra", "Energía +", "Contraste", "Datos 0", "Datos 1", "Datos 2", "Datos 3",
                "Datos 4", "Datos 5", "Datos 6", "Datos 7", "Elegir", "Leer/Escribir", "Habilitar",
                "Luz +", "Luz −"
            )
            ComponentType.DISPLAY_7SEG_BCD -> listOf("A", "B", "C", "D", "E", "F", "G", "Punto")
            ComponentType.WIRE -> listOf("Punto 1", "Punto 2")
        }

        /** Descripciones cortas de cada pin para el tutorial y ayudas */
        fun getPinDescriptions(type: ComponentType): List<String> = when (type) {
            ComponentType.BATTERY -> listOf("Da energía al circuito", "Devuelve la energía")
            ComponentType.GROUND -> listOf("Punto de referencia, voltaje cero")
            ComponentType.RESISTOR -> listOf("Por donde entra la energía", "Por donde sale controlada")
            ComponentType.LED -> listOf("Lado positivo (pata larga)", "Lado negativo (pata corta)")
            ComponentType.DIODE -> listOf("Por aquí entra", "Por aquí sale (no vuelve)")
            ComponentType.CAPACITOR -> listOf("Lado positivo", "Lado negativo")
            ComponentType.SWITCH -> listOf("Energía entra aquí", "Energía sale si está cerrado")
            ComponentType.TIMER_555 -> listOf(
                "Referencia (cero voltios)", "Inicia el temporizador", "La salida del temporizador",
                "Reinicia el chip", "Ajuste fino de voltaje", "Detecta cuándo parar",
                "Descarga el capacitor interno", "Alimentación del chip (5-15V)"
            )
            else -> List(type.pinCount) { "Punto de conexión ${it + 1}" }
        }

        /** Devuelve el nombre amigable de un pin concreto */
        fun pinName(type: ComponentType, index: Int): String {
            val names = getPinNames(type)
            return names.getOrElse(index) { "Punto ${index + 1}" }
        }
    }
}
