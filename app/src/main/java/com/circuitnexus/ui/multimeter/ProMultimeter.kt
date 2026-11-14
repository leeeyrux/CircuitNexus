package com.circuitnexus.ui.multimeter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

/**
 * Modos del multímetro, con nombres sencillos.
 */
enum class MeterMode(
    val short: String,
    val label: String,
    val icon: String,
    val color: Color
) {
    V_DC("VDC", "Voltaje continuo", "⎓", CircuitColors.Cyan),
    V_AC("VAC", "Voltaje alterno", "∼", CircuitColors.Cyan),
    A_DC("mA", "Corriente", "⇢", CircuitColors.Emerald),
    OHM("Ω", "Resistencia", "Ω", CircuitColors.Amber),
    CONT("•))", "Continuidad", "🔊", CircuitColors.Emerald),
    DIODE("▷|", "Diodo", "▷|", CircuitColors.Amber),
    FREQ("Hz", "Frecuencia", "〰", CircuitColors.Coral)
}

/**
 * Multímetro digital bonito, con pantalla tipo LCD, barra analógica,
 * gráfico de historial, HOLD, MIN/MAX y fijado.
 */
@Composable
fun ProMultimeter(
    voltage: Float,
    current: Float,
    wireId: Long,
    wireLabel: String,
    history: List<Float>,
    isPinned: Boolean,
    onPin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(MeterMode.V_DC) }
    var hold by remember { mutableStateOf(false) }
    var relative by remember { mutableStateOf(false) }
    var relativeOffset by remember { mutableStateOf(0f) }
    var minValue by remember { mutableStateOf<Float?>(null) }
    var maxValue by remember { mutableStateOf<Float?>(null) }

    val rawValue = when (mode) {
        MeterMode.V_DC -> voltage
        MeterMode.V_AC -> voltage * 0.707f
        MeterMode.A_DC -> current * 1000f
        MeterMode.OHM -> if (abs(current) > 1e-6f) abs(voltage / current) else Float.POSITIVE_INFINITY
        MeterMode.CONT -> if (abs(current) > 1e-6f) abs(voltage / current) else Float.POSITIVE_INFINITY
        MeterMode.DIODE -> voltage
        MeterMode.FREQ -> estimateFrequency(history)
    }

    val displayValue = when {
        hold -> relativeOffset
        relative -> rawValue - relativeOffset
        else -> rawValue
    }

    // Actualizar min/max
    if (!hold && displayValue.isFinite()) {
        minValue = minValue?.let { minOf(it, displayValue) } ?: displayValue
        maxValue = maxValue?.let { maxOf(it, displayValue) } ?: displayValue
    }

    val unit = modeUnit(mode, displayValue)
    val formatted = formatMeterValue(displayValue, mode)
    val continuity = mode == MeterMode.CONT && abs(displayValue) < 50f
    val accent = if (continuity) CircuitColors.Emerald else mode.color

    Column(
        modifier = modifier
            .width(300.dp)
            .background(Color(0xFF08090B), CircuitShapes.CardShape)
            .border(2.dp, if (isPinned) CircuitColors.Emerald else CircuitColors.Outline, CircuitShapes.CardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(accent, CircleShape)
                )
                Text(
                    "Multímetro",
                    color = CircuitColors.OnSurface,
                    fontWeight = FontWeight.W700,
                    fontSize = 13.sp
                )
            }
            Text(
                "Cable $wireId",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 9.sp
            )
        }

        // Pantalla LCD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(Color(0xFF02140A), CircuitShapes.MediumShape)
                .border(1.dp, accent.copy(alpha = 0.45f), CircuitShapes.MediumShape)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatted,
                        color = accent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W700,
                        fontSize = 34.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = " $unit",
                        color = accent.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W600,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mode.short, color = accent.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.W700)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hold) StatusTag("HOLD", CircuitColors.Cyan)
                        if (relative) StatusTag("REL", CircuitColors.Amber)
                        if (continuity) StatusTag("BEEP", CircuitColors.Emerald)
                        if (isPinned) StatusTag("📌", CircuitColors.Emerald)
                    }
                }
            }
        }

        // Barra analógica
        AnalogBar(value = displayValue, mode = mode, accent = accent, continuity = continuity)

        // Historial
        if (history.size >= 2) {
            Sparkline(values = history, color = accent)
        }

        // Min / Max
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatBox(
                label = "MIN",
                value = minValue?.let { formatMeterValue(it, mode) + unit } ?: "—",
                color = CircuitColors.Coral,
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "MAX",
                value = maxValue?.let { formatMeterValue(it, mode) + unit } ?: "—",
                color = CircuitColors.Emerald,
                modifier = Modifier.weight(1f)
            )
        }

        // Botones de función
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            FunctionButton("HOLD", hold, CircuitColors.Cyan) {
                if (!hold) relativeOffset = displayValue
                hold = !hold
            }
            FunctionButton("REL", relative, CircuitColors.Amber) {
                relative = !relative
                if (relative) relativeOffset = displayValue
            }
            FunctionButton("MIN/MAX", minValue != null, CircuitColors.Emerald) {
                minValue = null
                maxValue = null
            }
            FunctionButton(if (isPinned) "QUITAR" else "FIJAR", isPinned, CircuitColors.Emerald) {
                if (isPinned) onDismiss() else onPin()
            }
        }

        // Selector de modo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MeterMode.values().forEach { candidate ->
                val selected = candidate == mode
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) candidate.color.copy(alpha = 0.22f) else CircuitColors.SurfaceVariant,
                            CircuitShapes.PillShape
                        )
                        .border(
                            1.dp,
                            if (selected) candidate.color else CircuitColors.Outline,
                            CircuitShapes.PillShape
                        )
                        .pointerInput(candidate) {
                            detectTapGestures {
                                mode = candidate
                                hold = false
                                relative = false
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        candidate.short,
                        color = if (selected) candidate.color else CircuitColors.OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }

        // Cerrar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Coral.copy(alpha = 0.12f), CircuitShapes.PillShape)
                .border(1.dp, CircuitColors.Coral.copy(alpha = 0.5f), CircuitShapes.PillShape)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("CERRAR", color = CircuitColors.Coral, fontSize = 10.sp, fontWeight = FontWeight.W700)
        }
    }
}

// ===== Subcomponentes =====

@Composable
private fun StatusTag(text: String, color: Color) {
    Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.W800)
}

@Composable
private fun AnalogBar(value: Float, mode: MeterMode, accent: Color, continuity: Boolean) {
    val range = modeRange(mode)
    val normalized = if (value.isFinite() && range > 0f) {
        ((value + range) / (2f * range)).coerceIn(0f, 1f)
    } else 0f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val w = size.width
        val h = size.height
        drawRect(color = CircuitColors.SurfaceVariant, size = androidx.compose.ui.geometry.Size(w, h))
        // Marcas
        for (i in 0..10) {
            val x = w * i / 10f
            drawLine(
                color = CircuitColors.OnSurfaceVariant.copy(alpha = 0.35f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
        }
        // Relleno
        drawRect(
            color = accent.copy(alpha = 0.85f),
            size = androidx.compose.ui.geometry.Size(w * normalized, h)
        )
        if (continuity) {
            drawCircle(color = CircuitColors.Emerald, radius = h / 2.5f, center = Offset(w - 8f, h / 2f))
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val finite = values.filter { it.isFinite() }
        if (finite.size < 2) return@Canvas
        var minV = finite.min()
        var maxV = finite.max()
        if (maxV - minV < 1e-9f) {
            maxV = minV + 1f
        }
        val path = Path()
        finite.forEachIndexed { index, value ->
            val x = w * index / (finite.size - 1).toFloat()
            val y = h - ((value - minV) / (maxV - minV)) * h
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.6f))
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), CircuitShapes.SmallShape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.W700)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.W700, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FunctionButton(
    label: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .background(
                if (active) color.copy(alpha = 0.25f) else CircuitColors.SurfaceVariant,
                CircuitShapes.SmallShape
            )
            .border(
                1.dp,
                if (active) color else CircuitColors.Outline,
                CircuitShapes.SmallShape
            )
            .pointerInput(label) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) color else CircuitColors.OnSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.W700,
            textAlign = TextAlign.Center
        )
    }
}

// ===== Utilidades =====

private fun modeUnit(mode: MeterMode, value: Float): String {
    val a = if (value.isFinite()) abs(value) else 0f
    return when (mode) {
        MeterMode.V_DC, MeterMode.V_AC -> "V"
        MeterMode.A_DC -> if (a >= 1f) "A" else "mA"
        MeterMode.OHM, MeterMode.CONT -> when {
            a >= 1e6f -> "MΩ"
            a >= 1e3f -> "kΩ"
            else -> "Ω"
        }
        MeterMode.DIODE -> "V"
        MeterMode.FREQ -> when {
            a >= 1e6f -> "MHz"
            a >= 1e3f -> "kHz"
            else -> "Hz"
        }
    }
}

private fun formatMeterValue(value: Float, mode: MeterMode): String {
    if (!value.isFinite()) return "OL"
    if (value.isNaN()) return "----"
    val a = abs(value)
    return when (mode) {
        MeterMode.OHM, MeterMode.CONT -> when {
            a >= 1e6f -> "%.2f".format(value / 1e6f)
            a >= 1e3f -> "%.2f".format(value / 1e3f)
            else -> "%.1f".format(value)
        }
        MeterMode.FREQ -> when {
            a >= 1e6f -> "%.2f".format(value / 1e6f)
            a >= 1e3f -> "%.2f".format(value / 1e3f)
            else -> "%.1f".format(value)
        }
        MeterMode.A_DC -> if (a >= 1f) "%.3f".format(value) else "%.1f".format(value)
        else -> "%.2f".format(value)
    }
}

private fun modeRange(mode: MeterMode): Float = when (mode) {
    MeterMode.V_DC, MeterMode.V_AC -> 12f
    MeterMode.A_DC -> 500f
    MeterMode.OHM, MeterMode.CONT -> 10000f
    MeterMode.DIODE -> 2f
    MeterMode.FREQ -> 1000f
}

/** Estimación simple de frecuencia a partir del historial de valores */
fun estimateFrequency(values: List<Float>): Float {
    if (values.size < 8) return 0f
    val finite = values.filter { it.isFinite() }
    if (finite.size < 8) return 0f
    val avg = finite.average().toFloat()
    var crossings = 0
    var lastAbove = finite[0] > avg
    for (i in 1 until finite.size) {
        val above = finite[i] > avg
        if (above && !lastAbove) crossings++
        lastAbove = above
    }
    // El historial representa muestras a ritmo de simulación; estimación aproximada
    return (crossings * 10f).coerceAtMost(99_999f)
}
