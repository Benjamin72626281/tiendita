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
 * Proveedor de IA que usa la API de Google Gemini (generativelanguage.googleapis.com).
 * La API key se lee de BuildConfig.GEMINI_API_KEY, generada a partir de local.properties.
 */
class GeminiProvider(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelo: String = "gemini-2.0-flash"
) : AiProvider {

    override val nombre: String = "Gemini"

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
            throw IllegalStateException("Falta configurar GEMINI_API_KEY en local.properties")
        }

        val contents = JSONArray()
        for (mensaje in historial) {
            val rolGemini = if (mensaje.role == "user") "user" else "model"
            contents.put(
                JSONObject()
                    .put("role", rolGemini)
                    .put("parts", JSONArray().put(JSONObject().put("text", mensaje.texto)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", mensajeUsuario)))
        )

        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject().put("temperature", 0.4).put("maxOutputTokens", 700)
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw Exception("Gemini respondió con error ${response.code}: $bodyStr")
            }
            val json = JSONObject(bodyStr)
            val candidatos = json.optJSONArray("candidates")
                ?: throw Exception("Gemini no devolvió una respuesta válida: $bodyStr")

            candidatos.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        }
    }
}
