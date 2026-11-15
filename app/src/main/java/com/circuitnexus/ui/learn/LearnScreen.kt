package com.circuitnexus.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.ui.common.ButtonVariant
import com.circuitnexus.ui.common.CircuitButton
import com.circuitnexus.ui.common.CircuitCard
import com.circuitnexus.ui.common.CircuitTopAppBar
import com.circuitnexus.ui.common.EmptyState
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.viewmodel.LearnViewModel

@Composable
fun LearnScreen(viewModel: LearnViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val completedLessons by viewModel.completedLessons.collectAsStateWithLifecycle()
    val lessons = viewModel.lessonsForCategory[selectedCategory] ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CircuitTopAppBar(
            title = "Aprender",
            subtitle = "Elige un tema para empezar",
            statusText = "● ${completedLessons.size} lecciones completadas"
        )

        // Category Tabs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CircuitColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.lessonsForCategory.keys.forEach { category ->
                    CategoryTab(
                        category = category,
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }
        }

        // Lessons List
        Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background)) {
            if (lessons.isEmpty()) {
                EmptyState(
                    icon = "📚",
                    title = "Próximamente",
                    message = "Estamos preparando más lecciones para ti"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 0.dp, 0.dp, 24.dp)
                ) {
                    items(lessons) { lesson ->
                        val completed = completedLessons.contains(lesson.id)
                        LessonCard(
                            lesson = lesson,
                            completed = completed,
                            onClick = { viewModel.markLessonComplete(lesson.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTab(
    category: com.circuitnexus.viewmodel.LearnCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .background(
                if (selected) CircuitColors.EmeraldContainer else CircuitColors.SurfaceVariant,
                CircuitShapes.PillShape
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) CircuitColors.Emerald else CircuitColors.Outline,
                CircuitShapes.PillShape
            )
            .combinedClickable(onClick = onClick, onLongClick = null, interactionSource = interactionSource)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.icon, fontSize = 18.sp)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.title,
                color = if (selected) CircuitColors.Emerald else CircuitColors.OnSurfaceVariant,
                fontWeight = if (selected) FontWeight.W600 else FontWeight.Normal,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun LessonCard(
    lesson: com.circuitnexus.viewmodel.Lesson,
    completed: Boolean,
    onClick: () -> Unit
) {
    CircuitCard(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (completed) CircuitColors.EmeraldContainer.copy(alpha = 0.3f) else CircuitColors.Surface,
                CircuitShapes.CardShape
            )
            .border(
                if (completed) 2.dp else 1.dp,
                if (completed) CircuitColors.Emerald else CircuitColors.Outline,
                CircuitShapes.CardShape
            ),
        title = lesson.title,
        subtitle = lesson.description,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (completed) CircuitColors.Emerald else CircuitColors.SurfaceVariant,
                            CircuitShapes.CircleShape
                        )
                ) {
                    if (completed) {
                        Text(text = "✓", color = CircuitColors.EmeraldOn, fontSize = 14.sp, fontWeight = FontWeight.W700, modifier = Modifier.fillMaxSize().padding(4.dp))
                    } else {
                        Text(text = "${lesson.order}", color = CircuitColors.OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.W600, modifier = Modifier.fillMaxSize().padding(4.dp))
                    }
                }
                Column {
                    Text(
                        text = (if (completed) "Completada ✓" else "Pendiente"),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = (if (completed) CircuitColors.Emerald else CircuitColors.OnSurfaceVariant)
                    )
                }
            }
            if (!completed) {
                CircuitButton(
                    text = "Empezar",
                    onClick = onClick,
                    variant = ButtonVariant.PRIMARY,
                    fullWidth = false
                )
            }
        }
    }
}
