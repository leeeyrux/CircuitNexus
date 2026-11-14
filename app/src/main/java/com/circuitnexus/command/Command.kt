package com.circuitnexus.command

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Command Pattern para Undo/Redo
 * Cada acción del usuario es un comando reversible
 */
sealed interface Command {
    fun execute()
    fun undo()
    val description: String
}

// ===== Comandos de Componentes =====

data class AddComponentCommand(
    private val repository: CommandRepository,
    private val component: CircuitComponent
) : Command {
    override val description = "Añadir ${component.type.displayName}"
    
    override fun execute() {
        repository.addComponent(component)
    }
    
    override fun undo() {
        repository.deleteComponent(component.id)
    }
}

data class DeleteComponentCommand(
    private val repository: CommandRepository,
    private val component: CircuitComponent,
    private val connectedWires: List<CircuitWire>
) : Command {
    override val description = "Eliminar ${component.type.displayName}"
    
    override fun execute() {
        repository.deleteComponent(component.id)
        connectedWires.forEach { repository.deleteWire(it.id) }
    }
    
    override fun undo() {
        repository.addComponent(component)
        connectedWires.forEach { repository.addWire(it) }
    }
}

data class MoveComponentCommand(
    private val repository: CommandRepository,
    private val componentId: Long,
    private val oldX: Float,
    private val oldY: Float,
    private val oldRotation: Float,
    private val newX: Float,
    private val newY: Float,
    private val newRotation: Float
) : Command {
    override val description = "Mover componente"
    
    override fun execute() {
        repository.updateComponentPosition(componentId, newX, newY, newRotation)
    }
    
    override fun undo() {
        repository.updateComponentPosition(componentId, oldX, oldY, oldRotation)
    }
}

data class RotateComponentCommand(
    private val repository: CommandRepository,
    private val componentId: Long,
    private val oldRotation: Float,
    private val newRotation: Float,
    private val x: Float,
    private val y: Float
) : Command {
    override val description = "Girar pieza"

    override fun execute() {
        repository.updateComponentPosition(componentId, x, y, newRotation)
    }

    override fun undo() {
        repository.updateComponentPosition(componentId, x, y, oldRotation)
    }
}

data class UpdateComponentPropertyCommand(
    private val repository: CommandRepository,
    private val componentId: Long,
    private val propertyName: String,
    private val oldValue: Any,
    private val newValue: Any
) : Command {
    override val description = "Cambiar $propertyName"
    
    override fun execute() {
        repository.updateComponentProperty(componentId, propertyName, newValue)
    }
    
    override fun undo() {
        repository.updateComponentProperty(componentId, propertyName, oldValue)
    }
}

data class UpdateComponentLabelCommand(
    private val repository: CommandRepository,
    private val componentId: Long,
    private val oldLabel: String,
    private val newLabel: String
) : Command {
    override val description = "Renombrar componente"
    
    override fun execute() {
        repository.updateComponentLabel(componentId, newLabel)
    }
    
    override fun undo() {
        repository.updateComponentLabel(componentId, oldLabel)
    }
}

// ===== Comandos de Cables =====

data class AddWireCommand(
    private val repository: CommandRepository,
    private val wire: CircuitWire
) : Command {
    override val description = "Conectar cable"
    
    override fun execute() {
        repository.addWire(wire)
    }
    
    override fun undo() {
        repository.deleteWire(wire.id)
    }
}

data class DeleteWireCommand(
    private val repository: CommandRepository,
    private val wire: CircuitWire
) : Command {
    override val description = "Desconectar cable"
    
    override fun execute() {
        repository.deleteWire(wire.id)
    }
    
    override fun undo() {
        repository.addWire(wire)
    }
}

// ===== Comando compuesto (para agrupar múltiples acciones) =====

data class CompositeCommand(
    private val commands: List<Command>,
    override val description: String
) : Command {
    override fun execute() {
        commands.forEach { it.execute() }
    }
    
    override fun undo() {
        commands.asReversed().forEach { it.undo() }
    }
}

// ===== Repositorio de comandos (interface para desacoplar) =====

interface CommandRepository {
    fun addComponent(component: CircuitComponent)
    fun deleteComponent(componentId: Long)
    fun updateComponentPosition(id: Long, x: Float, y: Float, rotation: Float)
    fun updateComponentProperty(id: Long, propertyName: String, value: Any)
    fun updateComponentLabel(id: Long, label: String)
    fun addWire(wire: CircuitWire)
    fun deleteWire(wireId: Long)
    fun getWiresConnectedToComponent(componentId: Long): List<CircuitWire>
}

// ===== Manager de Historial =====

class HistoryManager(private val maxSize: Int = 50) {
    
    private val _undoStack = MutableStateFlow<List<Command>>(emptyList())
    val undoStack = _undoStack.asStateFlow()
    
    private val _redoStack = MutableStateFlow<List<Command>>(emptyList())
    val redoStack = _redoStack.asStateFlow()
    
    val canUndo: Boolean
        get() = _undoStack.value.isNotEmpty()
    val canRedo: Boolean
        get() = _redoStack.value.isNotEmpty()
    
    fun executeCommand(command: Command) {
        command.execute()
        
        // Añadir a undo stack, limitar tamaño
        _undoStack.value = (_undoStack.value + command).takeLast(maxSize)
        
        // Limpiar redo stack al hacer nueva acción
        _redoStack.value = emptyList()
    }
    
    /** Registra un comando que ya se aplicó (por ejemplo, tras guardar en la base de datos) */
    fun record(command: Command) {
        _undoStack.value = (_undoStack.value + command).takeLast(maxSize)
        _redoStack.value = emptyList()
    }

    fun undo(): Command? {
        val command = _undoStack.value.lastOrNull() ?: return null
        command.undo()
        _undoStack.value = _undoStack.value.dropLast(1)
        _redoStack.value = _redoStack.value + command
        return command
    }
    
    fun redo(): Command? {
        val command = _redoStack.value.lastOrNull() ?: return null
        command.execute()
        _redoStack.value = _redoStack.value.dropLast(1)
        _undoStack.value = _undoStack.value + command
        return command
    }
    
    fun clear() {
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
    }
    
    // Para guardar/restaurar estado (serialización)
    fun getState(): HistoryState = HistoryState(
        undoStack = _undoStack.value.map { it.description },
        redoStack = _redoStack.value.map { it.description }
    )
}

data class HistoryState(
    val undoStack: List<String>,
    val redoStack: List<String>
)