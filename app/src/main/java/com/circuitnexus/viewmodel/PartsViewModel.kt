package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import com.circuitnexus.model.ComponentType

class PartsViewModel : ViewModel() {

    // Todas las piezas disponibles organizadas por categoría
    val allParts = listOf(
        PartInfo(
            type = ComponentType.BATTERY,
            category = PartCategory.POWER,
            friendlyName = "Batería / Energía",
            description = "La fuente de toda la energía. Como un tanque de agua a presión.",
            color = 0xFF00E676,
            learnMoreId = "battery"
        ),
        PartInfo(
            type = ComponentType.GROUND,
            category = PartCategory.POWER,
            friendlyName = "Negativo / Tierra",
            description = "El retorno de la energía. El punto de referencia (cero voltios).",
            color = 0xFF9E9E9E,
            learnMoreId = "ground"
        ),
        PartInfo(
            type = ComponentType.RESISTOR,
            category = PartCategory.CONTROL,
            friendlyName = "Resistencia",
            description = "Pone freno a la energía. Protege a los componentes delicados.",
            color = 0xFFFF9100,
            learnMoreId = "resistor"
        ),
        PartInfo(
            type = ComponentType.LED,
            category = PartCategory.OUTPUT,
            friendlyName = "Luz LED",
            description = "Se ilumina cuando pasa energía. ¡La recompensa visual!",
            color = 0xFFFF5252,
            learnMoreId = "led"
        ),
        PartInfo(
            type = ComponentType.CAPACITOR,
            category = PartCategory.TIMING,
            friendlyName = "Capacitor",
            description = "Un pequeño depósito que se llena y vacía despacio. Crea retardos.",
            color = 0xFF00B0FF,
            learnMoreId = "capacitor"
        ),
        PartInfo(
            type = ComponentType.SWITCH,
            category = PartCategory.CONTROL,
            friendlyName = "Botón / Interruptor",
            description = "Tú decides cuándo pasa la energía. Abre o cierra el camino.",
            color = 0xFF9C27B0,
            learnMoreId = "switch"
        ),
        PartInfo(
            type = ComponentType.TIMER_555,
            category = PartCategory.TIMING,
            friendlyName = "Chip Temporizador 555",
            description = "El cerebro que crea ritmos. Hace parpadear, temporizar, repetir.",
            color = 0xFF673AB7,
            learnMoreId = "timer555"
        )
    )

    val powerParts = allParts.filter { it.category == PartCategory.POWER }
    val controlParts = allParts.filter { it.category == PartCategory.CONTROL }
    val outputParts = allParts.filter { it.category == PartCategory.OUTPUT }
    val timingParts = allParts.filter { it.category == PartCategory.TIMING }

    data class PartInfo(
        val type: ComponentType,
        val category: PartCategory,
        val friendlyName: String,
        val description: String,
        val color: Int,
        val learnMoreId: String
    )

    enum class PartCategory(val title: String, val icon: String) {
        POWER("Energía", "⚡"),
        CONTROL("Control", "🎛️"),
        OUTPUT("Salida", "💡"),
        TIMING("Tiempo", "⏱️")
    }
}