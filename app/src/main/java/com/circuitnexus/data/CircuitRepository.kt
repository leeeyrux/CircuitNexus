package com.circuitnexus.data

import androidx.room.withTransaction
import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CircuitRepository(private val database: CircuitDatabase) {

    // Circuitos
    val allCircuits: Flow<List<Circuit>> = database.circuitDao().getAllCircuits()
    val favoriteCircuits: Flow<List<Circuit>> = database.circuitDao().getFavoriteCircuits()

    suspend fun getCircuit(id: Long) = database.circuitDao().getCircuitById(id).first()

    suspend fun createCircuit(name: String = "Nuevo Circuito"): Long {
        val circuit = Circuit.createNew(name)
        return database.circuitDao().insertCircuit(circuit)
    }

    suspend fun saveCircuit(circuit: Circuit) {
        val updated = circuit.copy(updatedAt = System.currentTimeMillis())
        database.circuitDao().updateCircuit(updated)
    }

    suspend fun deleteCircuit(id: Long) {
        database.withTransaction {
            database.wireDao().deleteAllWires(id)
            database.componentDao().deleteAllComponents(id)
            database.circuitDao().deleteCircuitById(id)
        }
    }

    suspend fun duplicateCircuit(id: Long): Long {
        return database.withTransaction {
            val original = database.circuitDao().getCircuitById(id).first()
                ?: return@withTransaction createCircuit("Copia de circuito")

            val newCircuit = original.copy(
                id = 0,
                name = "${original.name} (Copia)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = database.circuitDao().insertCircuit(newCircuit)

            val components = database.componentDao().getComponentsByCircuit(id).first()
            val newComponents = components.map { it.copy(id = 0, circuitId = newId) }
            val newComponentIds = database.componentDao().insertComponents(newComponents)

            // Mapear IDs antiguos a nuevos
            val idMap = components.indices.associate { components[it].id to newComponentIds[it] }

            val wires = database.wireDao().getWiresByCircuit(id).first()
            val newWires = wires.map { wire ->
                wire.copy(
                    id = 0,
                    circuitId = newId,
                    fromComponentId = idMap[wire.fromComponentId] ?: wire.fromComponentId,
                    toComponentId = idMap[wire.toComponentId] ?: wire.toComponentId
                )
            }
            database.wireDao().insertWires(newWires)

            newId
        }
    }

    suspend fun toggleFavorite(id: Long, favorite: Boolean) {
        database.circuitDao().setFavorite(id, favorite)
    }

    // Componentes
    fun getComponents(circuitId: Long): Flow<List<CircuitComponent>> =
        database.componentDao().getComponentsByCircuit(circuitId)

    suspend fun addComponent(component: CircuitComponent): Long =
        database.componentDao().insertComponent(component)

    suspend fun updateComponent(component: CircuitComponent) =
        database.componentDao().updateComponent(component)

    suspend fun deleteComponent(componentId: Long) {
        database.withTransaction {
            database.wireDao().deleteWiresConnectedToComponent(componentId)
            database.componentDao().deleteComponentById(componentId)
        }
    }

    // Cables
    fun getWires(circuitId: Long): Flow<List<CircuitWire>> =
        database.wireDao().getWiresByCircuit(circuitId)

    suspend fun addWire(wire: CircuitWire): Long =
        database.wireDao().insertWire(wire)

    suspend fun updateWire(wire: CircuitWire) =
        database.wireDao().updateWire(wire)

    suspend fun deleteWire(wireId: Long) =
        database.wireDao().deleteWireById(wireId)

    suspend fun clearCircuit(circuitId: Long) {
        database.withTransaction {
            database.wireDao().deleteAllWires(circuitId)
            database.componentDao().deleteAllComponents(circuitId)
        }
    }

    /**
     * Reescribe el contenido del circuito conservando los IDs.
     * Se usa para autoguardado y para que deshacer/rehacer sea persistente.
     */
    suspend fun replaceCircuitContents(
        circuitId: Long,
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ) {
        database.withTransaction {
            database.wireDao().deleteAllWires(circuitId)
            database.componentDao().deleteAllComponents(circuitId)
            if (components.isNotEmpty()) {
                database.componentDao().insertComponents(
                    components.map { it.copy(circuitId = circuitId) }
                )
            }
            if (wires.isNotEmpty()) {
                database.wireDao().insertWires(
                    wires.map { it.copy(circuitId = circuitId) }
                )
            }
        }
    }

    // Batch operations for templates
    suspend fun addComponents(components: List<CircuitComponent>): List<Long> =
        database.componentDao().insertComponents(components)

    suspend fun addWires(wires: List<CircuitWire>) =
        database.wireDao().insertWires(wires)

    suspend fun deleteAllWires(circuitId: Long) =
        database.wireDao().deleteAllWires(circuitId)

    suspend fun deleteAllComponents(circuitId: Long) =
        database.componentDao().deleteAllComponents(circuitId)
}