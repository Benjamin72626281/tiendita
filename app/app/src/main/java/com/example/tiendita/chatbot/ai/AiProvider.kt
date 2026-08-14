package com.example.tiendita.chatbot.ai

import com.example.tiendita.model.ChatMessage

/**
 * Contrato común para cualquier proveedor de IA (Gemini, Groq, etc.).
 * Permite usar varios proveedores de forma intercambiable y encadenarlos
 * como respaldo si uno falla o se le acaban los intentos/cuota.
 */
interface AiProvider {

    /** Nombre para mostrar en logs o mensajes ("Gemini", "Groq"). */
    val nombre: String

    /**
     * Genera una respuesta del asistente.
     *
     * @param systemPrompt instrucciones + contexto de la tienda (productos, ventas, etc.)
     * @param historial mensajes previos de la conversación, para dar contexto
     * @param mensajeUsuario la pregunta actual del usuario
     * @return el texto de respuesta del modelo
     * @throws Exception si la llamada falla (sin red, cuota agotada, error del servidor, etc.)
     */
    suspend fun generar(
        systemPrompt: String,
        historial: List<ChatMessage>,
        mensajeUsuario: String
    ): String
}
