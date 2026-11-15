package com.circuitnexus.engine

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.ComponentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Motor de ejecución de bloques Scratch visuales.
 * Ejecuta bloques estilo Scratch en tiempo real sobre el circuito.
 */
class ScratchEngine {

    enum class BlockType {
        EVENT_START, LOOP_REPEAT, IF_THEN, WAIT, SET_PIN, READ_PIN, MATH_OP
    }

    data class ScratchBlock(
        val id: Long,
        val type: BlockType,
        val params: Map<String, Any> = emptyMap(),
        val children: List<ScratchBlock> = emptyList(),
        val next: Long? = null
    )

    data class ExecutionState(
        val variables: MutableMap<String, Double> = mutableMapOf(),
        val pinStates: MutableMap<Long, Boolean> = mutableMapOf(),
        val currentBlockId: Long? = null,
        val isRunning: Boolean = false
    )

    private val _state = MutableStateFlow(ExecutionState())
    val state = _state.asStateFlow()

    private var currentProgram: List<ScratchBlock> = emptyList()
    private var blockMap: Map<Long, ScratchBlock> = emptyMap()

    fun loadProgram(blocks: List<ScratchBlock>) {
        currentProgram = blocks
        blockMap = blocks.associateBy { it.id }
        _state.value = _state.value.copy(variables = mutableMapOf(), pinStates = mutableMapOf(), isRunning = false)
    }

    fun start() {
        _state.value = _state.value.copy(isRunning = true, currentBlockId = currentProgram.firstOrNull()?.id)
    }

    fun stop() {
        _state.value = _state.value.copy(isRunning = false, currentBlockId = null)
    }

    fun executeStep(components: List<CircuitComponent>, wires: List<com.circuitnexus.model.CircuitWire>) {
        if (!_state.value.isRunning) return

        val currentId = _state.value.currentBlockId ?: return
        val block = blockMap[currentId] ?: return

        when (block.type) {
            BlockType.EVENT_START -> {
                _state.value = _state.value.copy(currentBlockId = block.next)
            }
            BlockType.LOOP_REPEAT -> {
                val count = (block.params["count"] as? Int) ?: 10
                val loopVar = block.params["var"] as? String ?: "i"
                val current = _state.value.variables[loopVar]?.toInt() ?: 0
                if (current < count) {
                    _state.value = _state.value.copy(variables = _state.value.variables + (loopVar to (current + 1).toDouble()))
                    _state.value = _state.value.copy(currentBlockId = block.children.firstOrNull()?.id ?: block.next)
                } else {
                    _state.value = _state.value.copy(currentBlockId = block.next)
                }
            }
            BlockType.IF_THEN -> {
                val condition = (block.params["condition"] as? Boolean) ?: false
                _state.value = _state.value.copy(currentBlockId = if (condition) block.children.firstOrNull()?.id else block.next)
            }
            BlockType.WAIT -> {
                val ms = (block.params["ms"] as? Long) ?: 1000L
                _state.value = _state.value.copy(currentBlockId = block.next)
            }
            BlockType.SET_PIN -> {
                val pinId = (block.params["pinId"] as? Long) ?: return
                val value = (block.params["value"] as? Boolean) ?: false
                _state.value = _state.value.copy(
                    pinStates = _state.value.pinStates + (pinId to value)
                )
                _state.value = _state.value.copy(currentBlockId = block.next)
            }
            BlockType.READ_PIN -> {
                val pinId = (block.params["pinId"] as? Long) ?: return
                val varName = (block.params["variable"] as? String) ?: "pinValue"
                val value = _state.value.pinStates[pinId] ?: false
                _state.value = _state.value.copy(
                    variables = _state.value.variables + (varName to if (value) 1.0 else 0.0),
                    currentBlockId = block.next
                )
            }
            BlockType.MATH_OP -> {
                val op = (block.params["op"] as? String) ?: "+"
                val a = (block.params["a"] as? Double) ?: 0.0
                val b = (block.params["b"] as? Double) ?: 0.0
                val result = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else 0.0
                    else -> 0.0
                }
                val varName = (block.params["result"] as? String) ?: "result"
                _state.value = _state.value.copy(
                    variables = _state.value.variables + (varName to result),
                    currentBlockId = block.next
                )
            }
        }

        if (_state.value.currentBlockId == currentId) {
            _state.value = _state.value.copy(currentBlockId = block.next)
        }
    }

    fun setVariable(name: String, value: Double) {
        _state.value = _state.value.copy(variables = _state.value.variables + (name to value))
    }

    fun getVariable(name: String): Double = _state.value.variables[name] ?: 0.0

    fun setPinState(pinId: Long, value: Boolean) {
        _state.value = _state.value.copy(pinStates = _state.value.pinStates + (pinId to value))
    }

    companion object {
        @Volatile
        private var INSTANCE: ScratchEngine? = null

        fun getInstance(): ScratchEngine =
            INSTANCE ?: synchronized(this) { INSTANCE ?: ScratchEngine().also { INSTANCE = it } }
    }
}