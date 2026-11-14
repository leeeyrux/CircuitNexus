package com.circuitnexus.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circuitnexus.command.AddComponentCommand
import com.circuitnexus.command.AddWireCommand
import com.circuitnexus.command.CommandRepository
import com.circuitnexus.command.CompositeCommand
import com.circuitnexus.command.DeleteComponentCommand
import com.circuitnexus.command.DeleteWireCommand
import com.circuitnexus.command.HistoryManager
import com.circuitnexus.command.MoveComponentCommand
import com.circuitnexus.command.RotateComponentCommand
import com.circuitnexus.command.UpdateComponentLabelCommand
import com.circuitnexus.command.UpdateComponentPropertyCommand
import com.circuitnexus.data.CircuitRepository
import com.circuitnexus.data.SettingsDataStore
import com.circuitnexus.di.AppContainer
import com.circuitnexus.engine.CircuitEngine
import com.circuitnexus.engine.DrcEngine
import com.circuitnexus.engine.HelpCard
import com.circuitnexus.engine.OfflineHelpEngine
import com.circuitnexus.engine.transient.TransientEngine
import com.circuitnexus.export.ExportEngine
import com.circuitnexus.model.Circuit
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentProperties
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import com.circuitnexus.model.SimulationWarning
import com.circuitnexus.model.WarningSeverity
import com.circuitnexus.particles.ParticleSystem
import com.circuitnexus.templates.CircuitTemplate
import com.circuitnexus.ui.oscilloscope.ScopeChannel
import com.circuitnexus.ui.tutorial.TutorialStep
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.util.HapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Cerebro de la mesa de trabajo.
 *
 * Mantiene el circuito, corre la simulación en vivo (transitoria)
 * y prepara datos para osciloscopio, multímetro, revisión y exportación.
 */
class WorkbenchViewModel(
    private val repository: CircuitRepository,
    private val settings: SettingsDataStore,
    private val container: AppContainer,
    private val circuitId: Long,
    private val tutorialViewModel: com.circuitnexus.viewmodel.TutorialViewModel? = null,
    private val hapticManager: HapticFeedback.HapticManager? = null
) : ViewModel(), CommandRepository {

    // ===== Circuito =====
    private val _circuit = MutableStateFlow<Circuit?>(null)
    val circuit = _circuit.asStateFlow()

    private val _components = MutableStateFlow<List<CircuitComponent>>(emptyList())
    val components = _components.asStateFlow()

    private val _wires = MutableStateFlow<List<CircuitWire>>(emptyList())
    val wires = _wires.asStateFlow()

    // ===== Simulación en vivo =====
    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState = _simulationState.asStateFlow()

    private val _isRunning = MutableStateFlow(true)
    val isRunning = _isRunning.asStateFlow()

    private val _showOscilloscope = MutableStateFlow(false)
    val showOscilloscope = _showOscilloscope.asStateFlow()

    private val _channels = MutableStateFlow<List<ScopeChannel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _showLibrary = MutableStateFlow(false)
    val showLibrary = _showLibrary.asStateFlow()

    private val _drcReport = MutableStateFlow<DrcEngine.DrcReport?>(null)
    val drcReport = _drcReport.asStateFlow()

    private val _exportPreview = MutableStateFlow<ExportPreview?>(null)
    val exportPreview = _exportPreview.asStateFlow()

    private val _netlistLines = MutableStateFlow<List<String>>(emptyList())
    val netlistLines = _netlistLines.asStateFlow()

    // ===== Guardado automático =====
    private val _saveState = MutableStateFlow(SaveState.IDLE)
    val saveState = _saveState.asStateFlow()

    private var persistJob: Job? = null

    enum class SaveState { IDLE, SAVING, SAVED }

    // ===== Borradores del panel de piezas =====
    private val _draftResistance = MutableStateFlow(330f)
    val draftResistance = _draftResistance.asStateFlow()

    private val _draftLedColor = MutableStateFlow("red")
    val draftLedColor = _draftLedColor.asStateFlow()

    private val _draftBatteryVolts = MutableStateFlow(9f)
    val draftBatteryVolts = _draftBatteryVolts.asStateFlow()

    private val _draftTimerFrequency = MutableStateFlow(1f)
    val draftTimerFrequency = _draftTimerFrequency.asStateFlow()

    private val _draftCapacitance = MutableStateFlow(100f) // µF
    val draftCapacitance = _draftCapacitance.asStateFlow()

    // ===== Tutorial =====
    private val _tutorialStep = MutableStateFlow<TutorialStep?>(null)
    val tutorialStep = _tutorialStep.asStateFlow()

    private val _tutorialActive = MutableStateFlow(false)
    val tutorialActive = _tutorialActive.asStateFlow()

    private val _tutorialProgress = MutableStateFlow(0f)
    val tutorialProgress = _tutorialProgress.asStateFlow()

    // ===== Multímetro =====
    private val _multimeter = MutableStateFlow<MultimeterState?>(null)
    val multimeter = _multimeter.asStateFlow()

    // ===== Partículas =====
    private val particleSystem = ParticleSystem(maxParticles = 120)
    val particles = particleSystem.particlesFlow

    // ===== Historial =====
    private val historyManager = HistoryManager(maxSize = 50)
    val undoStack = historyManager.undoStack
    val redoStack = historyManager.redoStack

    // ===== Ayuda =====
    private val _helpCards = MutableStateFlow<List<HelpCard>>(emptyList())
    val helpCards = _helpCards.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    // ===== UI =====
    private val _selectedComponentId = MutableStateFlow<Long?>(null)
    val selectedComponentId = _selectedComponentId.asStateFlow()

    private val _connectingWireStart = MutableStateFlow<WireConnectionStart?>(null)
    val connectingWireStart = _connectingWireStart.asStateFlow()

    private val _panZoom = MutableStateFlow(PanZoomState(0f, 0f, 1f))
    val panZoom = _panZoom.asStateFlow()

    private val _showGrid = MutableStateFlow(true)
    val showGrid = _showGrid.asStateFlow()

    private val _gridSize = MutableStateFlow(20)
    val gridSize = _gridSize.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid = _snapToGrid.asStateFlow()

    // ===== Motores =====
    private val transientEngine = TransientEngine.getInstance()
    private val offlineHelp = OfflineHelpEngine()
    private var simulationJob: Job? = null
    private var lastFrameNanos = System.nanoTime()
    private var lastDrcMillis = 0L

    init {
        loadCircuit()
        loadSettings()
        startSimulationLoop()
        observeTutorial()
    }

    private fun observeTutorial() {
        val vm = tutorialViewModel ?: return
        viewModelScope.launch {
            vm.currentTutorial.collect { step ->
                _tutorialStep.value = step
                _tutorialActive.value = step != null
                _tutorialProgress.value = if (step == null) 0f else {
                    (vm.currentStepIndex.value + 1).toFloat() / 10f
                }
            }
        }
    }

    fun startTutorial() {
        tutorialViewModel?.startTutorial("first_circuit")
    }

    fun nextTutorialStep() {
        tutorialViewModel?.nextStep()
    }

    fun skipTutorial() {
        tutorialViewModel?.skipTutorial()
        _tutorialStep.value = null
        _tutorialActive.value = false
    }

    // ===== Carga =====

    private fun loadCircuit() {
        viewModelScope.launch {
            _circuit.value = repository.getCircuit(circuitId)
            _components.value = repository.getComponents(circuitId).first()
            _wires.value = repository.getWires(circuitId).first()
            rebuildNetlist()
            runQuickReview()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settings.showGrid.collect { _showGrid.value = it }
        }
        viewModelScope.launch {
            settings.snapToGrid.collect { _snapToGrid.value = it }
        }
        viewModelScope.launch {
            settings.gridSize.collect { _gridSize.value = it }
        }
    }

    // ===== Bucle de simulación (≈60 fps) =====

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var tick = 0
            var frame = 0
            while (true) {
                val now = System.nanoTime()
                val dtSeconds = ((now - lastFrameNanos) / 1_000_000_000.0).coerceAtMost(0.05)
                lastFrameNanos = now

                if (_isRunning.value) {
                    val comps = _components.value
                    val wrs = _wires.value
                    if (comps.isNotEmpty()) {
                        try {
                            // Reconstruye la matriz solo si cambió el circuito
                            transientEngine.updateTopology(comps, wrs)
                            // Pasos baratos sin calcular estado ni historial
                            repeat(6) {
                                transientEngine.step(comps, wrs, computeState = false, record = false)
                            }
                            // Un paso final completo para la UI y el osciloscopio
                            val result = transientEngine.step(comps, wrs, computeState = true, record = true)
                            frame++
                            // Publicamos a ~30 fps: suficiente para verse fluido y ahorra batería
                            if (result.success && frame % 2 == 0) {
                                _simulationState.value = SimulationState(
                                    isRunning = true,
                                    nodeVoltages = result.nodeVoltages,
                                    componentCurrents = result.componentCurrents,
                                    wireCurrents = result.wireCurrents,
                                    warnings = _simulationState.value.warnings,
                                    lastUpdate = System.currentTimeMillis()
                                )
                                updateChannels()
                            }
                        } catch (t: Throwable) {
                            // Un circuito raro nunca debe tumbar la app:
                            // pausamos la simulación y avisamos con lenguaje simple.
                            _isRunning.value = false
                            _drcReport.value = DrcEngine.DrcReport(
                                issues = listOf(
                                    DrcEngine.DrcIssue(
                                        severity = WarningSeverity.INFO,
                                        title = "Cálculo detenido",
                                        message = "La simulación se detuvo porque el circuito es muy complejo o tiene una conexión rara.",
                                        suggestion = "Revisa las conexiones y vuelve a darle a Reproducir."
                                    )
                                ),
                                score = 50,
                                summary = "Simulación en pausa por seguridad."
                            )
                        }
                    }
                }

                particleSystem.update(_components.value, _wires.value, _simulationState.value, dtSeconds.toFloat())

                // Revisión periódica (~2 veces por segundo)
                tick++
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastDrcMillis > 700) {
                    lastDrcMillis = nowMs
                    runQuickReview()
                }

                delay(16)
            }
        }
    }

    private fun updateChannels() {
        val comps = _components.value
        if (comps.isEmpty()) {
            _channels.value = emptyList()
            return
        }
        val palette = listOf(
            CircuitColors.Emerald,
            CircuitColors.Cyan,
            CircuitColors.Amber,
            CircuitColors.Coral
        )
        // Señales: hasta 4 nodos con más actividad
        val voltages = _simulationState.value.nodeVoltages
        val nodes = voltages.keys.sorted().take(4)
        val labels = listOf("Señal A", "Señal B", "Señal C", "Señal D")
        _channels.value = nodes.mapIndexed { index, node ->
            ScopeChannel(
                id = "node-$node",
                label = labels.getOrElse(index) { "Señal ${index + 1}" },
                color = palette[index % palette.size],
                samples = transientEngine.voltageHistory(node)
            )
        }
    }

    // ===== Revisión + ayuda =====

    private fun runQuickReview() {
        val report = DrcEngine.review(_components.value, _wires.value, _simulationState.value)
        _drcReport.value = report

        val warnings = report.issues.map {
            SimulationWarning(
                severity = it.severity,
                message = it.message,
                componentId = it.componentId,
                wireId = it.wireId,
                suggestion = it.suggestion
            )
        }
        _simulationState.value = _simulationState.value.copy(warnings = warnings)
        _helpCards.value = offlineHelp.analyzeCircuit(_components.value, _wires.value, warnings)

        val ledLit = _components.value.any {
            it.type == ComponentType.LED && (_simulationState.value.componentCurrents[it.id] ?: 0f) > 0.001f
        }
        tutorialViewModel?.recordSimulationRunning(true, ledLit)
    }

    fun reviewCircuit() {
        runQuickReview()
        hapticManager?.mediumClick()
    }

    // ===== Netlist simple =====

    private fun rebuildNetlist() {
        val comps = _components.value
        val lines = mutableListOf<String>()
        for (wire in _wires.value) {
            val from = comps.find { it.id == wire.fromComponentId } ?: continue
            val to = comps.find { it.id == wire.toComponentId } ?: continue
            val fromPin = CircuitWire.pinName(from.type, wire.fromPinIndex)
            val toPin = CircuitWire.pinName(to.type, wire.toPinIndex)
            lines.add("${from.type.displayName} · $fromPin  →  ${to.type.displayName} · $toPin")
        }
        if (lines.isEmpty() && comps.isNotEmpty()) {
            lines.add("Aún no hay cables. Toca un punto de una pieza y luego otro para unirlos.")
        }
        _netlistLines.value = lines
    }

    // ===== Comandos del repositorio =====

    override fun addComponent(component: CircuitComponent) {
        _components.value = _components.value + component
        onTopologyChanged()
    }

    override fun deleteComponent(componentId: Long) {
        _components.value = _components.value.filter { it.id != componentId }
        _wires.value = _wires.value.filter {
            it.fromComponentId != componentId && it.toComponentId != componentId
        }
        if (_selectedComponentId.value == componentId) _selectedComponentId.value = null
        onTopologyChanged()
    }

    override fun updateComponentPosition(id: Long, x: Float, y: Float, rotation: Float) {
        _components.value = _components.value.map {
            if (it.id == id) it.copy(position = it.position.copy(x = x, y = y, rotation = rotation)) else it
        }
        onTopologyChanged()
    }

    override fun updateComponentProperty(id: Long, propertyName: String, value: Any) {
        _components.value = _components.value.map { comp ->
            if (comp.id != id) comp
            else {
                val p = comp.properties
                val updated = when (propertyName) {
                    "resistanceOhms" -> p.copy(resistanceOhms = value as Float)
                    "capacitanceFarads" -> p.copy(capacitanceFarads = value as Float)
                    "inductanceHenries" -> p.copy(inductanceHenries = value as Float)
                    "potPosition" -> p.copy(potPosition = value as Float)
                    "voltageVolts" -> p.copy(voltageVolts = value as Float)
                    "currentAmps" -> p.copy(currentAmps = value as Float)
                    "forwardVoltage" -> p.copy(forwardVoltage = value as Float)
                    "ledForwardVoltage" -> p.copy(ledForwardVoltage = value as Float)
                    "ledColor" -> p.copy(ledColor = value as String)
                    "currentGain" -> p.copy(currentGain = value as Float)
                    "thresholdVoltage" -> p.copy(thresholdVoltage = value as Float)
                    "breakdownVoltage" -> p.copy(breakdownVoltage = value as Float)
                    "isNormallyOpen" -> p.copy(isNormallyOpen = value as Boolean)
                    "timerMode" -> p.copy(timerMode = value as String)
                    "frequencyHz" -> p.copy(frequencyHz = value as Float)
                    "dutyCycle" -> p.copy(dutyCycle = value as Float)
                    "regulationVolts" -> p.copy(regulationVolts = value as Float)
                    "gain" -> p.copy(gain = value as Float)
                    "logicFunction" -> p.copy(logicFunction = value as String)
                    "text" -> p.copy(text = value as String)
                    "brightness" -> p.copy(brightness = value as Float)
                    "pwmDuty" -> p.copy(pwmDuty = value as Float)
                    "sensorValue" -> p.copy(sensorValue = value as Float)
                    else -> p
                }
                comp.copy(properties = updated)
            }
        }
        onTopologyChanged()
    }

    override fun updateComponentLabel(id: Long, label: String) {
        _components.value = _components.value.map {
            if (it.id == id) it.copy(label = label) else it
        }
        onTopologyChanged()
    }

    override fun addWire(wire: CircuitWire) {
        _wires.value = _wires.value + wire
        onTopologyChanged()
    }

    override fun deleteWire(wireId: Long) {
        _wires.value = _wires.value.filter { it.id != wireId }
        onTopologyChanged()
    }

    override fun getWiresConnectedToComponent(componentId: Long): List<CircuitWire> =
        _wires.value.filter {
            it.fromComponentId == componentId || it.toComponentId == componentId
        }

    private fun onTopologyChanged() {
        rebuildNetlist()
        schedulePersist()
        // La simulación detecta el cambio sola (hash de topología).
        viewModelScope.launch {
            delay(120)
            runQuickReview()
        }
    }

    /** Guardado con pequeño retardo para no escribir en disco en cada gesto. */
    private fun schedulePersist() {
        persistJob?.cancel()
        _saveState.value = SaveState.SAVING
        persistJob = viewModelScope.launch {
            delay(600)
            val comps = _components.value
            val wrs = _wires.value
            repository.replaceCircuitContents(circuitId, comps, wrs)
            _circuit.value?.let {
                repository.saveCircuit(it.copy(updatedAt = System.currentTimeMillis()))
            }
            _saveState.value = SaveState.SAVED
        }
    }

    /** Fuerza el guardado inmediato (al salir de la pantalla). */
    fun flushSave() {
        persistJob?.cancel()
        viewModelScope.launch {
            repository.replaceCircuitContents(circuitId, _components.value, _wires.value)
            _circuit.value?.let {
                repository.saveCircuit(it.copy(updatedAt = System.currentTimeMillis()))
            }
            _saveState.value = SaveState.SAVED
        }
    }

    /** Gira la pieza 90° (útil para ordenar el circuito). */
    fun rotateComponent(componentId: Long) {
        val comp = _components.value.find { it.id == componentId } ?: return
        val oldRotation = comp.position.rotation
        val newRotation = (oldRotation + 90f) % 360f
        historyManager.executeCommand(
            RotateComponentCommand(
                this, componentId, oldRotation, newRotation,
                comp.position.x, comp.position.y
            )
        )
        hapticManager?.mediumClick()
    }

    // ===== Acciones de usuario =====

    fun addComponent(type: ComponentType, x: Float, y: Float) {
        val snapped = snapToGrid(x, y)
        val base = CircuitComponent.createDefault(type, circuitId, snapped.first, snapped.second)
        val component = base.copy(properties = applyDrafts(base.properties, type))
        _showLibrary.value = false
        _components.value = _components.value + component

        viewModelScope.launch {
            val id = repository.addComponent(component)
            val real = component.copy(id = id)
            _components.value = _components.value.map { if (it === component) real else it }
            historyManager.record(AddComponentCommand(this@WorkbenchViewModel, real))
            onTopologyChanged()
        }
        tutorialViewModel?.recordComponentPlaced(type)
        hapticManager?.mediumClick()
    }

    fun moveComponent(componentId: Long, newX: Float, newY: Float) {
        val comp = _components.value.find { it.id == componentId } ?: return
        val snapped = snapToGrid(newX, newY)
        if (snapped.first == comp.position.x && snapped.second == comp.position.y) return
        historyManager.executeCommand(
            MoveComponentCommand(
                this, componentId,
                comp.position.x, comp.position.y, comp.position.rotation,
                snapped.first, snapped.second, comp.position.rotation
            )
        )
        viewModelScope.launch {
            _components.value.find { it.id == componentId }?.let { repository.updateComponent(it) }
        }
    }

    fun deleteComponent(componentId: Long) {
        val comp = _components.value.find { it.id == componentId } ?: return
        historyManager.executeCommand(
            DeleteComponentCommand(this, comp, getWiresConnectedToComponent(componentId))
        )
        viewModelScope.launch { repository.deleteComponent(componentId) }
        hapticManager?.lightClick()
    }

    fun setProperty(componentId: Long, name: String, value: Any) {
        val comp = _components.value.find { it.id == componentId } ?: return
        val oldValue = when (name) {
            "resistanceOhms" -> comp.properties.resistanceOhms
            "capacitanceFarads" -> comp.properties.capacitanceFarads
            "inductanceHenries" -> comp.properties.inductanceHenries
            "potPosition" -> comp.properties.potPosition
            "voltageVolts" -> comp.properties.voltageVolts
            "currentAmps" -> comp.properties.currentAmps
            "forwardVoltage" -> comp.properties.forwardVoltage
            "ledForwardVoltage" -> comp.properties.ledForwardVoltage
            "ledColor" -> comp.properties.ledColor
            "currentGain" -> comp.properties.currentGain
            "thresholdVoltage" -> comp.properties.thresholdVoltage
            "breakdownVoltage" -> comp.properties.breakdownVoltage
            "isNormallyOpen" -> comp.properties.isNormallyOpen
            "timerMode" -> comp.properties.timerMode
            "frequencyHz" -> comp.properties.frequencyHz
            "dutyCycle" -> comp.properties.dutyCycle
            "regulationVolts" -> comp.properties.regulationVolts
            "gain" -> comp.properties.gain
            "logicFunction" -> comp.properties.logicFunction
            "text" -> comp.properties.text
            "brightness" -> comp.properties.brightness
            "pwmDuty" -> comp.properties.pwmDuty
            "sensorValue" -> comp.properties.sensorValue
            else -> null
        }
        if (oldValue == value) return
        historyManager.executeCommand(
            UpdateComponentPropertyCommand(this, componentId, name, oldValue ?: "", value)
        )
        viewModelScope.launch {
            _components.value.find { it.id == componentId }?.let { repository.updateComponent(it) }
        }
    }

    fun setLabel(componentId: Long, label: String) {
        val comp = _components.value.find { it.id == componentId } ?: return
        if (comp.label == label) return
        historyManager.executeCommand(
            UpdateComponentLabelCommand(this, componentId, comp.label, label)
        )
        viewModelScope.launch {
            _components.value.find { it.id == componentId }?.let { repository.updateComponent(it) }
        }
    }

    // ===== Panel de piezas =====

    fun setDraftResistance(ohms: Float) {
        _draftResistance.value = ohms.coerceIn(1f, 1_000_000f)
    }

    fun setDraftLedColor(color: String) {
        _draftLedColor.value = color
        hapticManager?.lightClick()
    }

    fun setDraftBatteryVolts(volts: Float) {
        _draftBatteryVolts.value = volts.coerceIn(1.5f, 24f)
    }

    fun setDraftTimerFrequency(hz: Float) {
        _draftTimerFrequency.value = hz.coerceIn(0.1f, 100f)
    }

    fun setDraftCapacitance(microFarads: Float) {
        _draftCapacitance.value = microFarads.coerceIn(0.1f, 10_000f)
    }

    private fun applyDrafts(base: ComponentProperties, type: ComponentType): ComponentProperties =
        when (type) {
            ComponentType.RESISTOR -> base.copy(resistanceOhms = _draftResistance.value)
            ComponentType.LED -> base.copy(ledColor = _draftLedColor.value)
            ComponentType.BATTERY -> base.copy(voltageVolts = _draftBatteryVolts.value)
            ComponentType.TIMER_555 -> base.copy(frequencyHz = _draftTimerFrequency.value)
            ComponentType.CAPACITOR -> base.copy(capacitanceFarads = _draftCapacitance.value / 1e6f)
            else -> base
        }

    // ===== Cables =====

    data class WireConnectionStart(
        val componentId: Long,
        val pinIndex: Int,
        val startX: Float,
        val startY: Float
    )

    fun startWireConnection(componentId: Long, pinIndex: Int, x: Float, y: Float) {
        _connectingWireStart.value = WireConnectionStart(componentId, pinIndex, x, y)
        hapticManager?.dragStart()
    }

    fun completeWireConnection(targetComponentId: Long, targetPinIndex: Int) {
        val start = _connectingWireStart.value ?: return
        _connectingWireStart.value = null
        if (start.componentId == targetComponentId && start.pinIndex == targetPinIndex) return

        val exists = _wires.value.any { wire ->
            (wire.fromComponentId == start.componentId && wire.fromPinIndex == start.pinIndex &&
                wire.toComponentId == targetComponentId && wire.toPinIndex == targetPinIndex) ||
                (wire.fromComponentId == targetComponentId && wire.fromPinIndex == targetPinIndex &&
                    wire.toComponentId == start.componentId && wire.toPinIndex == start.pinIndex)
        }
        if (exists) return

        val wire = CircuitWire(
            circuitId = circuitId,
            fromComponentId = start.componentId,
            fromPinIndex = start.pinIndex,
            toComponentId = targetComponentId,
            toPinIndex = targetPinIndex
        )
        _wires.value = _wires.value + wire
        viewModelScope.launch {
            val id = repository.addWire(wire)
            val real = wire.copy(id = id)
            _wires.value = _wires.value.map { if (it === wire) real else it }
            historyManager.record(AddWireCommand(this@WorkbenchViewModel, real))
            onTopologyChanged()
        }
        val from = _components.value.find { it.id == start.componentId }
        val to = _components.value.find { it.id == targetComponentId }
        if (from != null && to != null) tutorialViewModel?.recordConnection(from.type, to.type)
        hapticManager?.mediumClick()
    }

    fun cancelWireConnection() {
        _connectingWireStart.value = null
    }

    fun deleteWire(wireId: Long) {
        val wire = _wires.value.find { it.id == wireId } ?: return
        historyManager.executeCommand(DeleteWireCommand(this, wire))
        viewModelScope.launch { repository.deleteWire(wireId) }
        hapticManager?.lightClick()
    }

    // ===== Undo / Redo =====

    fun undo() {
        historyManager.undo()
        schedulePersist()
        hapticManager?.lightClick()
    }

    fun redo() {
        historyManager.redo()
        schedulePersist()
        hapticManager?.lightClick()
    }

    // ===== Selección y vista =====

    fun selectComponent(componentId: Long?) {
        _selectedComponentId.value = componentId
    }

    data class PanZoomState(val panX: Float, val panY: Float, val zoom: Float)

    fun updatePanZoom(panX: Float, panY: Float, zoom: Float) {
        _panZoom.value = PanZoomState(panX, panY, zoom.coerceIn(0.25f, 4f))
    }

    fun resetPanZoom() {
        _panZoom.value = PanZoomState(0f, 0f, 1f)
    }

    // ===== Multímetro =====

    data class MultimeterState(
        val wireId: Long,
        val label: String,
        val voltage: Float,
        val current: Float,
        val history: List<Float>
    )

    fun showMultimeter(wire: CircuitWire) {
        val state = _simulationState.value
        val current = state.wireCurrents[wire.id] ?: 0f
        val voltages = state.nodeVoltages.values
        val voltage = voltages.maxOrNull() ?: 0f
        _multimeter.value = MultimeterState(
            wireId = wire.id,
            label = "Cable ${wire.id}",
            voltage = voltage,
            current = current,
            history = transientEngine.wireCurrentHistory(wire.id).toList().map { it * 1000f }
        )
        hapticManager?.lightClick()
    }

    fun closeMultimeter() {
        _multimeter.value = null
    }

    fun toggleOscilloscope() {
        _showOscilloscope.value = !_showOscilloscope.value
        if (_showOscilloscope.value) updateChannels()
        hapticManager?.mediumClick()
    }

    fun toggleRunning() {
        _isRunning.value = !_isRunning.value
    }

    fun toggleLibrary() {
        _showLibrary.value = !_showLibrary.value
    }

    // ===== Plantillas =====

    fun loadTemplate(template: CircuitTemplate) {
        val commands = mutableListOf<com.circuitnexus.command.Command>()
        val idMap = HashMap<String, Long>()
        var tempId = -1L
        for (tc in template.components) {
            val id = tempId--
            idMap[tc.key] = id
            commands.add(
                AddComponentCommand(
                    this,
                    CircuitComponent(
                        id = id,
                        circuitId = circuitId,
                        type = tc.type,
                        position = tc.position,
                        properties = tc.properties,
                        label = tc.customLabel ?: tc.type.displayName
                    )
                )
            )
        }
        for (tw in template.wires) {
            commands.add(
                AddWireCommand(
                    this,
                    CircuitWire(
                        id = tw.fromPin.toLong() * 100000 + (idMap[tw.toKey] ?: 0),
                        circuitId = circuitId,
                        fromComponentId = idMap[tw.fromKey] ?: 0L,
                        fromPinIndex = tw.fromPin,
                        toComponentId = idMap[tw.toKey] ?: 0L,
                        toPinIndex = tw.toPin
                    )
                )
            )
        }
        historyManager.executeCommand(CompositeCommand(commands, "Cargar ${template.name}"))

        viewModelScope.launch {
            repository.deleteAllWires(circuitId)
            repository.deleteAllComponents(circuitId)
            val newComponents = template.buildComponents(circuitId)
            val newIds = repository.addComponents(newComponents)
            val realMap = HashMap<String, Long>()
            template.components.forEachIndexed { index, tc ->
                realMap[tc.key] = newIds.getOrElse(index) { 0L }
            }
            repository.addWires(template.buildWires(circuitId, realMap))
            _components.value = repository.getComponents(circuitId).first()
            _wires.value = repository.getWires(circuitId).first()
            transientEngine.reset()
            rebuildNetlist()
            runQuickReview()
        }
        hapticManager?.success()
    }

    // ===== Exportación =====

    data class ExportPreview(val title: String, val content: String, val format: String)

    fun exportJson() {
        val circuit = _circuit.value ?: return
        _exportPreview.value = ExportPreview(
            "Circuito (JSON)",
            ExportEngine.toJson(circuit, _components.value, _wires.value),
            "json"
        )
    }

    fun exportSpice() {
        val circuit = _circuit.value ?: return
        _exportPreview.value = ExportPreview(
            "Lista para simuladores (SPICE)",
            ExportEngine.toSpice(circuit, _components.value, _wires.value),
            "cir"
        )
    }

    fun exportKiCad() {
        val circuit = _circuit.value ?: return
        _exportPreview.value = ExportPreview(
            "Esquemático KiCad",
            ExportEngine.toKiCad(circuit, _components.value, _wires.value),
            "kicad_sch"
        )
    }

    fun exportSvg() {
        _exportPreview.value = ExportPreview(
            "Dibujo del circuito (SVG)",
            ExportEngine.toSvg(_components.value, _wires.value),
            "svg"
        )
    }

    fun exportCsv() {
        _exportPreview.value = ExportPreview(
            "Medidas (CSV)",
            ExportEngine.toCsv(_components.value, _simulationState.value),
            "csv"
        )
    }

    fun exportMarkdown() {
        val circuit = _circuit.value ?: return
        _exportPreview.value = ExportPreview(
            "Receta del circuito (Markdown)",
            ExportEngine.toMarkdown(circuit, _components.value, _wires.value, _simulationState.value),
            "md"
        )
    }

    fun closeExport() {
        _exportPreview.value = null
    }

    // ===== Guardado y nombre =====

    fun saveCircuit() {
        val circuit = _circuit.value ?: return
        viewModelScope.launch {
            repository.saveCircuit(circuit.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateCircuitName(name: String) {
        val circuit = _circuit.value ?: return
        val updated = circuit.copy(name = name, updatedAt = System.currentTimeMillis())
        _circuit.value = updated
        viewModelScope.launch { repository.saveCircuit(updated) }
    }

    // ===== Ayuda IA =====

    fun askAi() {
        val engine = container.aiHelpEngine
        if (engine == null) {
            _aiResponse.value = "Para usar el Asistente Inteligente, primero pega tu clave en Ajustes → " +
                "Asistente Inteligente (Opcional). Es gratis con Groq."
            return
        }
        _aiLoading.value = true
        _aiResponse.value = null
        viewModelScope.launch {
            val result = engine.diagnoseCircuit(
                _components.value,
                _wires.value,
                _simulationState.value
            )
            _aiLoading.value = false
            when (result) {
                is com.circuitnexus.engine.AiHelpEngine.Result.Success ->
                    _aiResponse.value = result.value
                is com.circuitnexus.engine.AiHelpEngine.Result.Failure ->
                    _aiResponse.value = "No se pudo conectar: ${result.exception.message}"
            }
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    // ===== Utilidades =====

    fun snapToGrid(x: Float, y: Float): Pair<Float, Float> {
        if (!_snapToGrid.value) return x to y
        val size = _gridSize.value.toFloat().coerceAtLeast(1f)
        return (Math.round(x / size) * size) to (Math.round(y / size) * size)
    }

    fun pinPosition(component: CircuitComponent, pinIndex: Int): Pair<Float, Float> {
        val count = component.type.pinCount
        val angleStep = if (count > 1) 360f / count else 0f
        val radius = 40f
        val angle = Math.toRadians((pinIndex * angleStep - 90).toDouble())
        return (component.position.x + radius * kotlin.math.cos(angle).toFloat()) to
            (component.position.y + radius * kotlin.math.sin(angle).toFloat())
    }

    override fun onCleared() {
        simulationJob?.cancel()
        persistJob?.cancel()
        // Guardado final best-effort al cerrar la pantalla
        container.coroutineScope.launch {
            repository.replaceCircuitContents(circuitId, _components.value, _wires.value)
        }
        super.onCleared()
    }
}
