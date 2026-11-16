package com.circuitnexus.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utilidad centralizada para feedback háptico
 * Diferentes patrones para cada tipo de interacción
 */
class HapticManager(private val context: Context) {
    
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var _enabled = mutableStateOf(true)
    val enabled = _enabled
    
    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }
    
    // === Patrones predefinidos ===
    
    /** Click ligero - selección, tap en pieza */
    fun lightClick() = trigger(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    
    /** Click medio - conexión exitosa */
    fun mediumClick() = trigger(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    
    /** Click fuerte - acción completada, circuito funcionando */
    fun heavyClick() = trigger(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    
    /** Doble click - advertencia, error */
    fun warning() = trigger(VibrationEffect.createWaveform(longArrayOf(0, 15, 10, 15), -1))
    
    /** Error - cortocircuito, peligro */
    fun error() = trigger(VibrationEffect.createWaveform(longArrayOf(0, 30, 20, 30, 20, 30), -1))
    
    /** Éxito - tutorial completado, logro */
    fun success() = trigger(VibrationEffect.createWaveform(longArrayOf(0, 10, 20, 10, 20, 10, 30), -1))
    
    /** Arrastre iniciado */
    fun dragStart() = trigger(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE / 2))
    
    /** Arrastre terminado (snap a grid) */
    fun dragEndSnapped() = trigger(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
    
    /** Partícula llega a LED */
    fun particleHit() = trigger(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE / 3))
    
    /** Scroll/paginación */
    fun scrollTick() = trigger(VibrationEffect.createOneShot(3, VibrationEffect.DEFAULT_AMPLITUDE / 4))
    
    private fun trigger(effect: VibrationEffect) {
        if (!_enabled.value) return
        vibrator.vibrate(effect)
    }
    
    // === Compatibilidad con HapticFeedbackConstants (para Views) ===
    fun performHapticFeedback(view: android.view.View, feedbackType: Int) {
        if (_enabled.value) {
            view.performHapticFeedback(feedbackType)
        }
    }
}

/**
 * Instancia global accesible desde Compose
 */
object HapticFeedback {
    private var manager: HapticManager? = null
    
    fun initialize(context: Context) {
        manager = HapticManager(context)
    }
    
    val instance: HapticManager
        get() = manager ?: throw IllegalStateException("HapticFeedback no inicializado. Llama a initialize(context) en Application/Activity.")
    
    @Composable
    fun rememberHaptic(): HapticManager {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        return remember { HapticManager(ctx) }
    }
}

// === Extension functions para uso fácil ===

fun CoroutineScope.hapticLightClick() = launch(Dispatchers.IO) { HapticFeedback.instance.lightClick() }
fun CoroutineScope.hapticMediumClick() = launch(Dispatchers.IO) { HapticFeedback.instance.mediumClick() }
fun CoroutineScope.hapticHeavyClick() = launch(Dispatchers.IO) { HapticFeedback.instance.heavyClick() }
fun CoroutineScope.hapticWarning() = launch(Dispatchers.IO) { HapticFeedback.instance.warning() }
fun CoroutineScope.hapticError() = launch(Dispatchers.IO) { HapticFeedback.instance.error() }
fun CoroutineScope.hapticSuccess() = launch(Dispatchers.IO) { HapticFeedback.instance.success() }
fun CoroutineScope.hapticDragStart() = launch(Dispatchers.IO) { HapticFeedback.instance.dragStart() }
fun CoroutineScope.hapticDragEndSnapped() = launch(Dispatchers.IO) { HapticFeedback.instance.dragEndSnapped() }
fun CoroutineScope.hapticParticleHit() = launch(Dispatchers.IO) { HapticFeedback.instance.particleHit() }
fun CoroutineScope.hapticScrollTick() = launch(Dispatchers.IO) { HapticFeedback.instance.scrollTick() }

// === Composable helper ===

@Composable
fun HapticButton(
    onClick: () -> Unit,
    hapticType: HapticType = HapticType.LIGHT,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @Composable () -> Unit
) {
    val haptic = HapticFeedback.rememberHaptic()
    
    androidx.compose.material3.Button(
        onClick = {
            when (hapticType) {
                HapticType.LIGHT -> haptic.lightClick()
                HapticType.MEDIUM -> haptic.mediumClick()
                HapticType.HEAVY -> haptic.heavyClick()
                HapticType.SUCCESS -> haptic.success()
                HapticType.WARNING -> haptic.warning()
                HapticType.ERROR -> haptic.error()
            }
            onClick()
        },
        modifier = modifier
    ) {
        content()
    }
}

enum class HapticType {
    LIGHT, MEDIUM, HEAVY, SUCCESS, WARNING, ERROR
}