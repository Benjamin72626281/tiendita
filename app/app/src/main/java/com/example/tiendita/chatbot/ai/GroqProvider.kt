package com.example.tiendita.chatbot.ai

import com.example.tiendita.BuildConfig
import com.example.tiendita.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Proveedor de IA que usa la API de Groq (compatible con el formato de OpenAI).
 * Se usa como respaldo automático si Gemini falla o se queda sin cuota.
 * La API key se lee de BuildConfig.GROQ_API_KEY, generada a partir de local.properties.
 */
class GroqProvider(
    private val apiKey: String = BuildConfig.GROQ_API_KEY,
    private val modelo: String = "llama-3.3-70b-versatile"
) : AiProvider {

    override val nombre: String = "Groq"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generar(
        systemPrompt: String,
        historial: List<ChatMessage>,
        mensajeUsuario: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Falta configurar GROQ_API_KEY en local.properties")
        }

        val mensajes = JSONArray()
        mensajes.put(JSONObject().put("role", "system").put("content", systemPrompt))
        for (mensaje in historial) {
            val rolOpenAi = if (mensaje.role == "user") "user" else "assistant"
            mensajes.put(JSONObject().put("role", rolOpenAi).put("content", mensaje.texto))
        }
        mensajes.put(JSONObject().put("role", "user").put("content", mensajeUsuario))

        val body = JSONObject()
            .put("model", modelo)
            .put("messages", mensajes)
            .put("temperature", 0.4)
            .put("max_tokens", 700)

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Groq respondió con error ${response.code}: $bodyStr")
            }
            val json = JSONObject(bodyStr)
            val choices = json.optJSONArray("choices")
                ?: throw Exception("Groq no devolvió una respuesta válida: $bodyStr")

            choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }
}
