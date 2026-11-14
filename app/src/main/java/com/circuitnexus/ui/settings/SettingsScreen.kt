package com.circuitnexus.ui.settings

import kotlin.math.roundToInt

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
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val apiKeyValid by viewModel.apiKeyValid.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val testingConnection by viewModel.testingConnection.collectAsStateWithLifecycle()
    val showGrid by viewModel.showGrid.collectAsStateWithLifecycle()
    val snapToGrid by viewModel.snapToGrid.collectAsStateWithLifecycle()
    val gridSize by viewModel.gridSize.collectAsStateWithLifecycle()
    val autoSimulate by viewModel.autoSimulate.collectAsStateWithLifecycle()
    val hapticFeedback by viewModel.hapticFeedback.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CircuitTopAppBar(
            title = "Ajustes",
            subtitle = "Personaliza tu experiencia",
            statusText = "● Todo listo"
        )

        Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background)) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 0.dp, 0.dp, 24.dp)
            ) {
                // AI Assistant Section
                item {
                    AISettingsSection(
                        apiKey = apiKey,
                        apiKeyValid = apiKeyValid,
                        selectedProvider = selectedProvider,
                        selectedModel = selectedModel,
                        testingConnection = testingConnection,
                        availableModels = viewModel.availableModels,
                        providerDisplayNames = viewModel.providerDisplayNames,
                        onApiKeyChange = viewModel::onApiKeyChange,
                        onSaveApiKey = viewModel::saveApiKey,
                        onClearApiKey = viewModel::clearApiKey,
                        onProviderChange = viewModel::onProviderChange,
                        onModelChange = viewModel::onModelChange,
                        onTestConnection = viewModel::testConnection
                    )
                }

                // Workbench Settings
                item {
                    WorkbenchSettingsSection(
                        showGrid = showGrid,
                        snapToGrid = snapToGrid,
                        gridSize = gridSize,
                        autoSimulate = autoSimulate,
                        onShowGridChange = viewModel::onShowGridChange,
                        onSnapToGridChange = viewModel::onSnapToGridChange,
                        onGridSizeChange = viewModel::onGridSizeChange,
                        onAutoSimulateChange = viewModel::onAutoSimulateChange
                    )
                }

                // Feedback Settings
                item {
                    FeedbackSettingsSection(
                        hapticFeedback = hapticFeedback,
                        onHapticFeedbackChange = viewModel::onHapticFeedbackChange
                    )
                }

                // About Section
                item {
                    AboutSection()
                }
            }
        }
    }
}

@Composable
fun AISettingsSection(
    apiKey: String,
    apiKeyValid: Boolean?,
    selectedProvider: String,
    selectedModel: String,
    testingConnection: Boolean,
    availableModels: Map<String, List<String>>,
    providerDisplayNames: Map<String, String>,
    onApiKeyChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: () -> Unit
) {
    CircuitCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 24.sp)
                    Text(
                        text = "Asistente Inteligente (Opcional)",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = CircuitColors.OnSurface,
                        fontWeight = FontWeight.W600
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Conecta tu propia clave API para que la IA te explique circuitos, diagnostique problemas y sugiera experimentos. Funciona con Groq, OpenRouter o NVIDIA (gratis).",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = CircuitColors.OnSurfaceVariant
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            // Provider selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                providerDisplayNames.forEach { (key, name) ->
                    val isSelected = selectedProvider == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (isSelected) CircuitColors.AmberContainer else CircuitColors.SurfaceVariant,
                                CircuitShapes.PillShape
                            )
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) CircuitColors.Amber else CircuitColors.Outline,
                                CircuitShapes.PillShape
                            )
                            .combinedClickable(onClick = { onProviderChange(key) }, onLongClick = null)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) CircuitColors.Amber else CircuitColors.OnSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            // Model selector
            val models = availableModels[selectedProvider] ?: emptyList()
            if (models.isNotEmpty()) {
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { }
                ) {
                    androidx.compose.material3.Text(
                        text = selectedModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        color = CircuitColors.OnSurface,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    // Simplified - in production use proper dropdown menu
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            // API Key input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.TextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(CircuitColors.SurfaceVariant, CircuitShapes.PillShape)
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Pega tu clave API aquí...", color = CircuitColors.OnSurfaceVariant) },
                    singleLine = true,
                    visualTransformation = androidx.compose.material3.PasswordVisualTransformation(),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = CircuitColors.SurfaceVariant,
                        focusedContainerColor = CircuitColors.SurfaceVariant,
                        cursorColor = CircuitColors.Emerald,
                        textColor = CircuitColors.OnSurface,
                        placeholderColor = CircuitColors.OnSurfaceVariant,
                        disabledTextColor = CircuitColors.OnSurfaceDisabled
                    )
                )
                if (apiKey.isNotBlank()) {
                    CircuitButton(
                        text = if (apiKeyValid == true) "✓ Conectado" else if (apiKeyValid == false) "✗ Error" else "Probar",
                        onClick = onTestConnection,
                        variant = if (apiKeyValid == true) ButtonVariant.PRIMARY else if (apiKeyValid == false) ButtonVariant.DANGER else ButtonVariant.AMBER,
                        enabled = !testingConnection
                    )
                    CircuitButton(
                        text = "Borrar",
                        onClick = onClearApiKey,
                        variant = ButtonVariant.SECONDARY
                    )
                } else {
                    CircuitButton(
                        text = "Guardar",
                        onClick = onSaveApiKey,
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }

            if (testingConnection) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = CircuitColors.Amber, strokeWidth = 2.dp)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    Text("Probando conexión...", color = CircuitColors.Amber, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun WorkbenchSettingsSection(
    showGrid: Boolean,
    snapToGrid: Boolean,
    gridSize: Int,
    autoSimulate: Boolean,
    onShowGridChange: (Boolean) -> Unit,
    onSnapToGridChange: (Boolean) -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onAutoSimulateChange: (Boolean) -> Unit
) {
    CircuitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚙️  Mesa de Trabajo",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W600
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "Mostrar cuadrícula",
                description = "Puntos guía para alinear piezas",
                checked = showGrid,
                onChange = onShowGridChange
            )

            SettingToggle(
                title = "Ajustar a cuadrícula",
                description = "Las piezas se alinean automáticamente",
                checked = snapToGrid,
                onChange = onSnapToGridChange
            )

            SettingSlider(
                title = "Tamaño de cuadrícula",
                description = "Distancia entre puntos (px)",
                value = gridSize.toFloat(),
                min = 10f,
                max = 100f,
                step = 5f,
                format = { "${it.roundToInt()} px" },
                onValueChange = { onGridSizeChange(it.roundToInt()) }
            )

            SettingToggle(
                title = "Simulación automática",
                description = "Calcula voltajes y corrientes en tiempo real",
                checked = autoSimulate,
                onChange = onAutoSimulateChange
            )
        }
    }
}

@Composable
fun FeedbackSettingsSection(
    hapticFeedback: Boolean,
    onHapticFeedbackChange: (Boolean) -> Unit
) {
    CircuitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "📳  Retroalimentación",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W600
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "Vibración al tocar",
                description = "Pequeña vibración al conectar piezas",
                checked = hapticFeedback,
                onChange = onHapticFeedbackChange
            )
        }
    }
}

@Composable
fun AboutSection() {
    CircuitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ℹ️  Acerca de",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W600
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                InfoRow(label = "Versión", value = "1.0.0")
                InfoRow(label = "Motor", value = "Análisis Nodal (MNA) en Kotlin")
                InfoRow(label = "Diseño", value = "Identidad HyperNexus")
                InfoRow(label = "Filosofía", value = "Cero tecnicismos, máxima claridad")
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = CircuitColors.OnSurface, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text(text = description, color = CircuitColors.OnSurfaceVariant, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = CircuitColors.Emerald,
                checkedTrackColor = CircuitColors.Emerald.copy(alpha = 0.5f),
                uncheckedThumbColor = CircuitColors.OnSurfaceVariant,
                uncheckedTrackColor = CircuitColors.Outline
            )
        )
    }
}

@Composable
fun SettingSlider(
    title: String,
    description: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, color = CircuitColors.OnSurface, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text(text = description, color = CircuitColors.OnSurfaceVariant, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Text(text = format(value), color = CircuitColors.Emerald, fontWeight = FontWeight.W600, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        androidx.compose.material3.Slider(
            value = (value - min) / (max - min),
            onValueChange = { onValueChange(min + it * (max - min)) },
            modifier = Modifier.fillMaxWidth(),
            steps = ((max - min) / step).roundToInt(),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = CircuitColors.Emerald,
                trackColor = CircuitColors.Emerald,
                inactiveTrackColor = CircuitColors.Outline
            )
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CircuitColors.OnSurfaceVariant, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Text(text = value, color = CircuitColors.OnSurface, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W500)
    }
}
