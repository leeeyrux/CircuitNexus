package com.circuitnexus.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitnexus.engine.HelpCard
import com.circuitnexus.engine.HelpType
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes

enum class ButtonVariant { PRIMARY, SECONDARY, DANGER, AMBER, CYAN }

/**
 * Tarjeta carbón mate con esquinas amplias. Base visual de toda la app.
 */
@Composable
fun CircuitCard(
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CircuitColors.Surface, CircuitShapes.CardShape)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.CardShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CircuitColors.OnSurface,
                    fontWeight = FontWeight.W600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                trailing?.invoke()
            }
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CircuitColors.OnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        content()
    }
}

/**
 * Botón píldora. El principal es verde esmeralda con texto oscuro.
 */
@Composable
fun CircuitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    fullWidth: Boolean = false,
    icon: String? = null
) {
    val container = when (variant) {
        ButtonVariant.PRIMARY -> CircuitColors.Emerald
        ButtonVariant.SECONDARY -> CircuitColors.SurfaceVariant
        ButtonVariant.DANGER -> CircuitColors.Coral
        ButtonVariant.AMBER -> CircuitColors.Amber
        ButtonVariant.CYAN -> CircuitColors.Cyan
    }
    val content = when (variant) {
        ButtonVariant.PRIMARY -> CircuitColors.EmeraldOn
        ButtonVariant.SECONDARY -> CircuitColors.OnSurface
        ButtonVariant.DANGER -> CircuitColors.CoralOn
        ButtonVariant.AMBER -> CircuitColors.AmberOn
        ButtonVariant.CYAN -> CircuitColors.CyanOn
    }
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(48.dp)
            .background(container.copy(alpha = alpha), CircuitShapes.PillShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Text(icon, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = content.copy(alpha = alpha),
            fontWeight = FontWeight.W700
        )
    }
}

/**
 * Botón redondo compacto para acciones de icono.
 */
@Composable
fun CircuitIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                if (enabled) CircuitColors.SurfaceVariant else CircuitColors.Surface,
                CircleShape
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Encabezado centrado con título blanco y estado amigable.
 */
@Composable
fun CircuitTopAppBar(
    title: String,
    subtitle: String? = null,
    showStatus: Boolean = true,
    statusText: String = "● Listo para crear",
    statusColor: Color = CircuitColors.Emerald,
    actions: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = CircuitColors.OnBackground,
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (actions != null) actions() else Spacer(modifier = Modifier.width(40.dp))
        }
        if (showStatus) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.W500
                    )
                } else {
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }
        }
    }
}

/**
 * Tarjeta de ayuda (consejos, advertencias, peligros).
 */
@Composable
fun HelpCardView(card: HelpCard, onDismiss: (() -> Unit)? = null) {
    val accent = when (card.type) {
        HelpType.WELCOME -> CircuitColors.Emerald
        HelpType.TIP -> CircuitColors.Cyan
        HelpType.CAUTION -> CircuitColors.Amber
        HelpType.DANGER -> CircuitColors.Coral
        HelpType.INFO -> CircuitColors.Cyan
    }
    val icon = when (card.type) {
        HelpType.WELCOME -> "✨"
        HelpType.TIP -> "💡"
        HelpType.CAUTION -> "⚠️"
        HelpType.DANGER -> "🚨"
        HelpType.INFO -> "ℹ️"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.10f), CircuitShapes.CardShape)
            .border(1.dp, accent.copy(alpha = 0.55f), CircuitShapes.CardShape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(card.title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.W700)
            Text(card.message, color = CircuitColors.OnSurface, fontSize = 11.sp)
            if (card.actionText.isNotBlank()) {
                Text(
                    "→ ${card.actionText}",
                    color = CircuitColors.OnSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        if (onDismiss != null) {
            Text(
                "✕",
                color = CircuitColors.OnSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    }
}

/**
 * Estado vacío amigable.
 */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 56.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = CircuitColors.OnSurface,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = CircuitColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null) {
            Spacer(modifier = Modifier.height(18.dp))
            CircuitButton(text = actionText, onClick = { onAction?.invoke() })
        }
    }
}

/**
 * Diálogo de carga simple.
 */
@Composable
fun LoadingOverlay(message: String = "Procesando…") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CircuitColors.Background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CircuitColors.Emerald, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, color = CircuitColors.OnSurface, fontSize = 12.sp)
        }
    }
}

/** Espaciador vertical corto reutilizable */
@Composable
fun VSpace(height: Int = 8) = Spacer(modifier = Modifier.height(height.dp))

/** Espaciador horizontal reutilizable */
@Composable
fun HSpace(width: Int = 8) = Spacer(modifier = Modifier.width(width.dp))

@Suppress("unused")
private val unusedPadding = PaddingValues(0.dp)
