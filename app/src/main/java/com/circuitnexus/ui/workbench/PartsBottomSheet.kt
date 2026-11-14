package com.circuitnexus.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.model.ComponentCategory
import com.circuitnexus.model.ComponentType
import com.circuitnexus.templates.CircuitTemplate
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.util.HapticFeedback

/**
 * Panel de piezas deslizable (Bottom Sheet).
 *
 * - Se desliza desde abajo con esquinas superiores de 30 dp.
 * - Fondo carbón mate con vidrio sutil y borde superior fino.
 * - Píldora de arrastre visible.
 * - Tocar o sacar una pieza la coloca al instante en la mesa.
 * - Ajustes rápidos (resistencia, color de LED, voltaje, pulso) sin salir del panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsBottomSheet(
    worldX: Float,
    worldY: Float,
    templates: List<CircuitTemplate>,
    draftResistance: Float,
    draftLedColor: String,
    draftBatteryVolts: Float,
    draftTimerFrequency: Float,
    draftCapacitance: Float,
    onResistance: (Float) -> Unit,
    onLedColor: (String) -> Unit,
    onBatteryVolts: (Float) -> Unit,
    onTimerFrequency: (Float) -> Unit,
    onCapacitance: (Float) -> Unit,
    onAdd: (ComponentType) -> Unit,
    onTemplate: (CircuitTemplate) -> Unit,
    onOpenLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = HapticFeedback.rememberHaptic()
    var tab by remember { mutableStateOf(SheetTab.PARTS) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface.copy(alpha = 0.98f))
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Borde superior fino + píldora de arrastre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CircuitColors.Outline)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(5.dp)
                    .background(CircuitColors.OnSurfaceVariant.copy(alpha = 0.45f), CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Encabezado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Piezas",
                        color = CircuitColors.OnSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.W700
                    )
                    Text(
                        "Desliza la hoja • Toca una pieza para añadirla a la mesa",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                CircleAction("✕") {
                    haptic.lightClick()
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pestañas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetTabButton("🧩  Piezas", tab == SheetTab.PARTS) {
                    haptic.lightClick()
                    tab = SheetTab.PARTS
                }
                SheetTabButton("📋  Plantillas", tab == SheetTab.TEMPLATES) {
                    haptic.lightClick()
                    tab = SheetTab.TEMPLATES
                }
                Spacer(modifier = Modifier.weight(1f))
                SheetTabButton("📚  Biblioteca", false) {
                    haptic.lightClick()
                    onOpenLibrary()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tab == SheetTab.PARTS) {
                PartsTabContent(
                    worldX = worldX,
                    worldY = worldY,
                    draftResistance = draftResistance,
                    draftLedColor = draftLedColor,
                    draftBatteryVolts = draftBatteryVolts,
                    draftTimerFrequency = draftTimerFrequency,
                    draftCapacitance = draftCapacitance,
                    onResistance = onResistance,
                    onLedColor = onLedColor,
                    onBatteryVolts = onBatteryVolts,
                    onTimerFrequency = onTimerFrequency,
                    onCapacitance = onCapacitance,
                    onAdd = onAdd,
                    onDismiss = onDismiss,
                    onOpenLibrary = onOpenLibrary
                )
            } else {
                TemplatesTabContent(templates = templates, onTemplate = onTemplate)
            }
        }
    }
}

private enum class SheetTab { PARTS, TEMPLATES }

@Composable
private fun SheetTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) CircuitColors.Emerald.copy(alpha = 0.18f)
                else CircuitColors.SurfaceVariant,
                CircuitShapes.PillShape
            )
            .border(
                1.dp,
                if (selected) CircuitColors.Emerald.copy(alpha = 0.7f) else CircuitColors.Outline,
                CircuitShapes.PillShape
            )
            .pointerInput(text) { detectTapGestures { onClick() } }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            color = if (selected) CircuitColors.Emerald else CircuitColors.OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.W700
        )
    }
}

@Composable
private fun CircleAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(CircuitColors.SurfaceVariant, CircleShape)
            .pointerInput(text) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = CircuitColors.OnSurfaceVariant, fontSize = 13.sp)
    }
}

// ================= Pestaña de piezas =================

@Composable
private fun PartsTabContent(
    worldX: Float,
    worldY: Float,
    draftResistance: Float,
    draftLedColor: String,
    draftBatteryVolts: Float,
    draftTimerFrequency: Float,
    draftCapacitance: Float,
    onResistance: (Float) -> Unit,
    onLedColor: (String) -> Unit,
    onBatteryVolts: (Float) -> Unit,
    onTimerFrequency: (Float) -> Unit,
    onCapacitance: (Float) -> Unit,
    onAdd: (ComponentType) -> Unit,
    onDismiss: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val haptic = HapticFeedback.rememberHaptic()

    Column(modifier = Modifier.fillMaxWidth()) {

        // ---- Ajustes rápidos ----
        Text(
            "Ajustes rápidos",
            color = CircuitColors.OnSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickControlCard("Resistencia", valueLabel(draftResistance, "Ω"), CircuitColors.Amber) {
                Slider(
                    value = draftResistance,
                    onValueChange = onResistance,
                    valueRange = 10f..10_000f,
                    modifier = Modifier.width(150.dp).height(26.dp),
                    colors = sliderColors(CircuitColors.Amber)
                )
            }
            QuickControlCard("Batería", "${"%.1f".format(draftBatteryVolts)} V", CircuitColors.Cyan) {
                Slider(
                    value = draftBatteryVolts,
                    onValueChange = onBatteryVolts,
                    valueRange = 1.5f..24f,
                    modifier = Modifier.width(150.dp).height(26.dp),
                    colors = sliderColors(CircuitColors.Cyan)
                )
            }
            QuickControlCard("Pulso del 555", "${"%.1f".format(draftTimerFrequency)} Hz", CircuitColors.Coral) {
                Slider(
                    value = draftTimerFrequency,
                    onValueChange = onTimerFrequency,
                    valueRange = 0.1f..20f,
                    modifier = Modifier.width(150.dp).height(26.dp),
                    colors = sliderColors(CircuitColors.Coral)
                )
            }
            QuickControlCard("Capacitor", "${"%.0f".format(draftCapacitance)} µF", CircuitColors.Emerald) {
                Slider(
                    value = draftCapacitance,
                    onValueChange = onCapacitance,
                    valueRange = 1f..1000f,
                    modifier = Modifier.width(150.dp).height(26.dp),
                    colors = sliderColors(CircuitColors.Emerald)
                )
            }
            QuickControlCard("Color del LED", colorLabel(draftLedColor), CircuitColors.Emerald) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ledColorOptions.forEach { (key, emoji) ->
                        val selected = key == draftLedColor
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    if (selected) CircuitColors.Emerald.copy(alpha = 0.28f)
                                    else CircuitColors.Surface,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (selected) CircuitColors.Emerald else CircuitColors.Outline,
                                    CircleShape
                                )
                                .pointerInput(key) {
                                    detectTapGestures {
                                        haptic.lightClick()
                                        onLedColor(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Piezas esenciales ----
        Text(
            "Esenciales",
            color = CircuitColors.OnSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        val essentials = listOf(
            ComponentType.BATTERY to valueLabel(draftBatteryVolts, "V"),
            ComponentType.GROUND to "Conductor base",
            ComponentType.RESISTOR to valueLabel(draftResistance, "Ω"),
            ComponentType.LED to colorLabel(draftLedColor),
            ComponentType.SWITCH to "Abre y cierra",
            ComponentType.TIMER_555 to "${"%.1f".format(draftTimerFrequency)} Hz"
        )

        // Dos columnas de cápsulas
        essentials.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (type, preview) ->
                    PartCapsule(
                        type = type,
                        preview = preview,
                        modifier = Modifier.weight(1f),
                        onPlace = {
                            onAdd(type)
                            haptic.mediumClick()
                            onDismiss()
                        }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---- Más piezas ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PartCapsule(
                type = ComponentType.CAPACITOR,
                preview = "${"%.0f".format(draftCapacitance)} µF",
                modifier = Modifier.weight(1f),
                onPlace = { onAdd(ComponentType.CAPACITOR); haptic.mediumClick() }
            )
            PartCapsule(
                type = ComponentType.POTENTIOMETER,
                preview = "Perilla reguladora",
                modifier = Modifier.weight(1f),
                onPlace = { onAdd(ComponentType.POTENTIOMETER); haptic.mediumClick() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PartCapsule(
                type = ComponentType.LCD_16X2,
                preview = "Pantalla de texto",
                modifier = Modifier.weight(1f),
                onPlace = { onAdd(ComponentType.LCD_16X2); haptic.mediumClick() }
            )
            PartCapsule(
                type = ComponentType.DISPLAY_7SEG_4,
                preview = "Números de 4 dígitos",
                modifier = Modifier.weight(1f),
                onPlace = { onAdd(ComponentType.DISPLAY_7SEG_4); haptic.mediumClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Biblioteca completa ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .background(CircuitColors.Emerald.copy(alpha = 0.14f), CircuitShapes.PillShape)
                .border(1.dp, CircuitColors.Emerald.copy(alpha = 0.6f), CircuitShapes.PillShape)
                .pointerInput(Unit) {
                    detectTapGestures {
                        haptic.lightClick()
                        onOpenLibrary()
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "📚  Ver todas las piezas (50+)",
                color = CircuitColors.Emerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun QuickControlCard(
    title: String,
    value: String,
    accent: Color,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .background(CircuitColors.SurfaceVariant.copy(alpha = 0.85f), CircuitShapes.CardShape)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accent, CircleShape)
            )
            Text(
                "  $title",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.W600
            )
        }
        Text(
            value,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700
        )
        control()
    }
}

/**
 * Cápsula de pieza: se puede tocar o arrastrar hacia arriba para sacarla a la mesa.
 */
@Composable
private fun PartCapsule(
    type: ComponentType,
    preview: String,
    modifier: Modifier = Modifier,
    onPlace: () -> Unit
) {
    val haptic = HapticFeedback.rememberHaptic()
    val density = LocalDensity.current.density
    val accent = categoryColor(type.category)
    var dragY by remember { mutableStateOf(0f) }
    var dragX by remember { mutableStateOf(0f) }
    var lifted by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .offset(x = (dragX / density).dp, y = (dragY / density).dp)
            .background(
                if (lifted) CircuitColors.Surface else CircuitColors.Surface.copy(alpha = 0.96f),
                CircuitShapes.CardShape
            )
            .border(
                1.dp,
                if (lifted) accent.copy(alpha = 0.8f) else CircuitColors.Outline,
                CircuitShapes.CardShape
            )
            .pointerInput(type) {
                detectDragGestures(
                    onDragStart = {
                        lifted = true
                        haptic.dragStart()
                    },
                    onDragEnd = {
                        lifted = false
                        if (dragY < -70f) {
                            onPlace()
                        }
                        dragY = 0f
                        dragX = 0f
                    },
                    onDragCancel = {
                        lifted = false
                        dragY = 0f
                        dragX = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragY += dragAmount.y
                    dragX += dragAmount.x
                }
            }
            .pointerInput(type) {
                detectTapGestures { onPlace() }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(type.emoji, fontSize = 19.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                type.displayName,
                color = CircuitColors.OnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.W700,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                preview,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = accent, fontSize = 15.sp, fontWeight = FontWeight.W800)
        }
    }
}

// ================= Pestaña de plantillas =================

@Composable
private fun TemplatesTabContent(
    templates: List<CircuitTemplate>,
    onTemplate: (CircuitTemplate) -> Unit
) {
    val haptic = HapticFeedback.rememberHaptic()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Circuitos listos para aprender",
            color = CircuitColors.OnSurfaceVariant,
            fontSize = 10.sp
        )
        templates.take(6).forEach { template ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CircuitColors.SurfaceVariant.copy(alpha = 0.75f), CircuitShapes.CardShape)
                    .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
                    .pointerInput(template.id) {
                        detectTapGestures {
                            haptic.mediumClick()
                            onTemplate(template)
                        }
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CircuitColors.Emerald.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(template.thumbnailEmoji, fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        template.name,
                        color = CircuitColors.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${template.estimatedTimeMinutes} min · ${template.difficulty.label}",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                Text("Cargar", color = CircuitColors.Emerald, fontSize = 10.sp, fontWeight = FontWeight.W700)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ================= Utilidades visuales =================

private val ledColorOptions = listOf(
    "green" to "🟢",
    "red" to "🔴",
    "blue" to "🔵",
    "yellow" to "🟡",
    "white" to "⚪"
)

private fun categoryColor(category: ComponentCategory): Color = when (category) {
    ComponentCategory.POWER -> CircuitColors.Amber
    ComponentCategory.PASSIVE -> CircuitColors.Cyan
    ComponentCategory.SEMICONDUCTOR -> CircuitColors.Coral
    ComponentCategory.CONTROL -> CircuitColors.Amber
    ComponentCategory.IC -> CircuitColors.Cyan
    ComponentCategory.OUTPUT -> CircuitColors.Emerald
    ComponentCategory.SENSOR -> CircuitColors.Cyan
    ComponentCategory.DISPLAY -> CircuitColors.Emerald
    ComponentCategory.CONNECTOR -> CircuitColors.OnSurfaceVariant
    ComponentCategory.WIRE -> CircuitColors.Emerald
}

private fun colorLabel(key: String): String = when (key) {
    "green" -> "Verde"
    "red" -> "Rojo"
    "blue" -> "Azul"
    "yellow" -> "Amarillo"
    "white" -> "Blanco"
    else -> key
}

private fun valueLabel(value: Float, unit: String): String = when {
    value >= 1000f -> "${"%.1f".format(value / 1000f)} k$unit"
    value >= 10f -> "${"%.0f".format(value)} $unit"
    else -> "${"%.1f".format(value)} $unit"
}

@Composable
private fun sliderColors(accent: Color) = SliderDefaults.colors(
    thumbColor = accent,
    activeTrackColor = accent,
    inactiveTrackColor = CircuitColors.Outline
)
