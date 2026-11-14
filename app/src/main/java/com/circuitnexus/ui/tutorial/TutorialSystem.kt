package com.circuitnexus.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable

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
import com.circuitnexus.model.ComponentType
import com.circuitnexus.ui.common.ButtonVariant
import com.circuitnexus.ui.common.CircuitButton
import com.circuitnexus.ui.common.CircuitCard
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes

/**
 * Tutorial interactivo "Primeros Pasos"
 * Guía paso a paso con validación automática
 */
sealed class TutorialStep(
    val id: String,
    val title: String,
    val instruction: String,
    val targetComponent: ComponentType? = null,
    val targetAction: TutorialAction? = null,
    val validation: (TutorialState) -> Boolean = { true }
) {
    class PlaceComponent(
        id: String,
        title: String,
        instruction: String,
        val componentType: ComponentType,
        val hintPosition: String = "centro"
    ) : TutorialStep(
        id = id,
        title = title,
        instruction = instruction,
        targetComponent = componentType,
        targetAction = TutorialAction.PLACE,
        validation = { state -> state.placedComponents.contains(componentType) }
    )

    class ConnectComponents(
        id: String,
        title: String,
        instruction: String,
        val fromComponent: ComponentType,
        val fromPin: String,
        val toComponent: ComponentType,
        val toPin: String
    ) : TutorialStep(
        id = id,
        title = title,
        instruction = instruction,
        targetAction = TutorialAction.CONNECT,
        validation = { state -> state.hasConnection(fromComponent, toComponent) }
    )

    class WaitForResult(
        id: String,
        title: String,
        instruction: String,
        val expectedResult: String
    ) : TutorialStep(
        id = id,
        title = title,
        instruction = instruction,
        targetAction = TutorialAction.OBSERVE,
        validation = { state -> state.simulationRunning && state.ledIsLit }
    )

    class Complete(
        id: String,
        title: String,
        instruction: String,
        val celebrationMessage: String
    ) : TutorialStep(
        id = id,
        title = title,
        instruction = instruction,
        targetAction = TutorialAction.COMPLETE
    )
}

enum class TutorialAction {
    PLACE, CONNECT, OBSERVE, COMPLETE
}

data class TutorialState(
    val placedComponents: Set<ComponentType> = emptySet(),
    val connections: Set<Pair<ComponentType, ComponentType>> = emptySet(),
    val simulationRunning: Boolean = false,
    val ledIsLit: Boolean = false,
    val currentValues: Map<String, Float> = emptyMap()
) {
    fun hasConnection(from: ComponentType, to: ComponentType): Boolean {
        return connections.contains(Pair(from, to)) || connections.contains(Pair(to, from))
    }
}

object TutorialContent {
    
    val FIRST_CIRCUIT_TUTORIAL = listOf<TutorialStep>(
        TutorialStep.PlaceComponent(
            id = "step1_battery",
            title = "Paso 1: La Energía",
            instruction = "Arrastra una **Batería** del cajón al centro de la mesa.\n\nEs la fuente de toda la energía, como un tanque de agua a presión.",
            componentType = ComponentType.BATTERY
        ),
        TutorialStep.PlaceComponent(
            id = "step2_resistor",
            title = "Paso 2: El Freno",
            instruction = "Ahora pon una **Resistencia** cerca de la batería.\n\nProtege al LED limitando cuánta energía pasa. Sin ella, el LED se quema.",
            componentType = ComponentType.RESISTOR
        ),
        TutorialStep.PlaceComponent(
            id = "step3_led",
            title = "Paso 3: La Luz",
            instruction = "Coloca un **LED** al lado de la resistencia.\n\nLa pata larga (+) va hacia la resistencia. La corta (-) hacia el negativo.",
            componentType = ComponentType.LED
        ),
        TutorialStep.PlaceComponent(
            id = "step4_ground",
            title = "Paso 4: El Retorno",
            instruction = "Añade **Negativo / Tierra** y conéctalo al LED.\n\nLa energía necesita un camino de vuelta para fluir. Sin tierra, no hay circuito.",
            componentType = ComponentType.GROUND
        ),
        TutorialStep.ConnectComponents(
            id = "step5_connect1",
            title = "Paso 5: Primera Conexión",
            instruction = "Toca el **positivo (+) de la Batería** y luego la **entrada de la Resistencia**.\n\n¡Se crea un cable automático! Verde = energía fluyendo.",
            fromComponent = ComponentType.BATTERY,
            fromPin = "Positivo (+)",
            toComponent = ComponentType.RESISTOR,
            toPin = "Entrada"
        ),
        TutorialStep.ConnectComponents(
            id = "step6_connect2",
            title = "Paso 6: Segunda Conexión",
            instruction = "Conecta la **salida de la Resistencia** con el **ánodo (+) del LED**.\n\nLa energía pasa por el freno y llega controlada al LED.",
            fromComponent = ComponentType.RESISTOR,
            fromPin = "Salida",
            toComponent = ComponentType.LED,
            toPin = "Ánodo (+)"
        ),
        TutorialStep.ConnectComponents(
            id = "step7_connect3",
            title = "Paso 7: Cierra el Circuito",
            instruction = "Une el **cátodo (-) del LED** con el **Negativo / Tierra**.\n\n¡El círculo se cierra! La energía ya puede fluir completa.",
            fromComponent = ComponentType.LED,
            fromPin = "Cátodo (−)",
            toComponent = ComponentType.GROUND,
            toPin = "Tierra"
        ),
        TutorialStep.ConnectComponents(
            id = "step8_connect4",
            title = "Paso 8: El Último Enlace",
            instruction = "Conecta el **negativo (−) de la Batería** con la **Tierra**.\n\nAhora todo está unido: Batería → Resistencia → LED → Tierra → Batería.",
            fromComponent = ComponentType.BATTERY,
            fromPin = "Negativo (−)",
            toComponent = ComponentType.GROUND,
            toPin = "Tierra"
        ),
        TutorialStep.WaitForResult(
            id = "step9_observe",
            title = "Paso 9: ¡Funciona!",
            instruction = "Observa: el LED brilla 💡, los cables se ponen verdes 🟢.\n\nToca cualquier cable para ver el **Multímetro** con voltaje y corriente.",
            expectedResult = "LED encendido"
        ),
        TutorialStep.Complete(
            id = "step10_complete",
            title = "¡Tu Primer Circuito! 🎉",
            instruction = "Has creado un circuito real que enciende un LED de forma segura.\n\n**Lo aprendido:** Batería da energía → Resistencia frena → LED brilla → Tierra retorna.",
            celebrationMessage = "¡Eres un creador de circuitos! 🌟\n¿Quieres hacer que parpadee con el Chip 555?"
        )
    )
    
    val BLINK_TUTORIAL = listOf<TutorialStep>(
        TutorialStep.PlaceComponent(
            id = "blink1_battery",
            title = "Paso 1: Energía",
            instruction = "Batería 9V en el centro.",
            componentType = ComponentType.BATTERY
        ),
        TutorialStep.PlaceComponent(
            id = "blink2_555",
            title = "Paso 2: El Cerebro",
            instruction = "Chip **555** - el mago de los ritmos. Lo pondremos en modo **Astable** (oscilador).",
            componentType = ComponentType.TIMER_555
        ),
        TutorialStep.PlaceComponent(
            id = "blink3_r1",
            title = "Paso 3: Resistencia R1",
            instruction = "Resistencia entre pin 7 (Descarga) y pin 8 (VCC). Controla carga.",
            componentType = ComponentType.RESISTOR
        ),
        TutorialStep.PlaceComponent(
            id = "blink4_r2",
            title = "Paso 4: Resistencia R2",
            instruction = "Resistencia entre pin 7 (Descarga) y pin 6 (Umbral). Controla descarga.",
            componentType = ComponentType.RESISTOR
        ),
        TutorialStep.PlaceComponent(
            id = "blink5_cap",
            title = "Paso 5: Capacitor",
            instruction = "Capacitor entre pin 2 (Disparo) / pin 6 (Umbral) y Tierra. Crea el ritmo.",
            componentType = ComponentType.CAPACITOR
        ),
        TutorialStep.PlaceComponent(
            id = "blink6_led",
            title = "Paso 6: LED en Salida",
            instruction = "LED + Resistencia 330Ω en pin 3 (Salida) del 555. ¡Verás el parpadeo!",
            componentType = ComponentType.LED
        ),
        TutorialStep.PlaceComponent(
            id = "blink7_ground",
            title = "Paso 7: Tierra y Alimentación",
            instruction = "Tierra en pin 1. Batería (+) a pin 8 (VCC) y pin 4 (Reset). Batería (−) a Tierra.",
            componentType = ComponentType.GROUND
        ),
        TutorialStep.Complete(
            id = "blink8_done",
            title = "¡Parpadeo Automático! ⚡",
            instruction = "El 555 hace oscilar el capacitor: se carga por R1+R2, se descarga por R2.\n\nFrecuencia ≈ 1.44 / ((R1+2×R2) × C)\n\n¡Has construido un oscilador real!",
            celebrationMessage = "¡Dominas el ritmo! 🥁\nPrueba cambiando R1, R2 o C para ver qué pasa."
        )
    )
}

@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    progress: Float, // 0..1
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onActionHint: (ComponentType?) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Fondo semitransparente
        Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background.copy(alpha = 0.6f)))
        
        // Card del tutorial
        CircuitCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(CircuitColors.Surface, CircuitShapes.CardShape)
                .border(2.dp, CircuitColors.Emerald, CircuitShapes.CardShape)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header con progreso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎓", fontSize = 24.sp)
                        Column {
                            Text(currentStep.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = CircuitColors.OnSurface, fontWeight = FontWeight.W600)
                            // Barra de progreso
                            Box(
                                modifier = Modifier.width(200.dp).height(4.dp).background(CircuitColors.SurfaceVariant, CircuitShapes.SmallShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width((200 * progress).dp)
                                        .fillMaxHeight()
                                        .background(CircuitColors.Emerald, CircuitShapes.SmallShape)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CircuitColors.SurfaceVariant, CircuitShapes.CircleShape)
                            .combinedClickable(onClick = onSkip, onLongClick = null)
                    ) {
                        Text(
                            "✕",
                            color = CircuitColors.OnSurfaceVariant,
                            fontSize = 15.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                
                // Instrucción principal
                Text(
                    text = currentStep.instruction,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = CircuitColors.OnSurface,
                    lineHeight = 24.sp
                )
                
                // Pista de acción
                currentStep.targetComponent?.let { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👉", fontSize = 18.sp)
                        Text("Toca **${type.displayName}** en el cajón", color = CircuitColors.Emerald, fontWeight = FontWeight.W600, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                    }
                }
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircuitButton(
                        text = "Saltar",
                        onClick = onSkip,
                        variant = ButtonVariant.SECONDARY,
                        fullWidth = true
                    )
                    CircuitButton(
                        text = when (currentStep.targetAction) {
                            TutorialAction.COMPLETE -> "¡Hecho! 🎉"
                            else -> "Siguiente →"
                        },
                        onClick = onNext,
                        variant = ButtonVariant.PRIMARY,
                        fullWidth = true
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialCelebration(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().background(CircuitColors.Background.copy(alpha = 0.8f)))
        
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Confeti / animación simple
            Box(modifier = Modifier.size(120.dp)) {
                // Partículas de celebración
                ConfettiAnimation()
            }
            
            Text("🎉", fontSize = 64.sp)
            
            Text(
                text = message,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = CircuitColors.OnSurface,
                fontWeight = FontWeight.W600,
                textAlign = TextAlign.Center
            )
            
            CircuitButton(
                text = "¡Continuar explorando! 🚀",
                onClick = onDismiss,
                variant = ButtonVariant.PRIMARY,
                fullWidth = false,
                icon = "🚀"
            )
        }
    }
}

@Composable
fun ConfettiAnimation() {
    // Placeholder para animación de confeti
    // Se puede implementar con Canvas + partículas aleatorias
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Trigger confetti
    }
}

@Composable
fun TutorialPointer(
    targetPosition: androidx.compose.ui.geometry.Offset,
    message: String
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(CircuitColors.Surface, CircuitShapes.CardShape)
            .border(2.dp, CircuitColors.Emerald, CircuitShapes.CardShape)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👇", fontSize = 18.sp)
        Text(
            message,
            color = CircuitColors.OnSurface,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
