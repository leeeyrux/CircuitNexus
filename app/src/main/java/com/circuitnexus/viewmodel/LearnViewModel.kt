package com.circuitnexus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.circuitnexus.model.ComponentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LearnViewModel : ViewModel() {

    private val _selectedCategory = MutableStateFlow<LearnCategory>(LearnCategory.BASICS)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    val completedLessons = _completedLessons.asStateFlow()

    fun selectCategory(category: LearnCategory) {
        _selectedCategory.value = category
    }

    fun markLessonComplete(lessonId: String) {
        _completedLessons.value = _completedLessons.value + lessonId
    }

    fun isLessonComplete(lessonId: String): Boolean = _completedLessons.value.contains(lessonId)

    val lessonsForCategory: Map<LearnCategory, List<Lesson>> = mapOf(
        LearnCategory.BASICS to listOf(
            Lesson("what_is_electricity", "¿Qué es la electricidad?", "Imagina el agua fluyendo por una manguera...", 1),
            Lesson("voltage_current", "Voltaje y Corriente", "La presión y el caudal de la energía", 2),
            Lesson("first_circuit", "Tu primer circuito", "Batería + LED + Resistencia = ¡Luz!", 3),
            Lesson("breadboard_basics", "Cómo usar la mesa de trabajo", "Conecta piezas sin soldar", 4)
        ),
        LearnCategory.COMPONENTS to listOf(
            Lesson("battery", "La Batería: Fuente de energía", "De dónde sale la fuerza", 1),
            Lesson("resistor", "La Resistencia: El freno", "Controla cuánta energía pasa", 2),
            Lesson("led", "El LED: Luz mágica", "Se enciende con poca energía", 3),
            Lesson("capacitor", "El Capacitor: Depósito", "Guarda y suelta energía despacio", 4),
            Lesson("switch", "El Interruptor: Tú mandas", "Abre y cierra el paso", 5),
            Lesson("timer555", "El Chip 555: El reloj", "Crea ritmos y parpadeos", 6)
        ),
        LearnCategory.PROJECTS to listOf(
            Lesson("flashlight", "Linterna simple", "Enciende/apaga con botón", 1),
            Lesson("blinky", "LED parpadeante", "El 555 hace el trabajo", 2),
            Lesson("fade", "Luz que respira", "Se enciende y apaga suavemente", 3),
            Lesson("timer", "Temporizador de 1 min", "Para cepillarte los dientes", 4),
            Lesson("alarm", "Alarma de luz", "Suena cuando hay luz", 5)
        ),
        LearnCategory.TROUBLESHOOTING to listOf(
            Lesson("led_not_light", "Mi LED no se enciende", "Revisa conexiones y polaridad", 1),
            Lesson("led_burned", "Se me quemó el LED", "Siempre usa resistencia", 2),
            Lesson("battery_hot", "La batería se calienta", "¡Cortocircuito! Revisa cables", 3),
            Lesson("nothing_happens", "Nada pasa", "Verifica que todo esté conectado", 4)
        )
    )
}

enum class LearnCategory(val title: String, val icon: String) {
    BASICS("Empezando", "🌱"),
    COMPONENTS("Conoce tus piezas", "🧩"),
    PROJECTS("Proyectos divertidos", "🛠️"),
    TROUBLESHOOTING("Cuando algo falla", "🩹")
}

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val order: Int
)