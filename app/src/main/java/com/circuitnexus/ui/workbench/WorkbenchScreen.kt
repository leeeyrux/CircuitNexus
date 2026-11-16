package com.circuitnexus.ui.workbench

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.engine.HelpCard
import com.circuitnexus.engine.HelpType
import com.circuitnexus.engine.ScratchEngine
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.WarningSeverity
import com.circuitnexus.particles.ParticleLayer
import com.circuitnexus.templates.CircuitTemplate
import com.circuitnexus.templates.CircuitTemplates
import com.circuitnexus.ui.library.LibraryScreen
import com.circuitnexus.ui.multimeter.ProMultimeter
import com.circuitnexus.ui.workbench.ScratchPanel
import com.circuitnexus.ui.oscilloscope.OscilloscopeScreen
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.util.HapticFeedback
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.viewmodel.WorkbenchViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Mesa de trabajo 2D: lienzo con piezas, cables, energía animada
 * y todos los instrumentos (multímetro, osciloscopio, revisión, biblioteca, exportar).
 */
@Composable
fun WorkbenchScreen(
    viewModel: WorkbenchViewModel,
    onNavigateUp: () -> Unit
) {
    val components by viewModel.components.collectAsStateWithLifecycle()
    val wires by viewModel.wires.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationState.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val particles by viewModel.particles.collectAsStateWithLifecycle()
    val multimeter by viewModel.multimeter.collectAsStateWithLifecycle()
    val drc by viewModel.drcReport.collectAsStateWithLifecycle()
    val exportPreview by viewModel.exportPreview.collectAsStateWithLifecycle()
    val netlist by viewModel.netlistLines.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val helpCards by viewModel.helpCards.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedComponentId.collectAsStateWithLifecycle()
    val connecting by viewModel.connectingWireStart.collectAsStateWithLifecycle()
    val panZoom by viewModel.panZoom.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val gridSize by viewModel.gridSize.collectAsStateWithLifecycle()
    val circuit by viewModel.circuit.collectAsStateWithLifecycle()
    val undo by viewModel.undoStack.collectAsStateWithLifecycle()
    val redo by viewModel.redoStack.collectAsStateWithLifecycle()
    val showScope by viewModel.showOscilloscope.collectAsStateWithLifecycle()
    val showLibrary by viewModel.showLibrary.collectAsStateWithLifecycle()
    val showScratch by viewModel.showScratch.collectAsStateWithLifecycle()
    val scratchEngine by viewModel.scratchEngine.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val tutorialStep by viewModel.tutorialStep.collectAsStateWithLifecycle()
    val tutorialProgress by viewModel.tutorialProgress.collectAsStateWithLifecycle()
    val draftResistance by viewModel.draftResistance.collectAsStateWithLifecycle()
    val draftLedColor by viewModel.draftLedColor.collectAsStateWithLifecycle()
    val draftBatteryVolts by viewModel.draftBatteryVolts.collectAsStateWithLifecycle()
    val draftTimerFrequency by viewModel.draftTimerFrequency.collectAsStateWithLifecycle()
    val draftCapacitance by viewModel.draftCapacitance.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    val density = LocalDensity.current.density
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var showDrc by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showNetlist by remember { mutableStateOf(false) }
    var showPartsSheet by remember { mutableStateOf(false) }
    var showScratch by remember { mutableStateOf(false) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    val haptic = HapticFeedback.rememberHaptic()

    Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background)) {
        // ================= LIENZO =================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSizePx = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val state = viewModel.panZoom.value
                        viewModel.updatePanZoom(
                            state.panX + pan.x / density,
                            state.panY + pan.y / density,
                            state.zoom * zoomChange
                        )
                    }
                }
                .pointerInput(components, wires, connecting) {
                    detectTapGestures(
                        onTap = { offset ->
                            val worldX = (offset.x / density - viewModel.panZoom.value.panX) /
                                viewModel.panZoom.value.zoom
                            val worldY = (offset.y / density - viewModel.panZoom.value.panY) /
                                viewModel.panZoom.value.zoom
                            handleTap(worldX, worldY, components, wires, viewModel)
                        },
                        onLongPress = { offset ->
                            val worldX = (offset.x / density - viewModel.panZoom.value.panX) /
                                viewModel.panZoom.value.zoom
                            val worldY = (offset.y / density - viewModel.panZoom.value.panY) /
                                viewModel.panZoom.value.zoom
                            handleLongPress(worldX, worldY, components, wires, viewModel)
                        }
                    )
                }
        ) {
            // Rejilla
            GridCanvas(
                panX = panZoom.panX,
                panY = panZoom.panY,
                zoom = panZoom.zoom,
                gridSize = gridSize.toFloat(),
                visible = showGrid
            )

            // Partículas de energía
            ParticleLayer(
                particles = particles,
                wires = wires,
                components = components,
                panOffset = Offset(panZoom.panX, panZoom.panY),
                zoom = panZoom.zoom
            )

            // Cables
            WiresCanvas(
                components = components,
                wires = wires,
                simulation = simulation,
                panX = panZoom.panX,
                panY = panZoom.panY,
                zoom = panZoom.zoom
            )

            // Piezas (como Composables para mostrar emoji)
            components.forEach { comp ->
                val isDragging = draggingId == comp.id
                val extra = if (isDragging) dragOffset else Offset.Zero
                ComponentNode(
                    component = comp,
                    selected = selectedId == comp.id,
                    connectingFrom = connecting?.componentId == comp.id,
                    zoom = panZoom.zoom,
                    panX = panZoom.panX,
                    panY = panZoom.panY,
                    extra = extra,
                    current = simulation.componentCurrents[comp.id] ?: 0f,
                    onPinTap = { pinIndex ->
                        if (connecting != null) {
                            viewModel.completeWireConnection(comp.id, pinIndex)
                        } else {
                            viewModel.selectComponent(comp.id)
                            viewModel.startWireConnection(comp.id, pinIndex, comp.position.x, comp.position.y)
                        }
                    },
                    onBodyTap = {
                        viewModel.selectComponent(if (selectedId == comp.id) null else comp.id)
                    },
                    onDragStart = {
                        draggingId = comp.id
                        dragOffset = Offset.Zero
                    },
                    onDrag = { delta ->
                        dragOffset += Offset(delta.x / density / panZoom.zoom, delta.y / density / panZoom.zoom)
                    },
                    onDragEnd = {
                        if (draggingId == comp.id) {
                            viewModel.moveComponent(
                                comp.id,
                                comp.position.x + dragOffset.x,
                                comp.position.y + dragOffset.y
                            )
                        }
                        draggingId = null
                        dragOffset = Offset.Zero
                    }
                )
            }
        }

        // ================= BARRA SUPERIOR =================
        WorkbenchTopBar(
            title = circuit?.name ?: "Mesa de Trabajo",
            isRunning = isRunning,
            undoAvailable = undo.isNotEmpty(),
            redoAvailable = redo.isNotEmpty(),
            onBack = {
                viewModel.flushSave()
                onNavigateUp()
            },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onRun = {
                if (isRunning) viewModel.saveCircuit()
                viewModel.toggleRunning()
            },
            onScope = { viewModel.toggleOscilloscope() },
            onLibrary = { viewModel.toggleLibrary() },
            onReview = { showDrc = true; viewModel.reviewCircuit() },
            onExport = { showExport = true },
            onNetlist = { showNetlist = !showNetlist },
            onScratch = { viewModel.toggleScratch() },
            onAi = { viewModel.askAi() },
            saveLabel = when (saveState) {
                WorkbenchViewModel.SaveState.SAVING -> "Guardando…"
                WorkbenchViewModel.SaveState.SAVED -> "Guardado ✓"
                else -> ""
            }
        )

        // ================= ESTADO / AYUDA =================
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp, start = 12.dp, end = 12.dp)
        ) {
            if (connecting == null && selectedId == null && helpCards.isNotEmpty()) {
                HelpBanner(helpCards[0])
            }
        }

        // ================= ZOOM Y REENCUADRE =================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZoomButton("+") {
                haptic.lightClick()
                val pz = viewModel.panZoom.value
                viewModel.updatePanZoom(pz.panX, pz.panY, pz.zoom * 1.25f)
            }
            ZoomButton("−") {
                haptic.lightClick()
                val pz = viewModel.panZoom.value
                viewModel.updatePanZoom(pz.panX, pz.panY, pz.zoom / 1.25f)
            }
            ZoomButton("⌖") {
                haptic.lightClick()
                viewModel.resetPanZoom()
            }
        }

        // ================= MULTÍMETRO FLOTANTE =================
        multimeter?.let { meter ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 190.dp)
            ) {
                ProMultimeter(
                    voltage = meter.voltage,
                    current = meter.current,
                    wireId = meter.wireId,
                    wireLabel = meter.label,
                    history = meter.history,
                    isPinned = false,
                    onPin = { /* fijado no implementado en v1 */ },
                    onDismiss = { viewModel.closeMultimeter() }
                )
            }
        }

        // ================= PANEL DE PIEZA SELECCIONADA =================
        selectedId?.let { id ->
            components.find { it.id == id }?.let { component ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 188.dp)
                        .width(300.dp)
                ) {
                    ComponentPropertiesPanel(
                        component = component,
                        current = simulation.componentCurrents[component.id] ?: 0f,
                        onProperty = { name, value -> viewModel.setProperty(id, name, value) },
                        onLabel = { viewModel.setLabel(id, it) },
                        onDelete = { viewModel.deleteComponent(id) },
                        onRotate = { viewModel.rotateComponent(id) },
                        onClose = { viewModel.selectComponent(null) }
                    )
                }
            }
        }

        // ================= NETLIST =================
        if (showNetlist) {
            NetlistOverlay(lines = netlist, onClose = { showNetlist = false })
        }

        // ================= BOTÓN FLOTANTE DE PIEZAS =================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Píldora principal: abre el panel
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .background(CircuitColors.Surface.copy(alpha = 0.97f), CircuitShapes.PillShape)
                    .border(1.dp, CircuitColors.Outline, CircuitShapes.PillShape)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            haptic.lightClick()
                            showPartsSheet = true
                        }
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧩", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Piezas",
                        color = CircuitColors.OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W700
                    )
                    Text(
                        "Desliza para abrir",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 8.sp
                    )
                }
            }
            // Acceso rápido a plantillas
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(CircuitColors.Surface.copy(alpha = 0.97f), CircleShape)
                    .border(1.dp, CircuitColors.Outline, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            haptic.lightClick()
                            showPartsSheet = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("📋", fontSize = 18.sp)
            }
        }

        // ================= PANEL DESLIZABLE DE PIEZAS =================
        if (showPartsSheet) {
            // Centro visible del lienzo para colocar la pieza
            val centerWorldX = ((canvasSizePx.width / 2f / density) - panZoom.panX) / panZoom.zoom
            val centerWorldY = ((canvasSizePx.height / 2f / density) - panZoom.panY) / panZoom.zoom
            // Pequeño desfase para que no se apilen en el mismo punto
            val spread = (components.size % 3) * 30f - 30f
            PartsBottomSheet(
                worldX = centerWorldX + spread,
                worldY = centerWorldY + spread,
                templates = CircuitTemplates.ALL_TEMPLATES,
                draftResistance = draftResistance,
                draftLedColor = draftLedColor,
                draftBatteryVolts = draftBatteryVolts,
                draftTimerFrequency = draftTimerFrequency,
                draftCapacitance = draftCapacitance,
                onResistance = { viewModel.setDraftResistance(it) },
                onLedColor = { viewModel.setDraftLedColor(it) },
                onBatteryVolts = { viewModel.setDraftBatteryVolts(it) },
                onTimerFrequency = { viewModel.setDraftTimerFrequency(it) },
                onCapacitance = { viewModel.setDraftCapacitance(it) },
                onAdd = { type ->
                    viewModel.addComponent(type, centerWorldX + spread, centerWorldY + spread)
                },
                onTemplate = { template ->
                    viewModel.loadTemplate(template)
                    showPartsSheet = false
                },
                onOpenLibrary = {
                    showPartsSheet = false
                    viewModel.toggleLibrary()
                },
                onDismiss = { showPartsSheet = false }
            )
        }

        // ================= SCRATCH PANEL =================
        if (showScratch) {
            ScratchPanel(
                engine = scratchEngine,
                onClose = { viewModel.showScratch = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ================= DRC =================
        if (showDrc) {
            DrcOverlay(
                report = drc,
                onClose = { showDrc = false },
                onReviewAgain = { viewModel.reviewCircuit() }
            )
        }

        // ================= EXPORT =================
        if (showExport) {
            ExportOverlay(
                preview = exportPreview,
                onJson = { viewModel.exportJson() },
                onSpice = { viewModel.exportSpice() },
                onKiCad = { viewModel.exportKiCad() },
                onSvg = { viewModel.exportSvg() },
                onCsv = { viewModel.exportCsv() },
                onMarkdown = { viewModel.exportMarkdown() },
                onClose = { showExport = false; viewModel.closeExport() }
            )
        }

        // ================= IA =================
        if (aiLoading || aiResponse != null) {
            AiOverlay(
                loading = aiLoading,
                message = aiResponse,
                onClose = { viewModel.clearAiResponse() }
            )
        }

        // ================= OSCILOSCOPIO =================
        if (showScope) {
            OscilloscopeScreen(
                channels = channels,
                isRunning = isRunning,
                onToggleRun = { viewModel.toggleRunning() },
                onClose = { viewModel.toggleOscilloscope() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ================= TUTORIAL =================
        tutorialStep?.let { step ->
            TutorialCardOverlay(
                step = step,
                progress = tutorialProgress,
                onNext = { viewModel.nextTutorialStep() },
                onSkip = { viewModel.skipTutorial() }
            )
        }

        // ================= BIBLIOTECA =================
        if (showLibrary) {
            LibraryScreen(
                onPick = { type -> viewModel.addComponent(type, 120f, 160f) },
                onClose = { viewModel.toggleLibrary() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ================= HIT TESTING =================

private fun handleTap(
    worldX: Float,
    worldY: Float,
    components: List<CircuitComponent>,
    wires: List<CircuitWire>,
    viewModel: WorkbenchViewModel
) {
    // ¿Tocó un punto de conexión?
    for (comp in components) {
        for (pin in 0 until comp.type.pinCount) {
            val (px, py) = pinWorld(comp, pin)
            if (dist(worldX, worldY, px, py) < 18f) {
                if (viewModel.connectingWireStart.value != null) {
                    viewModel.completeWireConnection(comp.id, pin)
                } else {
                    viewModel.selectComponent(comp.id)
                    viewModel.startWireConnection(comp.id, pin, px, py)
                }
                return
            }
        }
    }
    // ¿Tocó una pieza?
    for (comp in components) {
        if (dist(worldX, worldY, comp.position.x, comp.position.y) < 44f) {
            viewModel.selectComponent(if (viewModel.selectedComponentId.value == comp.id) null else comp.id)
            return
        }
    }
    // ¿Tocó un cable? => multímetro
    for (wire in wires) {
        val from = components.find { it.id == wire.fromComponentId } ?: continue
        val to = components.find { it.id == wire.toComponentId } ?: continue
        val (ax, ay) = pinWorld(from, wire.fromPinIndex)
        val (bx, by) = pinWorld(to, wire.toPinIndex)
        if (distanceToSegment(worldX, worldY, ax, ay, bx, by) < 14f) {
            viewModel.showMultimeter(wire)
            return
        }
    }
    // Vacío
    viewModel.selectComponent(null)
    viewModel.cancelWireConnection()
    viewModel.closeMultimeter()
}

/**
 * Mantener pulsado sobre un cable lo elimina (con opción de deshacer).
 */
private fun handleLongPress(
    worldX: Float,
    worldY: Float,
    components: List<CircuitComponent>,
    wires: List<CircuitWire>,
    viewModel: WorkbenchViewModel
) {
    for (wire in wires) {
        val from = components.find { it.id == wire.fromComponentId } ?: continue
        val to = components.find { it.id == wire.toComponentId } ?: continue
        val (ax, ay) = pinWorld(from, wire.fromPinIndex)
        val (bx, by) = pinWorld(to, wire.toPinIndex)
        if (distanceToSegment(worldX, worldY, ax, ay, bx, by) < 18f) {
            viewModel.deleteWire(wire.id)
            return
        }
    }
}

private fun pinWorld(component: CircuitComponent, pin: Int): Pair<Float, Float> {
    val count = component.type.pinCount
    val step = if (count > 1) 360f / count else 0f
    val angle = Math.toRadians((pin * step - 90.0 + component.position.rotation).toDouble())
    val radius = 40f
    return (component.position.x + radius * kotlin.math.cos(angle).toFloat()) to
        (component.position.y + radius * kotlin.math.sin(angle).toFloat())
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun distanceToSegment(
    px: Float, py: Float,
    ax: Float, ay: Float,
    bx: Float, by: Float
): Float {
    val dx = bx - ax
    val dy = by - ay
    val lengthSq = dx * dx + dy * dy
    if (lengthSq < 1e-6f) return dist(px, py, ax, ay)
    val t = (((px - ax) * dx + (py - ay) * dy) / lengthSq).coerceIn(0f, 1f)
    return dist(px, py, ax + t * dx, ay + t * dy)
}

// ================= CAPAS DE DIBUJO =================

@Composable
private fun GridCanvas(
    panX: Float, panY: Float, zoom: Float, gridSize: Float, visible: Boolean
) {
    if (!visible) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepPx = (gridSize * zoom).dp.toPx().coerceAtLeast(8f)
        val startX = (panX * zoom).dp.toPx() % stepPx
        val startY = (panY * zoom).dp.toPx() % stepPx

        var x = startX
        while (x < size.width) {
            drawLine(
                color = CircuitColors.GridMinor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += stepPx
        }
        var y = startY
        while (y < size.height) {
            drawLine(
                color = CircuitColors.GridMinor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += stepPx
        }
        // Eje central
        drawLine(
            color = CircuitColors.GridMajor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun WiresCanvas(
    components: List<CircuitComponent>,
    wires: List<CircuitWire>,
    simulation: com.circuitnexus.model.SimulationState,
    panX: Float, panY: Float, zoom: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (wire in wires) {
            val from = components.find { it.id == wire.fromComponentId } ?: continue
            val to = components.find { it.id == wire.toComponentId } ?: continue
            val (ax, ay) = pinWorld(from, wire.fromPinIndex)
            val (bx, by) = pinWorld(to, wire.toPinIndex)

            val start = Offset(
                (ax * zoom + panX).dp.toPx(),
                (ay * zoom + panY).dp.toPx()
            )
            val end = Offset(
                (bx * zoom + panX).dp.toPx(),
                (by * zoom + panY).dp.toPx()
            )

            val current = simulation.wireCurrents[wire.id] ?: 0f
            val color = when {
                abs(current) > 0.01f -> CircuitColors.Emerald
                abs(current) > 0.001f -> CircuitColors.Amber
                else -> CircuitColors.WireIdle
            }

            val path = Path().apply {
                moveTo(start.x, start.y)
                val midX = (start.x + end.x) / 2f
                val midY = (start.y + end.y) / 2f
                val dx = end.x - start.x
                val dy = end.y - start.y
                val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val curve = (length * 0.15f).coerceAtMost(40f)
                quadTo(midX - dy / length * curve, midY + dx / length * curve, end.x, end.y)
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ComponentNode(
    component: CircuitComponent,
    selected: Boolean,
    connectingFrom: Boolean,
    zoom: Float,
    panX: Float,
    panY: Float,
    extra: Offset,
    current: Float,
    onPinTap: (Int) -> Unit,
    onBodyTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val sizeDp = 80f * zoom
    val centerX = (component.position.x + extra.x) * zoom + panX
    val centerY = (component.position.y + extra.y) * zoom + panY
    val left = centerX - sizeDp / 2f
    val top = centerY - sizeDp / 2f

    Box(
        modifier = Modifier
            .offset(x = left.dp, y = top.dp)
            .size(sizeDp.dp)
            .pointerInput(component.id) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .pointerInput(component.id) {
                detectTapGestures { onBodyTap() }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        selected -> CircuitColors.EmeraldContainer
                        connectingFrom -> CircuitColors.AmberContainer
                        else -> CircuitColors.Surface
                    },
                    CircuitShapes.CardShape
                )
                .border(
                    if (selected) 2.dp else 1.dp,
                    when {
                        selected -> CircuitColors.Emerald
                        connectingFrom -> CircuitColors.Amber
                        else -> CircuitColors.Outline
                    },
                    CircuitShapes.CardShape
                )
                .rotate(component.position.rotation),
            contentAlignment = Alignment.Center
        ) {
            if (component.type.category == com.circuitnexus.model.ComponentCategory.DISPLAY) {
                // Pantallas con aspecto de LCD real (fondo verdoso y dígitos de fósforo)
                val screenColor = when (component.type) {
                    ComponentType.LCD_16X2 -> Color(0xFF0A2A12)
                    ComponentType.OLED_SSD1306 -> Color(0xFF050505)
                    else -> Color(0xFF07180D)
                }
                val screenText = when (component.type) {
                    ComponentType.LCD_16X2, ComponentType.OLED_SSD1306 ->
                        (component.properties.text ?: "CircuitNexus").take(20)
                    ComponentType.DISPLAY_7SEG_10 -> "0000000000"
                    ComponentType.DISPLAY_7SEG_8 -> "00000000"
                    ComponentType.DISPLAY_7SEG_4 -> "0000"
                    else -> component.properties.text?.take(12) ?: component.type.emoji
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((4f * zoom).coerceIn(2f, 8f).dp)
                        .background(screenColor, CircuitShapes.SmallShape)
                        .border(1.dp, CircuitColors.Emerald.copy(alpha = 0.25f), CircuitShapes.SmallShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = screenText,
                        color = CircuitColors.Emerald,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = (8f * zoom).coerceIn(5f, 13f).sp,
                        fontWeight = FontWeight.W700,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(3.dp)
                    )
                }
            } else {
                Text(
                    component.type.emoji,
                    fontSize = (26f * zoom).coerceIn(14f, 44f).sp
                )
            }
        }

        // Puntos de conexión
        for (pin in 0 until component.type.pinCount) {
            val count = component.type.pinCount
            val step = if (count > 1) 360f / count else 0f
            val angle = Math.toRadians((pin * step - 90).toDouble())
            val radius = sizeDp / 2f
            val px = radius + radius * kotlin.math.cos(angle).toFloat()
            val py = radius + radius * kotlin.math.sin(angle).toFloat()

            Box(
                modifier = Modifier
                    .offset(
                        x = (px - 9f * zoom).dp,
                        y = (py - 9f * zoom).dp
                    )
                    .size((18f * zoom).coerceIn(12f, 26f).dp)
                    .background(CircuitColors.ComponentPin, CircleShape)
                    .pointerInput(component.id, pin) {
                        detectTapGestures { onPinTap(pin) }
                    }
            )
        }

        // Corriente
        if (current > 0.005f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (sizeDp * 0.25f).dp)
                    .background(CircuitColors.Emerald, CircuitShapes.PillShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    "${(current * 1000).roundToInt()}mA",
                    color = CircuitColors.EmeraldOn,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.W700
                )
            }
        }
    }
}

// ================= BARRA SUPERIOR =================

@Composable
private fun WorkbenchTopBar(
    title: String,
    isRunning: Boolean,
    undoAvailable: Boolean,
    redoAvailable: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRun: () -> Unit,
    onScope: () -> Unit,
    onLibrary: () -> Unit,
    onReview: () -> Unit,
    onExport: () -> Unit,
    onNetlist: () -> Unit,
    onScratch: () -> Unit,
    onAi: () -> Unit,
    saveLabel: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface.copy(alpha = 0.97f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconPill("‹") { onBack() }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                title,
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W700,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isRunning) "● Simulando en vivo" else "● En pausa",
                    color = if (isRunning) CircuitColors.Emerald else CircuitColors.Amber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W600
                )
                if (saveLabel.isNotEmpty()) {
                    Text(
                        "   $saveLabel",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IconPill("↶", enabled = undoAvailable) { onUndo() }
            IconPill("↷", enabled = redoAvailable) { onRedo() }
            IconPill(if (isRunning) "⏸" else "▶") { onRun() }
            IconPill("📚") { onLibrary() }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface.copy(alpha = 0.97f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActionChip("🎓 Tutorial", CircuitColors.Cyan, { viewModel.startTutorial() })
        ActionChip("📈 Osciloscopio", CircuitColors.Cyan, onScope)
        ActionChip("🔍 Revisar mi Circuito", CircuitColors.Amber, onReview)
        ActionChip("🔌 Conexiones", CircuitColors.Emerald, onNetlist)
        ActionChip("📤 Compartir / Guardar", CircuitColors.Cyan, onExport)
        ActionChip("🤖 ¿Está bien conectado?", CircuitColors.Amber, onAi)
        ActionChip("🧩 Bloques Scratch", CircuitColors.Coral, onScratch)
    }
}

@Composable
private fun IconPill(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                if (enabled) CircuitColors.SurfaceVariant else CircuitColors.Surface,
                CircleShape
            )
            .pointerInput(enabled) { if (enabled) detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) CircuitColors.OnSurface else CircuitColors.OnSurfaceDisabled,
            fontSize = 15.sp,
            fontWeight = FontWeight.W700
        )
    }
}

@Composable
private fun ActionChip(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircuitShapes.PillShape)
            .border(1.dp, color.copy(alpha = 0.55f), CircuitShapes.PillShape)
            .pointerInput(text) { detectTapGestures { onClick() } }
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.W700)
    }
}

// ================= PANEL DE PROPIEDADES =================

@Composable
private fun ComponentPropertiesPanel(
    component: CircuitComponent,
    current: Float,
    onProperty: (String, Any) -> Unit,
    onLabel: (String) -> Unit,
    onDelete: () -> Unit,
    onRotate: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(CircuitColors.Surface, CircuitShapes.CardShape)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(component.type.emoji, fontSize = 18.sp)
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text(
                        component.label,
                        color = CircuitColors.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        component.type.displayName,
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconPill("↻") { onRotate() }
                IconPill("🗑") { onDelete() }
                IconPill("✕") { onClose() }
            }
        }

        if (current > 0.0005f) {
            Text(
                "Corriente: ${"%.1f".format(current * 1000f)} mA",
                color = CircuitColors.Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.W600
            )
        }

        when (component.type) {
            ComponentType.RESISTOR -> 
                SliderRow("Resistencia", component.properties.resistanceOhms ?: 330f, 1f, 1_000_000f, "Ω") {
                    onProperty("resistanceOhms", it)
                }
            ComponentType.POTENTIOMETER -> {
                SliderRow("Resistencia total", component.properties.resistanceOhms ?: 10000f, 10f, 1_000_000f, "Ω") {
                    onProperty("resistanceOhms", it)
                }
                SliderRow("Posición de la perilla", component.properties.potPosition ?: 0.5f, 0.01f, 1f, "") {
                    onProperty("potPosition", it)
                }
            }
            ComponentType.CAPACITOR ->
                SliderRow("Capacidad (µF)", (component.properties.capacitanceFarads ?: 100e-6f) * 1e6f, 0.01f, 10000f, "µF") {
                    onProperty("capacitanceFarads", it / 1e6f)
                }
            ComponentType.INDUCTOR ->
                SliderRow("Bobina (mH)", (component.properties.inductanceHenries ?: 0.01f) * 1000f, 0.01f, 1000f, "mH") {
                    onProperty("inductanceHenries", it / 1000f)
                }
            ComponentType.BATTERY ->
                SliderRow("Voltaje", component.properties.voltageVolts ?: 9f, 1f, 24f, "V") {
                    onProperty("voltageVolts", it)
                }
            ComponentType.CURRENT_SOURCE ->
                SliderRow("Corriente (mA)", (component.properties.currentAmps ?: 0.02f) * 1000f, 0.1f, 1000f, "mA") {
                    onProperty("currentAmps", it / 1000f)
                }
            ComponentType.LED -> {
                SliderRow("Voltaje de encendido", component.properties.ledForwardVoltage ?: 2f, 1.5f, 3.5f, "V") {
                    onProperty("ledForwardVoltage", it)
                }
                ColorChoice(component.properties.ledColor ?: "red") { onProperty("ledColor", it) }
            }
            ComponentType.TIMER_555 -> {
                SliderRow("Frecuencia", component.properties.frequencyHz ?: 1f, 0.1f, 1000f, "Hz") {
                    onProperty("frequencyHz", it)
                }
                SliderRow("Tiempo encendido", component.properties.dutyCycle ?: 0.5f, 0.05f, 0.95f, "") {
                    onProperty("dutyCycle", it)
                }
            }
            ComponentType.OPAMP, ComponentType.COMPARATOR ->
                SliderRow("Ganancia", component.properties.gain ?: 100000f, 1f, 100000f, "x") {
                    onProperty("gain", it)
                }
            ComponentType.REGULATOR_LINEAR, ComponentType.REGULATOR_SWITCHING ->
                SliderRow("Voltaje de salida", component.properties.regulationVolts ?: 5f, 1.2f, 24f, "V") {
                    onProperty("regulationVolts", it)
                }
            ComponentType.SWITCH, ComponentType.RELAY ->
                ToggleRow("Normalmente abierto", component.properties.isNormallyOpen ?: true) {
                    onProperty("isNormallyOpen", it)
                }
            ComponentType.DIODE, ComponentType.SCHOTTKY_DIODE, ComponentType.ZENER_DIODE ->
                SliderRow("Voltaje de paso", component.properties.forwardVoltage ?: 0.7f, 0.2f, 2f, "V") {
                    onProperty("forwardVoltage", it)
                }
            ComponentType.THERMISTOR, ComponentType.LDR, ComponentType.SENSOR_TEMP,
            ComponentType.SENSOR_DISTANCE, ComponentType.SENSOR_MOTION ->
                SliderRow("Valor del sensor", component.properties.sensorValue ?: 0.5f, 0f, 1f, "") {
                    onProperty("sensorValue", it)
                }
            ComponentType.BUZZER, ComponentType.STEPPER ->
                SliderRow("Frecuencia", component.properties.frequencyHz ?: 440f, 20f, 5000f, "Hz") {
                    onProperty("frequencyHz", it)
                }
            ComponentType.MOTOR_DC, ComponentType.SERVO ->
                SliderRow("Velocidad", component.properties.pwmDuty ?: 1f, 0f, 1f, "") {
                    onProperty("pwmDuty", it)
                }
            ComponentType.DISPLAY_7SEG_4, ComponentType.DISPLAY_7SEG_8,
            ComponentType.DISPLAY_7SEG_10, ComponentType.LCD_16X2, ComponentType.OLED_SSD1306 -> {
                TextInputRow("Texto a mostrar", component.properties.text ?: "") { onProperty("text", it) }
                SliderRow("Brillo", component.properties.brightness ?: 1f, 0f, 1f, "") {
                    onProperty("brightness", it)
                }
            }
            else -> {
                Text(
                    "Esta pieza no tiene ajustes. ¡Pruébala en el circuito!",
                    color = CircuitColors.OnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        // Etiqueta
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nombre:", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
            BasicTextField(
                value = component.label,
                onValueChange = onLabel,
                textStyle = TextStyle(color = CircuitColors.OnSurface, fontSize = 10.sp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
                    .background(CircuitColors.SurfaceVariant, CircuitShapes.SmallShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
            Text(
                formatValue(value, unit),
                color = CircuitColors.Emerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.W700
            )
        }
        androidx.compose.material3.Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.height(24.dp),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = CircuitColors.Emerald,
                activeTrackColor = CircuitColors.Emerald,
                inactiveTrackColor = CircuitColors.Outline
            )
        )
    }
}

private fun formatValue(value: Float, unit: String): String {
    val text = when {
        value >= 1_000_000f -> "%.2fM".format(value / 1_000_000f)
        value >= 1000f -> "%.2fk".format(value / 1000f)
        value >= 10f -> "%.1f".format(value)
        value >= 0.1f -> "%.2f".format(value)
        else -> "%.3f".format(value)
    }
    return if (unit.isBlank()) text else "$text $unit"
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = CircuitColors.Emerald,
                checkedTrackColor = CircuitColors.Emerald.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun TextInputRow(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = CircuitColors.OnSurface, fontSize = 11.sp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.SurfaceVariant, CircuitShapes.SmallShape)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ColorChoice(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf(
        "red" to "🔴", "green" to "🟢", "blue" to "🔵",
        "yellow" to "🟡", "white" to "⚪"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        colors.forEach { (key, emoji) ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (selected == key) CircuitColors.Emerald.copy(alpha = 0.25f)
                        else CircuitColors.SurfaceVariant,
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (selected == key) CircuitColors.Emerald else CircuitColors.Outline,
                        CircleShape
                    )
                    .pointerInput(key) { detectTapGestures { onSelect(key) } },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 13.sp)
            }
        }
    }
}

// ================= OVERLAYS =================

@Composable
private fun HelpBanner(card: HelpCard) {
    val color = when (card.type) {
        HelpType.WELCOME -> CircuitColors.Emerald
        HelpType.TIP -> CircuitColors.Cyan
        HelpType.CAUTION -> CircuitColors.Amber
        HelpType.DANGER -> CircuitColors.Coral
        HelpType.INFO -> CircuitColors.Cyan
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), CircuitShapes.CardShape)
            .border(1.dp, color.copy(alpha = 0.55f), CircuitShapes.CardShape)
            .padding(10.dp)
    ) {
        Text(card.title, color = color, fontSize = 10.sp, fontWeight = FontWeight.W700)
        Text(card.message, color = CircuitColors.OnSurface, fontSize = 10.sp)
    }
}

@Composable
private fun DrcOverlay(
    report: com.circuitnexus.engine.DrcEngine.DrcReport?,
    onClose: () -> Unit,
    onReviewAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(2.dp, CircuitColors.Emerald, CircuitShapes.CardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Revisar mi Circuito",
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W700,
                fontSize = 16.sp
            )
            if (report == null) {
                Text("Revisando...", color = CircuitColors.OnSurfaceVariant, fontSize = 11.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Puntaje: ${report.score}/100",
                        color = scoreColor(report.score),
                        fontWeight = FontWeight.W700,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(report.summary, color = CircuitColors.OnSurfaceVariant, fontSize = 10.sp)
                }

                if (report.issues.isEmpty()) {
                    Text(
                        "✅ Todo perfecto. No encontré problemas.",
                        color = CircuitColors.Emerald,
                        fontSize = 11.sp
                    )
                }

                report.issues.take(8).forEach { issue ->
                    val color = when (issue.severity) {
                        WarningSeverity.ERROR -> CircuitColors.Coral
                        WarningSeverity.WARNING -> CircuitColors.Amber
                        WarningSeverity.INFO -> CircuitColors.Cyan
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color.copy(alpha = 0.10f), CircuitShapes.SmallShape)
                            .padding(8.dp)
                    ) {
                        Text("${issue.title}", color = color, fontSize = 10.sp, fontWeight = FontWeight.W700)
                        Text(issue.message, color = CircuitColors.OnSurface, fontSize = 9.sp)
                        if (issue.suggestion.isNotBlank()) {
                            Text("→ ${issue.suggestion}", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.Cyan.copy(alpha = 0.16f), CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onReviewAgain() } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Revisar otra vez", color = CircuitColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.W700)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.Coral.copy(alpha = 0.16f), CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onClose() } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cerrar", color = CircuitColors.Coral, fontSize = 11.sp, fontWeight = FontWeight.W700)
                }
            }
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 85 -> CircuitColors.Emerald
    score >= 60 -> CircuitColors.Amber
    else -> CircuitColors.Coral
}

@Composable
private fun NetlistOverlay(lines: List<String>, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(1.dp, CircuitColors.Emerald, CircuitShapes.CardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Conexiones del circuito",
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W700,
                fontSize = 15.sp
            )
            Text(
                "Así está unido todo, en palabras sencillas. " +
                    "Mantén pulsado un cable en la mesa para borrarlo.",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 10.sp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                lines.forEachIndexed { index, line ->
                    Text(
                        "${index + 1}. $line",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CircuitColors.Coral.copy(alpha = 0.16f), CircuitShapes.PillShape)
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cerrar", color = CircuitColors.Coral, fontSize = 11.sp, fontWeight = FontWeight.W700)
            }
        }
    }
}

@Composable
private fun ExportOverlay(
    preview: WorkbenchViewModel.ExportPreview?,
    onJson: () -> Unit,
    onSpice: () -> Unit,
    onKiCad: () -> Unit,
    onSvg: () -> Unit,
    onCsv: () -> Unit,
    onMarkdown: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(1.dp, CircuitColors.Cyan, CircuitShapes.CardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Compartir o guardar tu circuito",
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W700,
                fontSize = 15.sp
            )
            Text(
                "Elige un formato. Nada se sube a internet: todo se queda en tu teléfono.",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 10.sp
            )

            ExportButton("📦 Copia completa (JSON)", CircuitColors.Emerald, onJson)
            ExportButton("🧪 Para otros simuladores (SPICE)", CircuitColors.Cyan, onSpice)
            ExportButton("📐 Para editar en KiCad", CircuitColors.Cyan, onKiCad)
            ExportButton("🖼 Dibujo vectorial (SVG)", CircuitColors.Amber, onSvg)
            ExportButton("📊 Tabla de medidas (CSV)", CircuitColors.Amber, onCsv)
            ExportButton("📝 Receta en Markdown", CircuitColors.Cyan, onMarkdown)

            if (preview != null) {
                Text(
                    "${preview.title} (${preview.format})",
                    color = CircuitColors.Emerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black, CircuitShapes.SmallShape)
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            preview.content.take(4000),
                            color = CircuitColors.OnSurfaceVariant,
                            fontSize = 8.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CircuitColors.Coral.copy(alpha = 0.16f), CircuitShapes.PillShape)
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cerrar", color = CircuitColors.Coral, fontSize = 11.sp, fontWeight = FontWeight.W700)
            }
        }
    }
}

@Composable
private fun ExportButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.13f), CircuitShapes.PillShape)
            .border(1.dp, color.copy(alpha = 0.5f), CircuitShapes.PillShape)
            .pointerInput(text) { detectTapGestures { onClick() } }
            .padding(vertical = 9.dp, horizontal = 12.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.W600)
    }
}

@Composable
private fun AiOverlay(loading: Boolean, message: String?, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(1.dp, CircuitColors.Amber, CircuitShapes.CardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "🤖 Asistente Inteligente",
                color = CircuitColors.Amber,
                fontWeight = FontWeight.W700,
                fontSize = 15.sp
            )
            if (loading) {
                Text("Pensando…", color = CircuitColors.OnSurfaceVariant, fontSize = 11.sp)
            } else {
                Text(
                    message ?: "",
                    color = CircuitColors.OnSurface,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Entendido", color = CircuitColors.OnSurface, fontSize = 11.sp, fontWeight = FontWeight.W700)
            }
        }
    }
}


@Composable
private fun TutorialCardOverlay(
    step: com.circuitnexus.ui.tutorial.TutorialStep,
    progress: Float,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(2.dp, CircuitColors.Emerald, CircuitShapes.CardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    step.title,
                    color = CircuitColors.OnSurface,
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "✕",
                    color = CircuitColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { onSkip() } }
                )
            }

            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(CircuitColors.Emerald, CircuitShapes.PillShape)
                )
            }

            Text(
                step.instruction.replace("**", ""),
                color = CircuitColors.OnSurface,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onSkip() } }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Saltar", color = CircuitColors.OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.W600)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.Emerald, CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onNext() } }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Siguiente →", color = CircuitColors.EmeraldOn, fontSize = 11.sp, fontWeight = FontWeight.W700)
                }
            }
        }
    }
}


@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(CircuitColors.Surface.copy(alpha = 0.95f), CircleShape)
            .border(1.dp, CircuitColors.Outline, CircleShape)
            .pointerInput(label) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = CircuitColors.OnSurface,
            fontSize = if (label == "⌖") 15.sp else 19.sp,
            fontWeight = FontWeight.W700
        )
    }
}
