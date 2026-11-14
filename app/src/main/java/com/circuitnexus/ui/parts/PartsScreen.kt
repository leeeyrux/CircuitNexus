package com.circuitnexus.ui.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.ui.common.CircuitCard
import com.circuitnexus.ui.common.CircuitTopAppBar
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.viewmodel.PartsViewModel

/**
 * Catálogo de piezas por categorías, con descripciones en lenguaje cotidiano.
 */
@Composable
fun PartsScreen(viewModel: PartsViewModel) {
    val categories = listOf(
        viewModel.allParts.filter { it.category == PartsViewModel.PartCategory.POWER } to PartsViewModel.PartCategory.POWER,
        viewModel.allParts.filter { it.category == PartsViewModel.PartCategory.CONTROL } to PartsViewModel.PartCategory.CONTROL,
        viewModel.allParts.filter { it.category == PartsViewModel.PartCategory.OUTPUT } to PartsViewModel.PartCategory.OUTPUT,
        viewModel.allParts.filter { it.category == PartsViewModel.PartCategory.TIMING } to PartsViewModel.PartCategory.TIMING
    )

    Column(modifier = Modifier.fillMaxSize().background(CircuitColors.Background)) {
        CircuitTopAppBar(
            title = "Piezas",
            subtitle = "Conoce cada pieza y para qué sirve"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 130.dp)
        ) {
            categories.forEach { (parts, category) ->
                if (parts.isEmpty()) return@forEach
                item(key = "header-${category.name}") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.icon, fontSize = 18.sp)
                        Text(
                            category.title,
                            color = CircuitColors.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                items(parts, key = { it.type.name }) { part ->
                    CircuitCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = part.friendlyName,
                        subtitle = part.description
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(part.color), CircleShape)
                            )
                            Text(
                                "  ${part.type.pinCount} puntos de conexión",
                                color = CircuitColors.OnSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Box(modifier = Modifier.weight(1f))
                            Text(
                                part.type.emoji,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
