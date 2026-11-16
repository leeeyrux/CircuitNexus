package com.circuitnexus.engine

import kotlin.math.roundToInt

import com.circuitnexus.model.CircuitComponent
import com.circuitnexus.model.CircuitWire
import com.circuitnexus.model.ComponentType
import com.circuitnexus.model.SimulationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Motor de ayuda con IA (Groq, OpenRouter, NVIDIA)
 * Solo funciona si el usuario configuró su API Key en Ajustes
 */
class AiHelpEngine(
    private val apiKey: String,
    private val provider: String,  // "groq", "openrouter", "nvidia"
    private val model: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Pide a la IA que explique qué hace el circuito en lenguaje cotidiano
     */
    suspend fun explainCircuit(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        simulationState: SimulationState?
    ): Result = withContext(Dispatchers.IO) {
        try {
            val prompt = buildExplanationPrompt(components, wires, simulationState)
            val response = callApi(prompt, maxTokens = 500, temperature = 0.7)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Pide a la IA que diagnostique qué está mal y cómo arreglarlo
     */
    suspend fun diagnoseCircuit(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        simulationState: SimulationState
    ): Result = withContext(Dispatchers.IO) {
        try {
            val prompt = buildDiagnosisPrompt(components, wires, simulationState)
            val response = callApi(prompt, maxTokens = 600, temperature = 0.5)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Sugiere mejoras o siguientes pasos para aprender
     */
    suspend fun suggestNextSteps(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): Result = withContext(Dispatchers.IO) {
        try {
            val prompt = buildSuggestionPrompt(components, wires)
            val response = callApi(prompt, maxTokens = 400, temperature = 0.8)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private fun buildExplanationPrompt(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        state: SimulationState?
    ): String {
        val compDesc = components.joinToString("\n") { comp ->
            val props = comp.properties
            val extra = when (comp.type) {
                ComponentType.RESISTOR -> " (${(props.resistanceOhms ?: 0f).toInt()} Ω)"
                ComponentType.CAPACITOR -> " (${((props.capacitanceFarads ?: 0f) * 1e6).toInt()} µF)"
                ComponentType.BATTERY -> " (${(props.voltageVolts ?: 0f).toInt()}V)"
                ComponentType.LED -> " (${props.ledColor ?: "rojo"}, ${props.ledForwardVoltage}V)"
                ComponentType.TIMER_555 -> " (${props.timerMode}, ${props.frequencyHz}Hz)"
                else -> ""
            }
            "- ${comp.type.displayName}$extra"
        }

        val wireDesc = if (wires.isNotEmpty()) {
            wires.joinToString("\n") { wire ->
                "Cable: ${wire.fromComponentId}[${wire.fromPinIndex}] → ${wire.toComponentId}[${wire.toPinIndex}]"
            }
        } else {
            "Ninguno"
        }

        val voltageInfo = state?.nodeVoltages?.let { voltages ->
            voltages.joinToString(", ") { voltage ->
                "(Nodo ${voltage.key}: ${voltage.value.toInt()}V)"
            }
        } ?: "Simulación no ejecutada"

        return """
            Eres un mentor de electrónica amable y paciente para principiantes absolutos.
            Explica en español cotidiano, SIN palabras técnicas intimidantes.
            NUNCA uses términos como: "netlist", "MNA", "nodo", "análisis nodal", "tierra", "referencia", "polarización", "semiconductor", "junção PN".
            
            En su lugar usa: "energía", "voltaje" → "fuerza de la energía", "corriente" → "flujo de energía", "resistencia" → "freno", "capacitor" → "depósito de energía".
            
            Circuito del usuario:
            Piezas: 
            $compDesc
            
            Conexiones:
            $wireDesc
            
            Mediciones: $voltageInfo
            
            Explica en 3-4 frases máximas:
            1. Qué hace este circuito en la vida real (ej: "Enciende una luz", "Hace parpadear un LED", "Crea un temporizador")
            2. Cómo fluye la energía de forma simple
            3. Un consejo práctico para mejorarlo o experimentar
            """.trimIndent()
    }

    private fun buildDiagnosisPrompt(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>,
        state: SimulationState
    ): String {
        val warnings = state.warnings.joinToString("\n") { "⚠ ${it.message} — Sugerencia: ${it.suggestion}" }
        val compDesc = components.joinToString("\n") { "- ${it.type.displayName} (ID: ${it.id})" }

        return """
            Eres un mentor de electrónica que ayuda a principiantes a arreglar sus circuitos.
            Habla en español cotidiano, cálido y claro. CERO tecnicismos.
            
            Circuito actual:
            $compDesc
            
            Problemas detectados:
            $warnings
            
            Da UNA explicación corta (máx 3 frases) de:
            1. Qué pasa exactamente en palabras de todos los días
            2. Qué componente o conexión es el culpable
            3. Qué hacer concretamente para arreglarlo (paso a paso simple)
            
            Ejemplo de tono: "El problema es que la luz LED recibe demasiada energía directa de la batería. 
            Pon una resistencia de 330 ohmios entre la batería y el LED para frenar la energía. 
            Así el LED brillará sin quemarse."
            """.trimIndent()
    }

    private fun buildSuggestionPrompt(
        components: List<CircuitComponent>,
        wires: List<CircuitWire>
    ): String {
        val compTypes = components.map { it.type }.distinct().joinToString(", ") { it.displayName }

        return """
            Eres un mentor de electrónica que sugiere experimentos divertidos para aprender.
            Tono: entusiasta, animador, como un amigo que te enseña trucos.
            Español cotidiano, nada de jerga técnica.
            
            El usuario tiene ahora: $compTypes
            
            Sugiere 2-3 cosas divertidas que pueda probar a continuación para aprender más.
            Cada sugerencia en una línea, estilo: "Prueba añadir X para ver Y"
            """.trimIndent()
    }

    private fun callApi(prompt: String, maxTokens: Int, temperature: Double): String {
        val (url, headers, body) = when (provider.lowercase()) {
            "groq" -> buildGroqRequest(prompt, maxTokens, temperature)
            "openrouter" -> buildOpenRouterRequest(prompt, maxTokens, temperature)
            "nvidia" -> buildNvidiaRequest(prompt, maxTokens, temperature)
            else -> buildGroqRequest(prompt, maxTokens, temperature)
        }

        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw RuntimeException("Respuesta vacía")

        if (!response.isSuccessful) {
            throw RuntimeException("Error API ${response.code}: $responseBody")
        }

        return parseResponse(responseBody)
    }

    private fun buildGroqRequest(prompt: String, maxTokens: Int, temperature: Double): Triple<String, okhttp3.Headers, okhttp3.RequestBody> {
        val body = json.encodeToString(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "Eres un mentor de electrónica amable para principiantes. Explicas todo en español cotidiano sin tecnicismos."),
                    mapOf("role" to "user", "content" to prompt)
                ),
                "max_tokens" to maxTokens,
                "temperature" to temperature,
                "stream" to false
            )
        )
        val headers = okhttp3.Headers.Builder()
            .add("Authorization", "Bearer $apiKey")
            .add("Content-Type", "application/json")
            .build()
        return Triple("https://api.groq.com/openai/v1/chat/completions", headers, body.toRequestBody("application/json".toMediaType()))
    }

    private fun buildOpenRouterRequest(prompt: String, maxTokens: Int, temperature: Double): Triple<String, okhttp3.Headers, okhttp3.RequestBody> {
        val body = json.encodeToString(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "Eres un mentor de electrónica amable para principiantes. Explicas todo en español cotidiano sin tecnicismos."),
                    mapOf("role" to "user", "content" to prompt)
                ),
                "max_tokens" to maxTokens,
                "temperature" to temperature
            )
        )
        val headers = okhttp3.Headers.Builder()
            .add("Authorization", "Bearer $apiKey")
            .add("Content-Type", "application/json")
            .add("HTTP-Referer", "https://circuitnexus.app")
            .add("X-Title", "CircuitNexus")
            .build()
        return Triple("https://openrouter.ai/api/v1/chat/completions", headers, body.toRequestBody("application/json".toMediaType()))
    }

    private fun buildNvidiaRequest(prompt: String, maxTokens: Int, temperature: Double): Triple<String, okhttp3.Headers, okhttp3.RequestBody> {
        val body = json.encodeToString(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "Eres un mentor de electrónica amable para principiantes. Explicas todo en español cotidiano sin tecnicismos."),
                    mapOf("role" to "user", "content" to prompt)
                ),
                "max_tokens" to maxTokens,
                "temperature" to temperature
            )
        )
        val headers = okhttp3.Headers.Builder()
            .add("Authorization", "Bearer $apiKey")
            .add("Content-Type", "application/json")
            .build()
        return Triple("https://integrate.api.nvidia.com/v1/chat/completions", headers, body.toRequestBody("application/json".toMediaType()))
    }

    private fun parseResponse(jsonStr: String): String {
        val parsed = json.decodeFromString<AiResponse>(jsonStr)
        return parsed.choices.firstOrNull()?.message?.content?.trim() 
            ?: "La IA no pudo generar respuesta. Intenta de nuevo."
    }

    @Serializable
    private data class AiResponse(
        val choices: List<Choice>
    )

    @Serializable
    private data class Choice(
        val message: Message
    )

    @Serializable
    private data class Message(
        val role: String,
        val content: String
    )

    /** Resultado simple del asistente */
    sealed class Result {
        data class Success(val value: String) : Result()
        data class Failure(val exception: Exception) : Result()

        fun onSuccess(action: (String) -> Unit): Result {
            if (this is Success) action(value)
            return this
        }

        fun onFailure(action: (Exception) -> Unit): Result {
            if (this is Failure) action(exception)
            return this
        }
    }

    companion object {
        // Modelos recomendados por proveedor
        val GROQ_MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-70b-versatile",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        )

        val OPENROUTER_MODELS = listOf(
            "meta-llama/llama-3.3-70b-instruct",
            "google/gemini-flash-1.5",
            "anthropic/claude-3.5-haiku",
            "mistralai/mistral-7b-instruct"
        )

        val NVIDIA_MODELS = listOf(
            "meta/llama-3.3-70b-instruct",
            "nvidia/nemotron-3-ultra",
            "google/gemma-2-9b-it"
        )

        fun getDefaultModel(provider: String): String = when (provider.lowercase()) {
            "groq" -> GROQ_MODELS[0]
            "openrouter" -> OPENROUTER_MODELS[0]
            "nvidia" -> NVIDIA_MODELS[0]
            else -> GROQ_MODELS[0]
        }
    }
}
