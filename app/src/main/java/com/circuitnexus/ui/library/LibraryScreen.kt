package com.circuitnexus.ui.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.model.ComponentCategory
import com.circuitnexus.model.ComponentType
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes

/**
 * Biblioteca de piezas: búsqueda, categorías y hoja de datos sencilla.
 * El usuario encuentra cualquier pieza sin saber cómo se llama técnicamente.
 */
@Composable
fun LibraryScreen(
    onPick: (ComponentType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ComponentCategory?>(null) }
    var detail by remember { mutableStateOf<ComponentType?>(null) }

    val all = ComponentType.PLACEABLE
    val filtered = all.filter { type ->
        (category == null || type.category == category) &&
            (query.isBlank() ||
                type.displayName.contains(query, ignoreCase = true) ||
                type.description.contains(query, ignoreCase = true) ||
                type.name.contains(query, ignoreCase = true))
    }

    Box(modifier = modifier.fillMaxSize().background(CircuitColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Título
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CircuitColors.Surface)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Biblioteca de piezas", color = CircuitColors.OnSurface, fontWeight = FontWeight.W700, fontSize = 17.sp)
                    Text("${filtered.size} piezas disponibles", color = CircuitColors.OnSurfaceVariant, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .background(CircuitColors.Coral.copy(alpha = 0.14f), CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onClose() } }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("CERRAR", color = CircuitColors.Coral, fontSize = 11.sp, fontWeight = FontWeight.W700)
                }
            }

            // Buscador
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 14.sp)
                    Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        if (query.isEmpty()) {
                            Text("Busca: led, motor, pantalla...", color = CircuitColors.OnSurfaceVariant, fontSize = 13.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(color = CircuitColors.OnSurface, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategoryChip("Todo", "🧰", category == null) { category = null }
                ComponentCategory.values().forEach { cat ->
                    CategoryChip(cat.title, cat.icon, category == cat) { category = cat }
                }
            }

            // Cuadrícula
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { type ->
                    ComponentCard(
                        type = type,
                        onAdd = { onPick(type) },
                        onInfo = { detail = type }
                    )
                }
            }
        }

        // Hoja de datos
        detail?.let { type ->
            DatasheetOverlay(
                type = type,
                onClose = { detail = null },
                onAdd = {
                    detail = null
                    onPick(type)
                }
            )
        }
    }
}

@Composable
private fun CategoryChip(text: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) CircuitColors.Emerald.copy(alpha = 0.2f) else CircuitColors.SurfaceVariant,
                CircuitShapes.PillShape
            )
            .border(
                1.dp,
                if (selected) CircuitColors.Emerald else CircuitColors.Outline,
                CircuitShapes.PillShape
            )
            .pointerInput(text) { detectTapGestures { onClick() } }
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 11.sp)
            Text(
                " $text",
                color = if (selected) CircuitColors.Emerald else CircuitColors.OnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.W700 else FontWeight.W500
            )
        }
    }
}

@Composable
private fun ComponentCard(type: ComponentType, onAdd: () -> Unit, onInfo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface, CircuitShapes.CardShape)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(categoryColor(type.category).copy(alpha = 0.14f), CircuitShapes.MediumShape),
            contentAlignment = Alignment.Center
        ) {
            Text(type.emoji, fontSize = 26.sp)
        }
        Text(
            type.displayName,
            color = CircuitColors.OnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            "${type.pinCount} puntos",
            color = CircuitColors.OnSurfaceVariant,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(CircuitColors.Emerald.copy(alpha = 0.18f), CircuitShapes.PillShape)
                    .pointerInput(type) { detectTapGestures { onAdd() } }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Añadir", color = CircuitColors.Emerald, fontSize = 10.sp, fontWeight = FontWeight.W700)
            }
            Box(
                modifier = Modifier
                    .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                    .pointerInput(type) { detectTapGestures { onInfo() } }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ℹ️", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DatasheetOverlay(
    type: ComponentType,
    onClose: () -> Unit,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(2.dp, categoryColor(type.category), CircuitShapes.CardShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(categoryColor(type.category).copy(alpha = 0.16f), CircuitShapes.MediumShape),
                    contentAlignment = Alignment.Center
                ) { Text(type.emoji, fontSize = 24.sp) }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(type.displayName, color = CircuitColors.OnSurface, fontWeight = FontWeight.W700, fontSize = 16.sp)
                    Text(
                        type.category.title,
                        color = categoryColor(type.category),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }

            Text(
                type.description,
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 12.sp
            )

            Text(
                "Puntos de conexión",
                color = CircuitColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700
            )
            type.pinNames().forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(categoryColor(type.category), CircleShape))
                    Text(
                        "  ${index + 1}. $name",
                        color = CircuitColors.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onClose() } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Volver", color = CircuitColors.OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.W600)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CircuitColors.Emerald, CircuitShapes.PillShape)
                        .pointerInput(Unit) { detectTapGestures { onAdd() } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Añadir a la mesa", color = CircuitColors.EmeraldOn, fontSize = 12.sp, fontWeight = FontWeight.W700)
                }
            }
        }
    }
}

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

private fun ComponentType.pinNames(): List<String> = com.circuitnexus.model.CircuitWire.getPinNames(this)
