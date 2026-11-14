package com.circuitnexus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import kotlinx.coroutines.flow.Flow

@Dao
interface CircuitDao {

    @Query("SELECT * FROM circuits ORDER BY updatedAt DESC")
    fun getAllCircuits(): Flow<List<Circuit>>

    @Query("SELECT * FROM circuits WHERE id = :id")
    fun getCircuitById(id: Long): Flow<Circuit?>

    @Query("SELECT * FROM circuits WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteCircuits(): Flow<List<Circuit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircuit(circuit: Circuit): Long

    @Update
    suspend fun updateCircuit(circuit: Circuit): Int

    @Delete
    suspend fun deleteCircuit(circuit: Circuit): Int

    @Query("DELETE FROM circuits WHERE id = :id")
    suspend fun deleteCircuitById(id: Long): Int

    @Query("UPDATE circuits SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean): Int

    @Query("UPDATE circuits SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchCircuit(id: Long, updatedAt: Long): Int
}

@Dao
interface CircuitComponentDao {

    @Query("SELECT * FROM circuit_components WHERE circuitId = :circuitId")
    fun getComponentsByCircuit(circuitId: Long): Flow<List<CircuitComponent>>

    @Query("SELECT * FROM circuit_components WHERE circuitId = :circuitId AND id = :id")
    suspend fun getComponent(circuitId: Long, id: Long): CircuitComponent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: CircuitComponent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<CircuitComponent>): List<Long>

    @Update
    suspend fun updateComponent(component: CircuitComponent): Int

    @Delete
    suspend fun deleteComponent(component: CircuitComponent): Int

    @Query("DELETE FROM circuit_components WHERE circuitId = :circuitId")
    suspend fun deleteAllComponents(circuitId: Long): Int

    @Query("DELETE FROM circuit_components WHERE id = :id")
    suspend fun deleteComponentById(id: Long): Int
}

@Dao
interface CircuitWireDao {

    @Query("SELECT * FROM circuit_wires WHERE circuitId = :circuitId")
    fun getWiresByCircuit(circuitId: Long): Flow<List<CircuitWire>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWire(wire: CircuitWire): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWires(wires: List<CircuitWire>): List<Long>

    @Update
    suspend fun updateWire(wire: CircuitWire): Int

    @Delete
    suspend fun deleteWire(wire: CircuitWire): Int

    @Query("DELETE FROM circuit_wires WHERE circuitId = :circuitId")
    suspend fun deleteAllWires(circuitId: Long): Int

    @Query("DELETE FROM circuit_wires WHERE id = :id")
    suspend fun deleteWireById(id: Long): Int

    @Query("DELETE FROM circuit_wires WHERE fromComponentId = :componentId OR toComponentId = :componentId")
    suspend fun deleteWiresConnectedToComponent(componentId: Long): Int
}