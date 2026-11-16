package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circuitnexus.data.SettingsDataStore
import com.circuitnexus.ui.tutorial.TutorialContent
import com.circuitnexus.ui.tutorial.TutorialStep
import com.circuitnexus.ui.tutorial.TutorialState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TutorialViewModel(private val settings: SettingsDataStore) : ViewModel() {
    
    private val _currentTutorial = MutableStateFlow<TutorialStep?>(null)
    val currentTutorial = _currentTutorial.asStateFlow()
    
    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex = _currentStepIndex.asStateFlow()
    
    private val _tutorialState = MutableStateFlow(TutorialState())
    val tutorialState = _tutorialState.asStateFlow()
    
    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()
    
    private val _showCelebration = MutableStateFlow(false)
    val showCelebration = _showCelebration.asStateFlow()
    
    private val _completedTutorials = MutableStateFlow<Set<String>>(emptySet())
    val completedTutorials = _completedTutorials.asStateFlow()
    
    val availableTutorials = listOf(
        TutorialInfo("first_circuit", "Tu Primer Circuito", "Enciende un LED paso a paso", TutorialContent.FIRST_CIRCUIT_TUTORIAL, "🌱"),
        TutorialInfo("blink_555", "Parpadeo con 555", "Haz parpadear un LED automáticamente", TutorialContent.BLINK_TUTORIAL, "⚡")
    )
    
    init {
        loadProgress()
    }
    
    private fun loadProgress() {
        viewModelScope.launch {
            // TODO: Cargar de DataStore cuando añada la key
            // settings.tutorialProgress...
        }
    }
    
    fun startTutorial(tutorialId: String) {
        val tutorial = availableTutorials.find { it.id == tutorialId } ?: return
        _currentTutorial.value = tutorial.steps[0]
        _currentStepIndex.value = 0
        _tutorialState.value = TutorialState()
        _isActive.value = true
        _showCelebration.value = false
    }
    
    fun nextStep() {
        val tutorial = availableTutorials.find { it.steps.contains(_currentTutorial.value!!) } ?: return
        val currentIndex = _currentStepIndex.value
        if (currentIndex < tutorial.steps.size - 1) {
            _currentStepIndex.value = currentIndex + 1
            _currentTutorial.value = tutorial.steps[currentIndex + 1]
        } else {
            completeTutorial(tutorial.id)
        }
    }
    
    fun skipTutorial() {
        _isActive.value = false
        _currentTutorial.value = null
    }
    
    private fun completeTutorial(tutorialId: String) {
        _completedTutorials.value = _completedTutorials.value + tutorialId
        _showCelebration.value = true
        _isActive.value = false
        // TODO: Guardar en DataStore
    }
    
    fun dismissCelebration() {
        _showCelebration.value = false
        _currentTutorial.value = null
    }
    
    fun updateState(newState: TutorialState) {
        _tutorialState.value = newState
        
        // Auto-avanzar si la validación pasa
        _currentTutorial.value?.let { step ->
            if (step.validation(newState)) {
                // Pequeño delay para que el usuario vea el éxito
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500)
                    nextStep()
                }
            }
        }
    }
    
    fun recordComponentPlaced(type: com.circuitnexus.model.ComponentType) {
        val newState = _tutorialState.value.copy(
            placedComponents = _tutorialState.value.placedComponents + type
        )
        updateState(newState)
    }
    
    fun recordConnection(from: com.circuitnexus.model.ComponentType, to: com.circuitnexus.model.ComponentType) {
        val newState = _tutorialState.value.copy(
            connections = _tutorialState.value.connections + (from to to)
        )
        updateState(newState)
    }
    
    fun recordSimulationRunning(running: Boolean, ledLit: Boolean = false) {
        val newState = _tutorialState.value.copy(
            simulationRunning = running,
            ledIsLit = ledLit
        )
        updateState(newState)
    }
    
    fun isTutorialCompleted(id: String): Boolean = _completedTutorials.value.contains(id)
    
    data class TutorialInfo(
        val id: String,
        val title: String,
        val description: String,
        val steps: List<TutorialStep>,
        val emoji: String
    )
}