package com.circuitnexus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Circuit::class, CircuitComponent::class, CircuitWire::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CircuitDatabase : RoomDatabase() {

    abstract fun circuitDao(): CircuitDao
    abstract fun componentDao(): CircuitComponentDao
    abstract fun wireDao(): CircuitWireDao

    companion object {
        @Volatile private var INSTANCE: CircuitDatabase? = null

        fun getDatabase(context: Context): CircuitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CircuitDatabase::class.java,
                    "circuitnexus_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromComponentPosition(value: String): com.circuitnexus.model.ComponentPosition {
        return kotlinx.serialization.json.Json.decodeFromString(value)
    }

    @androidx.room.TypeConverter
    fun toComponentPosition(position: com.circuitnexus.model.ComponentPosition): String {
        return kotlinx.serialization.json.Json.encodeToString(position)
    }

    @androidx.room.TypeConverter
    fun fromComponentProperties(value: String): com.circuitnexus.model.ComponentProperties {
        return kotlinx.serialization.json.Json.decodeFromString(value)
    }

    @androidx.room.TypeConverter
    fun toComponentProperties(props: com.circuitnexus.model.ComponentProperties): String {
        return kotlinx.serialization.json.Json.encodeToString(props)
    }
}

/**
 * Extensiones para transacciones fáciles
 */
fun CircuitDatabase.runInTransaction(block: () -> Unit) {
    runInTransaction(block)
}

suspend fun <T> CircuitDatabase.withTransaction(block: () -> T): T {
    return withTransaction(block)
}