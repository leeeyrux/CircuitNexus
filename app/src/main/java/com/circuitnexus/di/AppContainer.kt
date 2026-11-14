package com.circuitnexus.di

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.room.Room
import com.circuitnexus.CircuitNexusApplication
import com.circuitnexus.data.CircuitDatabase
import com.circuitnexus.data.CircuitRepository
import com.circuitnexus.data.SettingsDataStore
import com.circuitnexus.engine.CircuitEngine
import com.circuitnexus.engine.OfflineHelpEngine
import com.circuitnexus.engine.AiHelpEngine
import com.circuitnexus.templates.CircuitTemplates
import com.circuitnexus.util.HapticFeedback
import com.circuitnexus.viewmodel.TutorialViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(private val application: Application) {

    // Coroutine scope para toda la app
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Base de datos
    val database: CircuitDatabase = Room.databaseBuilder(
        application,
        CircuitDatabase::class.java,
        "circuitnexus_db"
    ).fallbackToDestructiveMigration().build()

    // Repositorio
    val repository = CircuitRepository(database)

    // Preferencias
    val settingsDataStore = SettingsDataStore(application)

    // Motor de simulación
    val circuitEngine = CircuitEngine()

    // Ayuda offline (reglas rápidas)
    val offlineHelpEngine = OfflineHelpEngine()

    // Tutorial
    val tutorialViewModel = TutorialViewModel(settingsDataStore)

    // Haptics
    val hapticManager = HapticFeedback.HapticManager(application)

    // Ayuda IA (opcional, se inicializa cuando hay API key)
    var aiHelpEngine: AiHelpEngine? = null

    // Observer de ciclo de vida
    val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            coroutineScope.cancel()
        }
    }

    /**
     * Primera vez que se abre la app: crea circuitos de ejemplo
     * para que el usuario no vea una pantalla vacía.
     */
    fun seedExamplesIfNeeded() {
        coroutineScope.launch {
            val isFirstRun = settingsDataStore.isFirstRun.first()
            if (!isFirstRun) return@launch

            val examples = listOf(
                "basic_flashlight" to "Mi primera linterna",
                "blink_555_astable" to "LED que parpadea",
                "basic_voltage_divider" to "Reparto de energía"
            )

            for ((templateId, name) in examples) {
                val template = CircuitTemplates.getById(templateId) ?: continue
                val circuitId = repository.createCircuit(name)
                val newIds = repository.addComponents(template.buildComponents(circuitId))
                val idByKey = HashMap<String, Long>()
                template.components.forEachIndexed { index, tc ->
                    idByKey[tc.key] = newIds.getOrElse(index) { 0L }
                }
                repository.addWires(template.buildWires(circuitId, idByKey))
            }

            settingsDataStore.setFirstRunComplete()
        }
    }

    fun initializeAiEngine(apiKey: String, provider: String, model: String) {
        aiHelpEngine = AiHelpEngine(apiKey, provider, model)
    }

    fun clearAiEngine() {
        aiHelpEngine = null
    }
}