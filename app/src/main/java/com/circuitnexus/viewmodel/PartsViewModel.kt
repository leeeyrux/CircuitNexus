package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.ComponentCategory

class PartsViewModel : ViewModel() {

    // Todas las piezas disponibles organizadas por categoría (usando ComponentCategory del modelo)
    val allParts = listOf(
        // ENERGÍA
        PartInfo(ComponentType.BATTERY, "Batería / Energía", "La fuente de toda la energía. Como un tanque de agua a presión.", 0xFF00E676.toInt(), "battery"),
        PartInfo(ComponentType.GROUND, "Negativo / Tierra", "El retorno de la energía. El punto de referencia (cero voltios).", 0xFF9E9E9E.toInt(), "ground"),
        PartInfo(ComponentType.CURRENT_SOURCE, "Fuente de energía constante", "Entrega siempre la misma cantidad de energía.", 0xFF00E676.toInt(), "current_source"),

        // CONTROL
        PartInfo(ComponentType.RESISTOR, "Resistencia", "Pone freno a la energía. Protege a los componentes delicados.", 0xFFFF9100.toInt(), "resistor"),
        PartInfo(ComponentType.POTENTIOMETER, "Perilla Reguladora", "Resistencia que puedes ajustar girándola.", 0xFFFF9100.toInt(), "potentiometer"),
        PartInfo(ComponentType.SWITCH, "Botón / Interruptor", "Tú decides cuándo pasa la energía. Abre o cierra el camino.", 0xFF9C27B0.toInt(), "switch"),
        PartInfo(ComponentType.RELAY, "Relé", "Un interruptor movido por un electroimán.", 0xFF9C27B0.toInt(), "relay"),

        // SALIDAS
        PartInfo(ComponentType.LED, "Luz LED", "Se ilumina cuando pasa energía. ¡La recompensa visual!", 0xFFFF5252.toInt(), "led"),
        PartInfo(ComponentType.MOTOR_DC, "Motor de corriente continua", "Gira cuando le llega energía.", 0xFF3F51B5.toInt(), "motor_dc"),
        PartInfo(ComponentType.SERVO, "Servomotor", "Gira a un ángulo exacto que le pidas (0-180°).", 0xFF3F51B5.toInt(), "servo"),
        PartInfo(ComponentType.BUZZER, "Zumbador", "Hace sonido cuando recibe energía.", 0xFF3F51B5.toInt(), "buzzer"),
        PartInfo(ComponentType.SOLENOID, "Electroimán", "Empuja o tira cuando recibe energía.", 0xFF3F51B5.toInt(), "solenoid"),
        PartInfo(ComponentType.STEPPER, "Motor paso a paso", "Gira en pasos pequeños y precisos.", 0xFF3F51B5.toInt(), "stepper"),

        // TIEMPO
        PartInfo(ComponentType.CAPACITOR, "Capacitor", "Un pequeño depósito que se llena y vacía despacio. Crea retardos.", 0xFF00B0FF.toInt(), "capacitor"),
        PartInfo(ComponentType.TIMER_555, "Chip Temporizador 555", "El cerebro que crea ritmos. Hace parpadear, temporizar, repetir.", 0xFF673AB7.toInt(), "timer555"),
        PartInfo(ComponentType.INDUCTOR, "Bobina", "Guarda energía en forma de magnetismo.", 0xFF00B0FF.toInt(), "inductor"),

        // SEMICONDUCTORES
        PartInfo(ComponentType.DIODE, "Diodo", "Deja pasar la energía solo en un sentido.", 0xFFE91E63.toInt(), "diode"),
        PartInfo(ComponentType.TRANSISTOR_SWITCH, "Transistor interruptor", "Controla mucha energía con poca señal.", 0xFFE91E63.toInt(), "transistor_switch"),
        PartInfo(ComponentType.BJT_NPN, "Interruptor electrónico NPN", "Con una pequeña señal controla una grande.", 0xFFE91E63.toInt(), "bjt_npn"),
        PartInfo(ComponentType.MOSFET_N, "Interruptor de energía N", "Interruptor controlado por voltaje, muy eficiente.", 0xFFE91E63.toInt(), "mosfet_n"),

        // CHIPS
        PartInfo(ComponentType.OPAMP, "Amplificador operacional", "Amplifica señales pequeñas.", 0xFF607D8B.toInt(), "opamp"),
        PartInfo(ComponentType.COMPARATOR, "Comparador", "Compara dos voltajes y avisa cuál es mayor.", 0xFF607D8B.toInt(), "comparator"),
        PartInfo(ComponentType.LOGIC_GATE, "Compuerta lógica", "Toma decisiones: Y, O, NO...", 0xFF607D8B.toInt(), "logic_gate"),

        // SENSORES
        PartInfo(ComponentType.SENSOR_TEMP, "Sensor de temperatura", "Mide qué tan caliente está algo.", 0xFF795548.toInt(), "sensor_temp"),
        PartInfo(ComponentType.SENSOR_DISTANCE, "Sensor de distancia", "Mide qué tan lejos está un objeto.", 0xFF795548.toInt(), "sensor_distance"),
        PartInfo(ComponentType.SENSOR_MOTION, "Sensor de movimiento", "Detecta cuando algo se mueve.", 0xFF795548.toInt(), "sensor_motion"),
        PartInfo(ComponentType.LDR, "Sensor de luz (resistencia)", "Su resistencia cambia con la luz.", 0xFF795548.toInt(), "ldr"),
        PartInfo(ComponentType.THERMISTOR, "Sensor de calor (resistencia)", "Su resistencia cambia con la temperatura.", 0xFF795548.toInt(), "thermistor"),

        // PANTALLAS
        PartInfo(ComponentType.LCD_16X2, "Pantalla de texto", "Muestra 2 líneas de texto.", 0xFF37474F.toInt(), "lcd_16x2"),
        PartInfo(ComponentType.LCD_16X2_EDITABLE, "Pantalla de texto editable", "Escribe texto en la pantalla táctil.", 0xFF37474F.toInt(), "lcd_16x2_editable"),
        PartInfo(ComponentType.DISPLAY_7SEG_4, "Pantalla de números (4)", "Muestra 4 números como un reloj.", 0xFF37474F.toInt(), "display_7seg_4"),
        PartInfo(ComponentType.DISPLAY_7SEG_BCD, "Display 7 seg BCD", "Muestra números con decodificador BCD.", 0xFF37474F.toInt(), "display_7seg_bcd"),
        PartInfo(ComponentType.OLED_SSD1306, "Pantalla pequeña OLED", "Pantalla diminuta para dibujos y datos.", 0xFF37474F.toInt(), "oled_ssd1306"),

        // CONECTORES
        PartInfo(ComponentType.HEADER, "Conector de pines", "Para unir cables de forma ordenada.", 0xFF546E7A.toInt(), "header"),
        PartInfo(ComponentType.USB_CONNECTOR, "Conector USB", "Entrada de energía o datos USB.", 0xFF546E7A.toInt(), "usb_connector"),

        // SCRATCH
        PartInfo(ComponentType.SCRATCH_EVENT, "Cuando Inicio", "Inicia el programa al tocar bandera verde.", 0xFF4CAF50.toInt(), "scratch_event"),
        PartInfo(ComponentType.SCRATCH_LOOP, "Repetir", "Repite los bloques de adentro.", 0xFF4CAF50.toInt(), "scratch_loop"),
        PartInfo(ComponentType.SCRATCH_IF, "Si... Entonces", "Ejecuta si la condición es verdadera.", 0xFF4CAF50.toInt(), "scratch_if"),
        PartInfo(ComponentType.SCRATCH_VARIABLE, "Variable", "Guarda y usa valores numéricos.", 0xFF4CAF50.toInt(), "scratch_variable"),
        PartInfo(ComponentType.SCRATCH_FUNCTION, "Función", "Crea bloques reutilizables con parámetros.", 0xFF4CAF50.toInt(), "scratch_function")
    )

    // Agrupar por ComponentCategory para la UI
    val partsByCategory = allParts.groupBy { it.type.category }

    data class PartInfo(
        val type: ComponentType,
        val friendlyName: String,
        val description: String,
        val color: Int,
        val learnMoreId: String
    ) {
        val category: ComponentCategory
            get() = type.category
    }
}