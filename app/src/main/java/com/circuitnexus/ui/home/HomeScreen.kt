package com.circuitnexus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circuitnexus.model.Circuit
import com.circuitnexus.ui.common.CircuitCard
import com.circuitnexus.ui.common.CircuitTopAppBar
import com.circuitnexus.ui.common.EmptyState
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla "Mis Circuitos": lista de proyectos guardados.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewCircuit: (Long) -> Unit,
    onOpenCircuit: (Long) -> Unit
) {
    val circuits by viewModel.circuits.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            CircuitTopAppBar(
                title = "CircuitNexus",
                subtitle = if (circuits.isEmpty()) {
                    "● Listo para crear"
                } else {
                    "● ${circuits.size} circuito${if (circuits.size == 1) "" else "s"} guardado${if (circuits.size == 1) "" else "s"}"
                }
            )

            // Buscador + favoritos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍", fontSize = 13.sp)
                    Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                "Buscar circuitos…",
                                color = CircuitColors.OnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = viewModel::onSearchQueryChange,
                            textStyle = TextStyle(color = CircuitColors.OnSurface, fontSize = 12.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .background(
                            if (onlyFavorites) CircuitColors.Emerald.copy(alpha = 0.22f)
                            else CircuitColors.SurfaceVariant,
                            CircuitShapes.PillShape
                        )
                        .clickable { viewModel.toggleFavoritesFilter() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (onlyFavorites) "⭐ Favoritos" else "☆ Favoritos",
                        color = if (onlyFavorites) CircuitColors.Emerald else CircuitColors.OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }

            // Lista
            Box(modifier = Modifier.fillMaxSize()) {
                if (circuits.isEmpty()) {
                    EmptyState(
                        icon = if (query.isBlank()) "🧰" else "🔎",
                        title = if (query.isBlank()) "Aún no hay circuitos" else "Sin resultados",
                        message = if (query.isBlank()) {
                            "Tu mesa de trabajo está lista. ¡Crea tu primer circuito y enciende una luz!"
                        } else {
                            "Prueba con otro nombre."
                        },
                        actionText = if (query.isBlank()) "+ Empezar Nuevo Circuito" else null,
                        onAction = { viewModel.createNewCircuit(onNewCircuit) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                    ) {
                        items(circuits, key = { it.id }) { circuit ->
                            CircuitListItem(
                                circuit = circuit,
                                onOpen = { onOpenCircuit(circuit.id) },
                                onFavorite = { viewModel.toggleFavorite(circuit) },
                                onDuplicate = { viewModel.duplicateCircuit(circuit) },
                                onDelete = { viewModel.deleteCircuit(circuit) }
                            )
                        }
                    }
                }
            }
        }

        // Botón principal encima de la barra flotante
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (busy) CircuitColors.Emerald.copy(alpha = 0.5f) else CircuitColors.Emerald,
                        CircuitShapes.PillShape
                    )
                    .clickable(enabled = !busy) { viewModel.createNewCircuit(onNewCircuit) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.W800, color = CircuitColors.EmeraldOn)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (busy) "Creando…" else "Empezar Nuevo Circuito",
                    color = CircuitColors.EmeraldOn,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W800
                )
            }
        }
    }
}

@Composable
private fun CircuitListItem(
    circuit: Circuit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    CircuitCard(
        modifier = Modifier.fillMaxWidth(),
        title = circuit.name,
        subtitle = "Última edición: ${formatDate(circuit.updatedAt)}",
        onClick = onOpen,
        trailing = {
            Row {
                IconAction(if (circuit.isFavorite) "⭐" else "☆", onFavorite, CircuitColors.Amber)
                IconAction("📋", onDuplicate, CircuitColors.Cyan)
                IconAction("🗑", onDelete, CircuitColors.Coral)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Badge("Abrir para editar", CircuitColors.Emerald)
            if (circuit.description.isNotBlank()) {
                Badge(circuit.description.take(28), CircuitColors.Cyan)
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.13f), CircuitShapes.SmallShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.W600)
    }
}

@Composable
private fun IconAction(text: String, onClick: () -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color.copy(alpha = 0.10f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp)
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diffDays = (now.time - date.time) / (1000L * 60 * 60 * 24)
    return when {
        diffDays == 0L -> "hoy ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        diffDays == 1L -> "ayer"
        diffDays < 7 -> "hace $diffDays días"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}
