package com.circuitnexus.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.engine.ScratchEngine
import com.circuitnexus.model.ComponentType
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes

@Composable
fun ScratchPanel(
    engine: ScratchEngine,
    onClose: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val selectedBlockType by remember { mutableStateOf<ScratchEngine.BlockType?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(CircuitColors.Surface)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧩 Bloques Scratch", color = CircuitColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.W700)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.isRunning) {
                        Text("▶️ Ejecutando", color = CircuitColors.Emerald, fontSize = 10.sp, fontWeight = FontWeight.W600)
                    } else {
                        Text("⏸ Detenido", color = CircuitColors.Amber, fontSize = 10.sp, fontWeight = FontWeight.W600)
                    }
                    androidx.compose.material3.IconButton(onClick = { if (state.isRunning) engine.stop() else engine.start() }) {
                        Text(if (state.isRunning) "⏸" else "▶️", fontSize = 16.sp)
                    }
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Text("✕", color = CircuitColors.OnSurfaceVariant, fontSize = 16.sp)
                    }
                }
            }

            // Paleta de bloques
            Text("Arrastra bloques al área de trabajo", color = CircuitColors.OnSurfaceVariant, fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp, 0.dp)
            ) {
                items(ScratchBlockPalette.ALL_BLOCKS) { block ->
                    ScratchBlockChip(block = block, onDragStart = { selectedBlockType = block.type })
                }
            }

            // Área de trabajo (simplificada)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(CircuitColors.Background, CircuitShapes.CardShape)
                    .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
                    .padding(8.dp)
            ) {
                Text(
                    "Área de trabajo - Arrastra bloques aquí",
                    color = CircuitColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

object ScratchBlockPalette {
    val ALL_BLOCKS = listOf(
        ScratchBlockInfo("🟢 Inicio", ScratchEngine.BlockType.EVENT_START, CircuitColors.Emerald),
        ScratchBlockInfo("🔄 Repetir", ScratchEngine.BlockType.LOOP_REPEAT, CircuitColors.Cyan),
        ScratchBlockInfo("🔀 Si...", ScratchEngine.BlockType.IF_THEN, CircuitColors.Amber),
        ScratchBlockInfo("⏱️ Esperar", ScratchEngine.BlockType.WAIT, CircuitColors.Amber),
        ScratchBlockInfo("📌 Pin ON/OFF", ScratchEngine.BlockType.SET_PIN, CircuitColors.Coral),
        ScratchBlockInfo("📖 Leer Pin", ScratchEngine.BlockType.READ_PIN, CircuitColors.Cyan),
        ScratchBlockInfo("➕ Matemáticas", ScratchEngine.BlockType.MATH_OP, CircuitColors.Coral),
    )
}

data class ScratchBlockInfo(
    val label: String,
    val type: ScratchEngine.BlockType,
    val color: Color
)

@Composable
fun ScratchBlockChip(block: ScratchBlockInfo, onDragStart: (ScratchEngine.BlockType) -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(90.dp)
            .height(50.dp)
            .background(
                if (isPressed) block.color.copy(alpha = 0.3f) else block.color.copy(alpha = 0.15f),
                CircuitShapes.CardShape
            )
            .border(
                1.dp,
                if (isPressed) block.color else CircuitColors.Outline,
                CircuitShapes.CardShape
            )
            .pointerInput(block.type) {
                androidx.compose.foundation.gestures.detectDragGestures(
                    onDragStart = { onDragStart(block.type) }
                )
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(block.label, color = block.color, fontSize = 9.sp, fontWeight = FontWeight.W600, textAlign = TextAlign.Center, maxLines = 2)
    }
}

