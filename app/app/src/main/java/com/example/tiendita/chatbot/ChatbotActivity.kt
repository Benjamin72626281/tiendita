package com.example.tiendita.chatbot

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.chatbot.ai.ChatRepository
import com.example.tiendita.model.ChatMessage
import kotlinx.coroutines.launch

/**
 * Chat con el asistente de IA de la tienda: responde preguntas sobre productos,
 * precios, existencias, ventas y cortes de caja usando los datos reales de Firestore.
 * Usa Gemini como proveedor principal y Groq como respaldo automático.
 */
class ChatbotActivity : AppCompatActivity() {

    private val repositorio = ChatRepository()
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rvChat)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        progressBar = findViewById(R.id.progressBarChat)

        adapter = ChatAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.agregarMensaje(ChatMessage(role = "model", texto = getString(R.string.chatbot_bienvenida)))

        btnEnviar.setOnClickListener { enviarMensaje() }
    }

    private fun enviarMensaje() {
        val texto = etMensaje.text.toString().trim()
        if (texto.isEmpty()) return

        val historialPrevio = adapter.todosLosMensajes()
        adapter.agregarMensaje(ChatMessage(role = "user", texto = texto))
        etMensaje.text.clear()
        recyclerView.scrollToPosition(adapter.itemCount - 1)
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                // Se manda solo el historial reciente para no crecer el prompt sin límite.
                val (respuesta, _) = repositorio.preguntar(
                    historial = historialPrevio.takeLast(12),
                    mensajeUsuario = texto
                )
                adapter.agregarMensaje(ChatMessage(role = "model", texto = respuesta))
            } catch (e: Exception) {
                adapter.agregarMensaje(
                    ChatMessage(role = "model", texto = getString(R.string.chatbot_error, e.message ?: ""))
                )
            } finally {
                mostrarCargando(false)
                recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnEnviar.isEnabled = !mostrar
    }
}
