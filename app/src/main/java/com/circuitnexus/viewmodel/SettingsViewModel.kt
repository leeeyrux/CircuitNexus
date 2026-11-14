package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circuitnexus.data.SettingsDataStore
import com.circuitnexus.di.AppContainer
import com.circuitnexus.engine.AiHelpEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsDataStore,
    private val container: AppContainer
) : ViewModel() {

    // API Key state
    private val _apiKey = MutableStateFlow<String>("")
    val apiKey = _apiKey.asStateFlow()

    private val _apiKeyValid = MutableStateFlow<Boolean?>(null)
    val apiKeyValid = _apiKeyValid.asStateFlow()

    private val _selectedProvider = MutableStateFlow<String>("groq")
    val selectedProvider = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow<String>(AiHelpEngine.getDefaultModel("groq"))
    val selectedModel = _selectedModel.asStateFlow()

    private val _testingConnection = MutableStateFlow(false)
    val testingConnection = _testingConnection.asStateFlow()

    // UI Settings
    private val _showGrid = MutableStateFlow(true)
    val showGrid = _showGrid.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid = _snapToGrid.asStateFlow()

    private val _gridSize = MutableStateFlow(20)
    val gridSize = _gridSize.asStateFlow()

    private val _autoSimulate = MutableStateFlow(true)
    val autoSimulate = _autoSimulate.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(true)
    val hapticFeedback = _hapticFeedback.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settings.aiApiKey.distinctUntilChanged().collect { key ->
                _apiKey.value = key ?: ""
                _apiKeyValid.value = key?.isNotBlank()
            }
        }
        viewModelScope.launch {
            settings.aiProvider.distinctUntilChanged().collect { provider ->
                _selectedProvider.value = provider
                _selectedModel.value = AiHelpEngine.getDefaultModel(provider)
            }
        }
        viewModelScope.launch {
            settings.aiModel.distinctUntilChanged().collect { _selectedModel.value = it }
        }
        viewModelScope.launch {
            settings.showGrid.distinctUntilChanged().collect { _showGrid.value = it }
        }
        viewModelScope.launch {
            settings.snapToGrid.distinctUntilChanged().collect { _snapToGrid.value = it }
        }
        viewModelScope.launch {
            settings.gridSize.distinctUntilChanged().collect { _gridSize.value = it }
        }
        viewModelScope.launch {
            settings.autoSimulate.distinctUntilChanged().collect { _autoSimulate.value = it }
        }
        viewModelScope.launch {
            settings.hapticFeedback.distinctUntilChanged().collect { _hapticFeedback.value = it }
        }
    }

    fun onApiKeyChange(key: String) {
        _apiKey.value = key
        _apiKeyValid.value = null // Reset validation
    }

    fun saveApiKey() {
        viewModelScope.launch {
            settings.setAiApiKey(_apiKey.value.ifBlank { null })
            testConnection()
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            settings.setAiApiKey(null)
            _apiKey.value = ""
            _apiKeyValid.value = false
            container.clearAiEngine()
        }
    }

    fun onProviderChange(provider: String) {
        _selectedProvider.value = provider
        _selectedModel.value = AiHelpEngine.getDefaultModel(provider)
        viewModelScope.launch {
            settings.setAiProvider(provider)
            settings.setAiModel(_selectedModel.value)
            testConnection()
        }
    }

    fun onModelChange(model: String) {
        _selectedModel.value = model
        viewModelScope.launch {
            settings.setAiModel(model)
            testConnection()
        }
    }

    fun testConnection() {
        val key = _apiKey.value
        if (key.isBlank()) {
            _apiKeyValid.value = false
            return
        }

        _testingConnection.value = true
        viewModelScope.launch {
            try {
                val engine = AiHelpEngine(key, _selectedProvider.value, _selectedModel.value)
                val result = engine.explainCircuit(emptyList(), emptyList(), null)
                result.onSuccess {
                    _apiKeyValid.value = true
                    container.initializeAiEngine(key, _selectedProvider.value, _selectedModel.value)
                }.onFailure {
                    _apiKeyValid.value = false
                }
            } catch (e: Exception) {
                _apiKeyValid.value = false
            } finally {
                _testingConnection.value = false
            }
        }
    }

    fun onShowGridChange(show: Boolean) {
        _showGrid.value = show
        viewModelScope.launch { settings.setShowGrid(show) }
    }

    fun onSnapToGridChange(snap: Boolean) {
        _snapToGrid.value = snap
        viewModelScope.launch { settings.setSnapToGrid(snap) }
    }

    fun onGridSizeChange(size: Int) {
        _gridSize.value = size.coerceIn(10, 100)
        viewModelScope.launch { settings.setGridSize(_gridSize.value) }
    }

    fun onAutoSimulateChange(auto: Boolean) {
        _autoSimulate.value = auto
        viewModelScope.launch { settings.setAutoSimulate(auto) }
    }

    fun onHapticFeedbackChange(enabled: Boolean) {
        _hapticFeedback.value = enabled
        viewModelScope.launch { settings.setHapticFeedback(enabled) }
    }

    // Modelos disponibles por proveedor
    val availableModels: Map<String, List<String>> = mapOf(
        "groq" to AiHelpEngine.GROQ_MODELS,
        "openrouter" to AiHelpEngine.OPENROUTER_MODELS,
        "nvidia" to AiHelpEngine.NVIDIA_MODELS
    )

    val providerDisplayNames = mapOf(
        "groq" to "Groq (Rápido, Gratis)",
        "openrouter" to "OpenRouter (Muchos modelos)",
        "nvidia" to "NVIDIA (Nemotron, Gratis)"
    )
}