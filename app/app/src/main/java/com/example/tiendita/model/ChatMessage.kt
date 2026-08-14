package com.example.tiendita.model

/**
 * Mensaje dentro de la conversación con el asistente de IA.
 * role: "user" (el dueño de la tienda) o "model" (respuesta del asistente).
 */
data class ChatMessage(
    val role: String,
    val texto: String
)
