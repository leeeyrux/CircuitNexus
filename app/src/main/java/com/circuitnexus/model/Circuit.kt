package com.circuitnexus.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Un circuito guardado por el usuario
 * Amigable: "Mi proyecto de circuito"
 */
@Entity(tableName = "circuits")
@Serializable
data class Circuit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Mi Circuito",
    val description: String = "",
    val thumbnailData: String = "",           // Base64 encoded preview
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val version: Int = 1
) {
    companion object {
        fun createNew(name: String = "Nuevo Circuito"): Circuit {
            val now = System.currentTimeMillis()
            return Circuit(
                name = name,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

/**
 * Estado de simulación en tiempo real
 */
@Serializable
data class SimulationState(
    val isRunning: Boolean = false,
    val nodeVoltages: Map<Int, Float> = emptyMap(),    // Nodo -> Voltaje
    val componentCurrents: Map<Long, Float> = emptyMap(), // ComponentId -> Corriente
    val wireCurrents: Map<Long, Float> = emptyMap(),   // WireId -> Corriente
    val powerConsumption: Float = 0f,                  // Watts totales
    val warnings: List<SimulationWarning> = emptyList(),
    val lastUpdate: Long = System.currentTimeMillis()
)

@Serializable
data class SimulationWarning(
    val severity: WarningSeverity,
    val message: String,              // Mensaje amigable en español
    val componentId: Long? = null,    // Componente relacionado
    val wireId: Long? = null,         // Cable relacionado
    val suggestion: String = ""       // Qué hacer para arreglarlo
)

enum class WarningSeverity {
    INFO,       // Azul - Informativo
    WARNING,    // Ámbar - Cuidado suave
    ERROR       // Coral - Peligro real
}

/**
 * Resultado de medición en un punto
 */
@Serializable
data class Measurement(
    val voltage: Float = 0f,          // Voltaje en el nodo
    val current: Float = 0f,          // Corriente por el cable
    val power: Float = 0f,            // Potencia disipada
    val timestamp: Long = System.currentTimeMillis()
)