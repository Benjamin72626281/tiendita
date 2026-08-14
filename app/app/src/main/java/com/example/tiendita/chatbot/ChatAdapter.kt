package com.example.tiendita.chatbot

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.ChatMessage

/**
 * Adaptador que muestra la conversación como burbujas: alineadas a la derecha
 * para el usuario y a la izquierda para las respuestas del asistente.
 */
class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mensajes = mutableListOf<ChatMessage>()

    companion object {
        private const val TIPO_USUARIO = 1
        private const val TIPO_BOT = 2
    }

    fun agregarMensaje(mensaje: ChatMessage) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }

    /** Copia de los mensajes actuales, para usar como historial de contexto. */
    fun todosLosMensajes(): List<ChatMessage> = mensajes.toList()

    override fun getItemViewType(position: Int): Int =
        if (mensajes[position].role == "user") TIPO_USUARIO else TIPO_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutRes = if (viewType == TIPO_USUARIO) {
            R.layout.item_chat_usuario
        } else {
            R.layout.item_chat_bot
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.tvTextoMensaje).text = mensajes[position].texto
    }

    override fun getItemCount(): Int = mensajes.size
}
