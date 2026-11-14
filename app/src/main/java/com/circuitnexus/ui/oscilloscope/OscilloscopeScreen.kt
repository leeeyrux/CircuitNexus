package com.circuitnexus.ui.oscilloscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Un canal del osciloscopio: una señal de un punto del circuito.
 */
data class ScopeChannel(
    val id: String,
    val label: String,
    val color: Color,
    val samples: FloatArray
)

/**
 * Osciloscopio estilo analógico (fósforo verde), con:
 *  - 2 a 4 canales
 *  - base de tiempo ajustable
 *  - disparo por flanco con nivel
 *  - cursores de tiempo y voltaje
 *  - medidas automáticas
 *  - persistencia de fósforo (estela)
 */
@Composable
fun OscilloscopeScreen(
    channels: List<ScopeChannel>,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeBaseIndex by remember { mutableIntStateOf(3) }
    var voltsPerDiv by remember { mutableFloatStateOf(2f) }
    var triggerLevel by remember { mutableFloatStateOf(0f) }
    var risingEdge by remember { mutableStateOf(true) }
    var persistence by remember { mutableIntStateOf(2) }
    var cursorMode by remember { mutableStateOf(CursorMode.NONE) }
    var cursorA by remember { mutableFloatStateOf(0.35f) }
    var cursorB by remember { mutableFloatStateOf(0.72f) }
    var showMeasurements by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(ScopeMode.YT) }

    val timeBase = TIME_BASES[timeBaseIndex]
    val visibleSamples = remember(timeBaseIndex) {
        (timeBase.samples.coerceAtLeast(40))
    }

    val widthFraction = 1f

    Box(modifier = modifier.fillMaxSize().background(CircuitColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== Barra superior =====
            ScopeTopBar(
                channels = channels,
                isRunning = isRunning,
                timeBaseLabel = timeBase.label,
                voltsPerDiv = voltsPerDiv,
                onToggleRun = onToggleRun,
                onClose = onClose
            )

            // ===== Pantalla =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Color(0xFF030705), CircuitShapes.MediumShape)
                    .border(1.dp, CircuitColors.Emerald.copy(alpha = 0.35f), CircuitShapes.MediumShape)
            ) {
                ScopeGrid(
                    channels = channels,
                    visibleSamples = visibleSamples,
                    voltsPerDiv = voltsPerDiv,
                    triggerLevel = triggerLevel,
                    risingEdge = risingEdge,
                    persistence = persistence,
                    cursorMode = cursorMode,
                    cursorA = cursorA,
                    cursorB = cursorB,
                    mode = mode
                )

                // Etiquetas de canales (fuera del canvas para poder usar Text)
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    channels.forEach { channel ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(channel.color, CircleShape)
                            )
                            Text(
                                text = "  ${channel.label}",
                                color = channel.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.W700
                            )
                        }
                    }
                }

                // Escala vertical
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("+${"%.0f".format(voltsPerDiv * 4)}V", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
                    Text(" 0V", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
                    Text("-${"%.0f".format(voltsPerDiv * 4)}V", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp)
                }

                // Estado de disparo
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                ) {
                    Text(
                        text = if (isRunning) "● CORRIENDO" else "● PAUSADO",
                        color = if (isRunning) CircuitColors.Emerald else CircuitColors.Amber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W700
                    )
                }

                // Cursores
                if (cursorMode != CursorMode.NONE) {
                    CursorReadout(
                        channels = channels,
                        cursorMode = cursorMode,
                        cursorA = cursorA,
                        cursorB = cursorB,
                        timeBase = timeBase,
                        voltsPerDiv = voltsPerDiv,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
            }

            // ===== Medidas automáticas =====
            if (showMeasurements && channels.isNotEmpty()) {
                MeasurementsStrip(channels = channels)
            }

            // ===== Controles =====
            ScopeControls(
                timeBaseIndex = timeBaseIndex,
                voltsPerDiv = voltsPerDiv,
                triggerLevel = triggerLevel,
                risingEdge = risingEdge,
                persistence = persistence,
                cursorMode = cursorMode,
                mode = mode,
                onTimeBase = { timeBaseIndex = it.coerceIn(0, TIME_BASES.size - 1) },
                onVoltsPerDiv = { voltsPerDiv = it.coerceIn(0.1f, 50f) },
                onTriggerLevel = { triggerLevel = it.coerceIn(-20f, 20f) },
                onRisingEdge = { risingEdge = it },
                onPersistence = { persistence = it.coerceIn(0, 3) },
                onCursorMode = { cursorMode = it },
                onMode = { mode = it },
                onToggleMeasurements = { showMeasurements = !showMeasurements }
            )
        }
    }
}

// ===== Pantalla =====

@Composable
private fun ScopeGrid(
    channels: List<ScopeChannel>,
    visibleSamples: Int,
    voltsPerDiv: Float,
    triggerLevel: Float,
    risingEdge: Boolean,
    persistence: Int,
    cursorMode: CursorMode,
    cursorA: Float,
    cursorB: Float,
    mode: ScopeMode
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val div = h / 8f

        // Estela de fósforo
        if (persistence > 0) {
            val alpha = when (persistence) { 1 -> 0.08f; 2 -> 0.16f; else -> 0.28f }
            drawRect(color = Color(0xFF030705).copy(alpha = alpha), size = Size(w, h))
        }

        // Rejilla 10 x 8
        for (i in 0..10) {
            val x = w * i / 10f
            val strong = i == 5
            drawLine(
                color = CircuitColors.Emerald.copy(alpha = if (strong) 0.30f else 0.10f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = if (strong) 1f else 0.7f
            )
        }
        for (j in 0..8) {
            val y = h * j / 8f
            val strong = j == 4
            drawLine(
                color = CircuitColors.Emerald.copy(alpha = if (strong) 0.30f else 0.10f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (strong) 1f else 0.7f
            )
        }

        // Nivel de disparo
        val trigY = midY - (triggerLevel / voltsPerDiv) * div
        if (trigY in 0f..h) {
            drawLine(
                color = CircuitColors.Amber.copy(alpha = 0.7f),
                start = Offset(0f, trigY),
                end = Offset(w, trigY),
                strokeWidth = 1f
            )
            drawLine(
                color = CircuitColors.Amber,
                start = Offset(w - 10f, trigY),
                end = Offset(w, trigY),
                strokeWidth = 3f
            )
        }

        // Trazas
        if (mode == ScopeMode.YT) {
            for (channel in channels) {
                drawScopeChannel(channel, visibleSamples, voltsPerDiv, midY, div, w, h)
            }
        } else if (channels.size >= 2) {
            // Modo XY (figuras de Lissajous)
            val chX = channels[0]
            val chY = channels[1]
            val count = min(chX.samples.size, chY.samples.size)
            val path = Path()
            for (i in 0 until count) {
                val x = (chX.samples[i] / (voltsPerDiv * 4f)).coerceIn(-1f, 1f)
                val y = (chY.samples[i] / (voltsPerDiv * 4f)).coerceIn(-1f, 1f)
                val px = w / 2f + x * (w / 2f) * 0.9f
                val py = midY - y * (h / 2f) * 0.9f
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, CircuitColors.Cyan, style = Stroke(width = 1.4f))
        }

        // Cursores
        if (cursorMode == CursorMode.TIME || cursorMode == CursorMode.BOTH) {
            drawLine(
                color = CircuitColors.Emerald.copy(alpha = 0.9f),
                start = Offset(w * cursorA, 0f),
                end = Offset(w * cursorA, h),
                strokeWidth = 1.2f
            )
            drawLine(
                color = CircuitColors.Cyan.copy(alpha = 0.9f),
                start = Offset(w * cursorB, 0f),
                end = Offset(w * cursorB, h),
                strokeWidth = 1.2f
            )
        }
        if (cursorMode == CursorMode.VOLTAGE || cursorMode == CursorMode.BOTH) {
            val yA = h * cursorA
            val yB = h * cursorB
            drawLine(
                color = CircuitColors.Amber.copy(alpha = 0.9f),
                start = Offset(0f, yA),
                end = Offset(w, yA),
                strokeWidth = 1.2f
            )
            drawLine(
                color = CircuitColors.Coral.copy(alpha = 0.9f),
                start = Offset(0f, yB),
                end = Offset(w, yB),
                strokeWidth = 1.2f
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScopeChannel(
    channel: ScopeChannel,
    visibleSamples: Int,
    voltsPerDiv: Float,
    midY: Float,
    div: Float,
    width: Float,
    height: Float
) {
    val samples = channel.samples
    if (samples.size < 2) return

    val count = min(samples.size, visibleSamples)
    val startIndex = samples.size - count
    val path = Path()
    var prevX = 0f
    var prevY = midY

    for (i in 0 until count) {
        val value = samples[startIndex + i]
        val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * width
        val y = midY - (value / voltsPerDiv) * div
        val clampedY = y.coerceIn(0f, height)
        if (i == 0) {
            path.moveTo(x, clampedY)
        } else {
            path.lineTo(x, clampedY)
        }
        prevX = x
        prevY = clampedY
    }

    drawPath(path, channel.color.copy(alpha = 0.85f), style = Stroke(width = 1.6f))
    // Punto brillante al final (efecto fósforo)
    drawCircle(color = channel.color, radius = 2.4f, center = Offset(prevX, prevY))
    drawCircle(color = channel.color.copy(alpha = 0.25f), radius = 6f, center = Offset(prevX, prevY))
}

// ===== Barra superior =====

@Composable
private fun ScopeTopBar(
    channels: List<ScopeChannel>,
    isRunning: Boolean,
    timeBaseLabel: String,
    voltsPerDiv: Float,
    onToggleRun: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Osciloscopio",
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W700,
                fontSize = 16.sp
            )
            Text(
                "$timeBaseLabel/div · ${"%.1f".format(voltsPerDiv)}V/div · ${channels.size} canal(es)",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 10.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = if (isRunning) "PAUSAR" else "SEGUIR",
                color = if (isRunning) CircuitColors.Amber else CircuitColors.Emerald,
                onClick = onToggleRun
            )
            PillButton(text = "CERRAR", color = CircuitColors.Coral, onClick = onClose)
        }
    }
}

// ===== Medidas =====

@Composable
private fun MeasurementsStrip(channels: List<ScopeChannel>) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface)
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        channels.forEach { channel ->
            val stats = remember(channel.samples.size, channel.samples.lastOrNull()) {
                analyze(channel.samples)
            }
            MeasureChip("${channel.label} Vpp", "${"%.2f".format(stats.vpp)} V", channel.color)
            MeasureChip("${channel.label} Vrms", "${"%.2f".format(stats.vrms)} V", channel.color)
            MeasureChip("${channel.label} Vmed", "${"%.2f".format(stats.average)} V", channel.color)
            if (stats.frequency > 0f) {
                MeasureChip("${channel.label} Frec", formatFrequency(stats.frequency), channel.color)
            }
        }
    }
}

private fun formatFrequency(hz: Float): String = when {
    hz >= 1_000_000f -> "${"%.2f".format(hz / 1_000_000f)} MHz"
    hz >= 1_000f -> "${"%.2f".format(hz / 1_000f)} kHz"
    else -> "${"%.1f".format(hz)} Hz"
}

@Composable
private fun MeasureChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), CircuitShapes.SmallShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.W600)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.W700, fontFamily = FontFamily.Monospace)
    }
}

data class SignalStats(
    val vpp: Float = 0f,
    val vrms: Float = 0f,
    val average: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 0f,
    val frequency: Float = 0f
)

private fun analyze(samples: FloatArray): SignalStats {
    if (samples.size < 4) return SignalStats()
    var minV = Float.MAX_VALUE
    var maxV = -Float.MAX_VALUE
    var sum = 0.0
    var sumSq = 0.0
    for (s in samples) {
        if (s < minV) minV = s
        if (s > maxV) maxV = s
        sum += s
        sumSq += s.toDouble() * s.toDouble()
    }
    val n = samples.size
    val avg = (sum / n).toFloat()
    val rms = sqrt(sumSq / n).toFloat()

    // Frecuencia estimada por cruces por cero alrededor de la media
    var crossings = 0
    var lastAbove = samples[0] > avg
    for (i in 1 until n) {
        val above = samples[i] > avg
        if (above && !lastAbove) crossings++
        lastAbove = above
    }
    val durationSamples = n.toFloat()
    val freq = if (crossings > 0) crossings / durationSamples * 100f else 0f

    return SignalStats(
        vpp = maxV - minV,
        vrms = rms,
        average = avg,
        minValue = minV,
        maxValue = maxV,
        frequency = freq
    )
}

// ===== Cursores =====

@Composable
private fun CursorReadout(
    channels: List<ScopeChannel>,
    cursorMode: CursorMode,
    cursorA: Float,
    cursorB: Float,
    timeBase: TimeBase,
    voltsPerDiv: Float,
    modifier: Modifier = Modifier
) {
    val dtMs = abs(cursorB - cursorA) * timeBase.totalSeconds * 1000f
    val dv = abs(cursorB - cursorA) * voltsPerDiv * 8f

    Box(
        modifier = modifier
            .background(CircuitColors.Background.copy(alpha = 0.92f), CircuitShapes.SmallShape)
            .border(1.dp, CircuitColors.Emerald.copy(alpha = 0.6f), CircuitShapes.SmallShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cursorMode == CursorMode.TIME || cursorMode == CursorMode.BOTH) {
                Text(
                    "ΔT = ${formatTime(dtMs)}   (${if (dtMs > 0f) "${"%.1f".format(1f / (dtMs / 1000f))} Hz" else "—"})",
                    color = CircuitColors.Emerald, fontSize = 10.sp, fontWeight = FontWeight.W700
                )
            }
            if (cursorMode == CursorMode.VOLTAGE || cursorMode == CursorMode.BOTH) {
                Text(
                    "ΔV = ${"%.2f".format(dv)} V",
                    color = CircuitColors.Amber, fontSize = 10.sp, fontWeight = FontWeight.W700
                )
            }
        }
    }
}

private fun formatTime(ms: Float): String = when {
    ms >= 1000f -> "${"%.2f".format(ms / 1000f)} s"
    ms >= 1f -> "${"%.2f".format(ms)} ms"
    else -> "${"%.0f".format(ms * 1000f)} µs"
}

// ===== Controles =====

@Composable
private fun ScopeControls(
    timeBaseIndex: Int,
    voltsPerDiv: Float,
    triggerLevel: Float,
    risingEdge: Boolean,
    persistence: Int,
    cursorMode: CursorMode,
    mode: ScopeMode,
    onTimeBase: (Int) -> Unit,
    onVoltsPerDiv: (Float) -> Unit,
    onTriggerLevel: (Float) -> Unit,
    onRisingEdge: (Boolean) -> Unit,
    onPersistence: (Int) -> Unit,
    onCursorMode: (CursorMode) -> Unit,
    onMode: (ScopeMode) -> Unit,
    onToggleMeasurements: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Base de tiempo y escala
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tiempo", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp, modifier = Modifier.width(44.dp))
            StepperButton("−") { onTimeBase(timeBaseIndex - 1) }
            Text(
                TIME_BASES[timeBaseIndex].label,
                color = CircuitColors.Cyan, fontWeight = FontWeight.W700, fontSize = 12.sp,
                modifier = Modifier.width(56.dp), textAlign = TextAlign.Center
            )
            StepperButton("+") { onTimeBase(timeBaseIndex + 1) }

            Text("Escala", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp))
            StepperButton("−") { onVoltsPerDiv(voltsPerDiv / 2f) }
            Text(
                "${"%.1f".format(voltsPerDiv)}V",
                color = CircuitColors.Cyan, fontWeight = FontWeight.W700, fontSize = 12.sp,
                modifier = Modifier.width(48.dp), textAlign = TextAlign.Center
            )
            StepperButton("+") { onVoltsPerDiv(voltsPerDiv * 2f) }
        }

        // Disparo
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Disparo", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp, modifier = Modifier.width(44.dp))
            StepperButton("−") { onTriggerLevel(triggerLevel - 0.5f) }
            Text(
                "${"%.1f".format(triggerLevel)}V",
                color = CircuitColors.Amber, fontWeight = FontWeight.W700, fontSize = 12.sp,
                modifier = Modifier.width(48.dp), textAlign = TextAlign.Center
            )
            StepperButton("+") { onTriggerLevel(triggerLevel + 0.5f) }
            PillButton(
                text = if (risingEdge) "↗ Subida" else "↘ Bajada",
                color = CircuitColors.Amber,
                onClick = { onRisingEdge(!risingEdge) }
            )
        }

        // Modos
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Vista", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp, modifier = Modifier.width(44.dp))
            PillButton(
                text = if (mode == ScopeMode.YT) "Ondas" else "XY",
                color = CircuitColors.Cyan,
                onClick = { onMode(if (mode == ScopeMode.YT) ScopeMode.XY else ScopeMode.YT) }
            )
            PillButton(
                text = "Fósforo ${persistenceLabel(persistence)}",
                color = CircuitColors.Emerald,
                onClick = { onPersistence((persistence + 1) % 4) }
            )
            PillButton(
                text = "Cursores",
                color = CircuitColors.Coral,
                onClick = {
                    onCursorMode(
                        when (cursorMode) {
                            CursorMode.NONE -> CursorMode.TIME
                            CursorMode.TIME -> CursorMode.VOLTAGE
                            CursorMode.VOLTAGE -> CursorMode.BOTH
                            CursorMode.BOTH -> CursorMode.NONE
                        }
                    )
                }
            )
            PillButton(text = "Medidas", color = CircuitColors.Cyan, onClick = onToggleMeasurements)
        }
    }
}

private fun persistenceLabel(value: Int): String = when (value) {
    0 -> "off"
    1 -> "bajo"
    2 -> "medio"
    else -> "alto"
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(CircuitColors.SurfaceVariant, CircuitShapes.SmallShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = CircuitColors.OnSurface, fontWeight = FontWeight.W700, fontSize = 16.sp)
    }
}

@Composable
private fun PillButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircuitShapes.PillShape)
            .border(1.dp, color.copy(alpha = 0.6f), CircuitShapes.PillShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.W700)
    }
}

// ===== Constantes =====

enum class CursorMode { NONE, TIME, VOLTAGE, BOTH }
enum class ScopeMode { YT, XY }

data class TimeBase(val label: String, val samples: Int, val totalSeconds: Float)

private val TIME_BASES = listOf(
    TimeBase("10µs", 600, 1e-5f),
    TimeBase("50µs", 600, 5e-5f),
    TimeBase("200µs", 600, 2e-4f),
    TimeBase("1ms", 600, 1e-3f),
    TimeBase("5ms", 600, 5e-3f),
    TimeBase("20ms", 600, 2e-2f),
    TimeBase("100ms", 600, 1e-1f),
    TimeBase("500ms", 600, 5e-1f),
    TimeBase("1s", 600, 1f),
    TimeBase("5s", 600, 5f)
)
