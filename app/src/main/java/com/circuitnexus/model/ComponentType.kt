package com.circuitnexus.model

/**
 * Categorías amigables de piezas (para agrupar en la biblioteca)
 */
enum class ComponentCategory(val title: String, val icon: String) {
    POWER("Energía", "⚡"),
    PASSIVE("Piezas pasivas", "🔧"),
    SEMICONDUCTOR("Semiconductores", "🔺"),
    CONTROL("Control", "🎛️"),
    IC("Chips / Integrados", "🧠"),
    OUTPUT("Salidas", "🔔"),
    SENSOR("Sensores", "👁️"),
    DISPLAY("Pantallas y luces", "🖥️"),
    CONNECTOR("Conectores", "🔌"),
    WIRE("Cable", "🔗")
}

/**
 * Tipos de componentes con nombres 100% amigables.
 * El usuario NUNCA ve "GND", "VCC", "MOSFET N-channel"...
 * Ve "Negativo / Tierra", "Energía del chip", "Interruptor electrónico".
 *
 * @param pinCount número de puntos de conexión (autoritativo para toda la app)
 */
enum class ComponentType(
    val displayName: String,
    val emoji: String,
    val description: String,
    val category: ComponentCategory = ComponentCategory.PASSIVE,
    val pinCount: Int = 2
) {
    // ===== ENERGÍA =====
    BATTERY("Batería / Energía", "🔋", "Proporciona energía al circuito", ComponentCategory.POWER, 2),
    GROUND("Negativo / Tierra", "⏚", "Referencia de voltaje cero, devuelve la energía", ComponentCategory.POWER, 1),
    CURRENT_SOURCE("Fuente de energía constante", "🔆", "Entrega siempre la misma cantidad de energía", ComponentCategory.POWER, 2),

    // ===== PASIVAS =====
    RESISTOR("Resistencia", "🔧", "Controla cuánta energía pasa por el cable", ComponentCategory.PASSIVE, 2),
    POTENTIOMETER("Perilla Reguladora", "🎚️", "Resistencia que puedes ajustar girándola", ComponentCategory.PASSIVE, 3),
    CAPACITOR("Capacitor", "🔋⚡", "Guarda energía y la suelta despacio", ComponentCategory.PASSIVE, 2),
    INDUCTOR("Bobina", "🌀", "Guarda energía en forma de magnetismo", ComponentCategory.PASSIVE, 2),
    TRANSFORMER("Transformador", "🔁", "Sube o baja el voltaje cambiándolo de lado", ComponentCategory.PASSIVE, 4),
    THERMISTOR("Sensor de calor (resistencia)", "🌡️", "Su resistencia cambia con la temperatura", ComponentCategory.PASSIVE, 2),
    LDR("Sensor de luz (resistencia)", "☀️", "Su resistencia cambia con la luz", ComponentCategory.SENSOR, 2),
    VARISTOR("Protector de picos", "🛡️", "Protege de subidas bruscas de energía", ComponentCategory.PASSIVE, 2),

    // ===== SEMICONDUCTORES =====
    LED("Luz LED", "💡", "Se ilumina cuando pasa energía", ComponentCategory.SEMICONDUCTOR, 2),
    DIODE("Diodo", "➡️", "Deja pasar la energía solo en un sentido", ComponentCategory.SEMICONDUCTOR, 2),
    ZENER_DIODE("Diodo regulador (Zener)", "⏬", "Mantiene el voltaje estable, como un tope", ComponentCategory.SEMICONDUCTOR, 2),
    SCHOTTKY_DIODE("Diodo ultrarrápido", "⚡➡️", "Diodo que reacciona muy rápido", ComponentCategory.SEMICONDUCTOR, 2),
    BJT_NPN("Interruptor electrónico NPN", "🔺", "Con una pequeña señal controla una grande", ComponentCategory.SEMICONDUCTOR, 3),
    BJT_PNP("Interruptor electrónico PNP", "🔻", "Como el NPN pero al revés", ComponentCategory.SEMICONDUCTOR, 3),
    MOSFET_N("Interruptor de energía N", "🎚️⚡", "Interruptor controlado por voltaje, muy eficiente", ComponentCategory.SEMICONDUCTOR, 3),
    MOSFET_P("Interruptor de energía P", "🎚️🔻", "Interruptor de energía tipo P", ComponentCategory.SEMICONDUCTOR, 3),
    JFET_N("Interruptor de señal", "🔀", "Controla el paso de señal con voltaje", ComponentCategory.SEMICONDUCTOR, 3),
    IGBT("Interruptor de potencia", "🚛", "Interruptor para cargas grandes", ComponentCategory.SEMICONDUCTOR, 3),

    // ===== CONTROL =====
    SWITCH("Botón / Interruptor", "🔘", "Abre o cierra el paso de energía", ComponentCategory.CONTROL, 2),
    RELAY("Relé", "🔁🔘", "Un interruptor movido por un electroimán", ComponentCategory.CONTROL, 4),

    // ===== CHIPS =====
    TIMER_555("Chip Temporizador 555", "⏱️", "Crea ritmos, parpadeos y temporizaciones", ComponentCategory.IC, 8),
    OPAMP("Amplificador operacional", "🔊", "Amplifica señales pequeñas", ComponentCategory.IC, 5),
    COMPARATOR("Comparador", "⚖️", "Compara dos voltajes y avisa cuál es mayor", ComponentCategory.IC, 5),
    REGULATOR_LINEAR("Regulador de voltaje", "⚙️", "Entrega un voltaje fijo y estable", ComponentCategory.IC, 3),
    REGULATOR_SWITCHING("Regulador eficiente", "⚡⚙️", "Regula voltaje gastando menos energía", ComponentCategory.IC, 4),
    LOGIC_GATE("Compuerta lógica", "🔀🧠", "Toma decisiones: Y, O, NO...", ComponentCategory.IC, 3),
    COUNTER_IC("Contador", "🔢", "Cuenta pulsos automáticamente", ComponentCategory.IC, 8),
    SHIFT_REGISTER("Desplazador de luces", "💫", "Enciende muchas salidas en secuencia", ComponentCategory.IC, 8),

    // ===== SALIDAS =====
    MOTOR_DC("Motor de corriente continua", "⚙️🌀", "Gira cuando le llega energía", ComponentCategory.OUTPUT, 2),
    SERVO("Servomotor", "🦾", "Gira a un ángulo exacto que le pidas", ComponentCategory.OUTPUT, 3),
    STEPPER("Motor paso a paso", "🎯🌀", "Gira en pasos pequeños y precisos", ComponentCategory.OUTPUT, 4),
    BUZZER("Zumbador", "🔔", "Hace sonido cuando recibe energía", ComponentCategory.OUTPUT, 2),
    SOLENOID("Electroimán", "🧲", "Empuja o tira cuando recibe energía", ComponentCategory.OUTPUT, 2),

    // ===== SENSORES =====
    SENSOR_TEMP("Sensor de temperatura", "🌡️📏", "Mide qué tan caliente está algo", ComponentCategory.SENSOR, 3),
    SENSOR_DISTANCE("Sensor de distancia", "📡", "Mide qué tan lejos está un objeto", ComponentCategory.SENSOR, 4),
    SENSOR_MOTION("Sensor de movimiento", "🏃", "Detecta cuando algo se mueve", ComponentCategory.SENSOR, 3),

    // ===== PANTALLAS Y LUCES =====
    DISPLAY_7SEG_4("Pantalla de números (4)", "🔢", "Muestra 4 números como un reloj", ComponentCategory.DISPLAY, 8),
    DISPLAY_7SEG_8("Pantalla de números (8)", "🔢", "Muestra 8 números en fila", ComponentCategory.DISPLAY, 8),
    DISPLAY_7SEG_10("Pantalla de números (10)", "🔟", "Muestra 10 números en fila", ComponentCategory.DISPLAY, 10),
    LCD_16X2("Pantalla de texto", "📃", "Muestra 2 líneas de texto", ComponentCategory.DISPLAY, 16),
    OLED_SSD1306("Pantalla pequeña OLED", "🖥️", "Pantalla diminuta para dibujos y datos", ComponentCategory.DISPLAY, 4),
    LED_STRIP_WS2812("Tira de luces de colores", "🌈", "Muchos LEDs con color individual", ComponentCategory.DISPLAY, 3),

    // ===== CONECTORES =====
    HEADER("Conector de pines", "📌", "Para unir cables de forma ordenada", ComponentCategory.CONNECTOR, 2),
    TERMINAL_BLOCK("Bornera", "🔩", "Conector atornillado para cables gruesos", ComponentCategory.CONNECTOR, 2),
    JACK("Conector de audio", "🎧", "Entrada/salida de audio", ComponentCategory.CONNECTOR, 2),
    USB_CONNECTOR("Conector USB", "🔌", "Entrada de energía o datos USB", ComponentCategory.CONNECTOR, 4),

    // ===== ESPECIAL =====
    WIRE("Cable", "🔗", "Une dos puntos para que fluya la energía", ComponentCategory.WIRE, 2);

    /** ¿Es una pieza que actúa como resistencia? (para simulación) */
    val behavesAsResistor: Boolean
        get() = this == RESISTOR || this == POTENTIOMETER || this == THERMISTOR ||
                this == LDR || this == VARISTOR || this == LED_STRIP_WS2812

    /** ¿Es una pieza que emite luz? */
    val emitsLight: Boolean
        get() = this == LED || this == LED_STRIP_WS2812 || this == DISPLAY_7SEG_4 ||
                this == DISPLAY_7SEG_8 || this == DISPLAY_7SEG_10

    /** ¿Se puede cambiar alguna propiedad en el panel lateral? */
    val isConfigurable: Boolean
        get() = when (category) {
            ComponentCategory.PASSIVE, ComponentCategory.SEMICONDUCTOR,
            ComponentCategory.CONTROL, ComponentCategory.IC, ComponentCategory.POWER -> true
            else -> false
        }

    companion object {
        /** Piezas que aparecen en el cajón rápido de la mesa de trabajo */
        val QUICK_ACCESS: List<ComponentType> = listOf(
            BATTERY, GROUND, RESISTOR, LED, CAPACITOR, SWITCH, TIMER_555,
            POTENTIOMETER, DIODE, BJT_NPN, MOSFET_N, OPAMP
        )

        /** Todas las piezas que se pueden colocar (sin WIRE) */
        val PLACEABLE: List<ComponentType> = values().filter { it != WIRE }

        fun byCategory(category: ComponentCategory): List<ComponentType> =
            values().filter { it.category == category && it != WIRE }
    }
}
