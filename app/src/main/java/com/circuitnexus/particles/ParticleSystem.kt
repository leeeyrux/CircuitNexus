package com.circuitnexus.particles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import com.circuitnexus.ui.theme.CircuitColors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

/**
 * Sistema de partículas para visualizar flujo de energía
 * Partículas que se mueven por cables, se frenan en resistencias, brillan en LEDs
 */
data class EnergyParticle(
    val id: Int,
    var progress: Float, // 0..1 a lo largo del cable
    val wireId: Long,
    val speed: Float, // unidades por segundo
    val color: Color,
    val size: Float,
    val intensity: Float // 0..1 brillo
) {
    val isAtComponent: Boolean
        get() = progress >= 1f || progress <= 0f
}

class ParticleSystem(
    private val maxParticles: Int = 200
) {
    private val particles = mutableListOf<EnergyParticle>()
    private val random = Random()
    private var nextId = 0
    private val _particlesFlow = MutableStateFlow<List<EnergyParticle>>(emptyList())
    val particlesFlow = _particlesFlow.asStateFlow()
    
    fun update(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        simulationState: SimulationState,
        deltaTime: Float // segundos
    ) {
        // Generar nuevas partículas en fuentes de energía
        spawnParticles(components, wires, simulationState)
        
        // Actualizar partículas existentes
        val toRemove = mutableListOf<EnergyParticle>()
        
        for (particle in particles) {
            val wire = wires.find { it.id == particle.wireId }
            if (wire == null) {
                toRemove.add(particle)
                continue
            }
            
            // Velocidad base modificada por componentes en el camino
            var effectiveSpeed = particle.speed
            
            // Verificar si pasa por una resistencia
            val fromComp = components.find { it.id == wire.fromComponentId }
            val toComp = components.find { it.id == wire.toComponentId }
            
            if (fromComp?.type == ComponentType.RESISTOR || toComp?.type == ComponentType.RESISTOR) {
                val resistance = fromComp?.properties?.resistanceOhms ?: toComp?.properties?.resistanceOhms ?: 330f
                // Más resistencia = más lento (logarítmico para que se sienta)
                effectiveSpeed *= (100f / (resistance + 10f)).coerceIn(0.05f, 1f)
            }
            
            // Verificar si entra a un LED (se "absorbe" y brilla)
            if (fromComp?.type == ComponentType.LED || toComp?.type == ComponentType.LED) {
                if (particle.progress > 0.8f) {
                    // Partícula llega al LED - aumenta intensidad
                    // El LED "brilla" más con más partículas
                }
            }
            
            // Mover partícula
            particle.progress += effectiveSpeed * deltaTime * 0.5 // factor de escala
            
            // Si llega al final, eliminar o rebotar
            if (particle.progress >= 1f) {
                toRemove.add(particle)
            }
        }
        
        particles.removeAll(toRemove)
        
        // Limitar cantidad
        if (particles.size > maxParticles) {
            particles.removeFirst(particles.size - maxParticles)
        }
        
        _particlesFlow.value = particles.toList()
    }
    
    private fun spawnParticles(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        simulationState: SimulationState
    ) {
        // Generar desde baterías activas
        val batteries = components.filter { it.type == ComponentType.BATTERY }
        
        for (battery in batteries) {
            val current = simulationState.componentCurrents[battery.id] ?: 0f
            if (current > 0.001f) { // Solo si hay corriente real
                val connectedWires = wires.filter { 
                    it.fromComponentId == battery.id || it.toComponentId == battery.id 
                }
                
                for (wire in connectedWires) {
                    // Probabilidad de spawn proporcional a corriente
                    val spawnChance = (current * 10).coerceIn(0f, 1f)
                    if (random.nextFloat() < spawnChance * 0.1f) { // 10% base rate
                        val speed = (current * 50).coerceIn(0.1f, 2f)
                        particles.add(EnergyParticle(
                            id = nextId++,
                            progress = 0f,
                            wireId = wire.id,
                            speed = speed,
                            color = CircuitColors.Emerald,
                            size = (4f + current * 100).coerceIn(3f, 8f),
                            intensity = (current * 5).coerceIn(0.3f, 1f)
                        ))
                    }
                }
            }
        }
        
        // Generar desde 555 en salida (pin 3)
        val timers = components.filter { it.type == ComponentType.TIMER_555 }
        for (timer in timers) {
            val outputWires = wires.filter { it.fromComponentId == timer.id && it.fromPinIndex == 2 } // Pin 3 = salida
            for (wire in outputWires) {
                if (random.nextFloat() < 0.05f) {
                    particles.add(EnergyParticle(
                        id = nextId++,
                        progress = 0f,
                        wireId = wire.id,
                        speed = 0.8f,
                        color = CircuitColors.Amber,
                        size = 5f,
                        intensity = 0.8f
                    ))
                }
            }
        }
    }
    
    fun clear() {
        particles.clear()
        _particlesFlow.value = emptyList()
    }
}

@Composable
fun ParticleLayer(
    particles: List<EnergyParticle>,
    wires: List<CircuitWire>,
    components: List<CircuitComponent>,
    panOffset: androidx.compose.ui.geometry.Offset,
    zoom: Float
) {
    if (particles.isEmpty()) return
    
    Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        for (particle in particles) {
            val wire = wires.find { it.id == particle.wireId } ?: continue
            val fromComp = components.find { it.id == wire.fromComponentId }
            val toComp = components.find { it.id == wire.toComponentId }
            if (fromComp == null || toComp == null) continue
            
            // Calcular posición en el cable
            val fromPos = getPinWorldPosition(fromComp, wire.fromPinIndex, panOffset, zoom)
            val toPos = getPinWorldPosition(toComp, wire.toPinIndex, panOffset, zoom)
            
            val x = fromPos.x + (toPos.x - fromPos.x) * particle.progress
            val y = fromPos.y + (toPos.y - fromPos.y) * particle.progress
            
            // Dibujar partícula con brillo
            val alpha = particle.intensity * 0.8f
            val color = particle.color.copy(alpha = alpha)
            
            // Círculo principal
            drawCircle(
                color = color,
                center = androidx.compose.ui.geometry.Offset(x, y),
                radius = particle.size * zoom
            )
            
            // Halo exterior para partículas intensas
            if (particle.intensity > 0.7f) {
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    radius = particle.size * zoom * 2.5f
                )
            }
            
            // Estela (trail) - círculos anteriores más pequeños
            for (i in 1..3) {
                val trailProgress = (particle.progress - i * 0.02f).coerceAtLeast(0f)
                if (trailProgress >= 0f) {
                    val trailX = fromPos.x + (toPos.x - fromPos.x) * trailProgress
                    val trailY = fromPos.y + (toPos.y - fromPos.y) * trailProgress
                    drawCircle(
                        color = color.copy(alpha = alpha * (1f - i * 0.25f)),
                        center = androidx.compose.ui.geometry.Offset(trailX, trailY),
                        radius = particle.size * zoom * (1f - i * 0.2f)
                    )
                }
            }
        }
    }
}

private fun getPinWorldPosition(
    component: CircuitComponent,
    pinIndex: Int,
    panOffset: androidx.compose.ui.geometry.Offset,
    zoom: Float
): androidx.compose.ui.geometry.Offset {
    val pinCount = com.circuitnexus.model.CircuitWire.getPinCount(component.type)
    val angle = if (pinCount > 1) (pinIndex * 360f / pinCount - 90 + component.position.rotation) else 0f
    val radius = 40f * zoom
    val centerX = component.position.x * zoom + panOffset.x
    val centerY = component.position.y * zoom + panOffset.y
    
    return androidx.compose.ui.geometry.Offset(
        centerX + radius * kotlin.math.cos(angle * kotlin.math.PI / 180),
        centerY + radius * kotlin.math.sin(angle * kotlin.math.PI / 180)
    )
}

/**
 * Efecto de "brillo" en componentes activos
 */
@Composable
fun ComponentGlowEffect(
    component: CircuitComponent,
    simulationState: SimulationState,
    panOffset: androidx.compose.ui.geometry.Offset,
    zoom: Float
) {
    val current = simulationState.componentCurrents[component.id] ?: 0f
    if (current < 0.001f) return
    
    val centerX = component.position.x * zoom + panOffset.x
    val centerY = component.position.y * zoom + panOffset.y
    val radius = 40f * zoom
    
    // Color según tipo
    val glowColor = when (component.type) {
        com.circuitnexus.model.ComponentType.LED -> CircuitColors.Emerald
        com.circuitnexus.model.ComponentType.BATTERY -> CircuitColors.Amber
        com.circuitnexus.model.ComponentType.TIMER_555 -> CircuitColors.Amber
        com.circuitnexus.model.ComponentType.CAPACITOR -> CircuitColors.Cyan
        else -> CircuitColors.Emerald
    }
    
    // Pulso animado (fuera del Canvas: el Canvas no es composable)
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 1000
                0.5f at 500
            },
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glow-pulse"
    )

    Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        val glowRadius = radius * (1f + 0.3f * pulse)
        val alpha = (0.15f + 0.1f * pulse) * (current * 10).coerceIn(0f, 1f)
        
        drawCircle(
            color = glowColor.copy(alpha = alpha),
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            radius = glowRadius,
            style = androidx.compose.ui.graphics.DrawingStyle.Stroke(2f * zoom)
        )
        
        // Segundo anillo más sutil
        drawCircle(
            color = glowColor.copy(alpha = alpha * 0.5f),
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            radius = glowRadius * 1.5f,
            style = androidx.compose.ui.graphics.DrawingStyle.Stroke(1f * zoom)
        )
    }
}