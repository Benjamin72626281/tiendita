package com.example.tiendita.chatbot.ai

import com.example.tiendita.model.ChatMessage
import com.example.tiendita.model.CorteCaja
import com.example.tiendita.model.Producto
import com.example.tiendita.model.Venta
import com.example.tiendita.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Arma un resumen en texto de los datos reales de la tienda (productos, ventas
 * recientes y cortes de caja) guardados en Firestore, y se lo pasa como contexto
 * al proveedor de IA para que pueda responder preguntas concretas sobre el negocio.
 *
 * Si el proveedor principal (Gemini) falla por cualquier motivo -sin cuota, sin
 * red, error del servidor-, reintenta automáticamente con el proveedor de
 * respaldo (Groq), para que el chat casi nunca se quede sin responder.
 */
class ChatRepository(
    private val proveedorPrincipal: AiProvider = GeminiProvider(),
    private val proveedorRespaldo: AiProvider = GroqProvider()
) {

    private val db = FirebaseFirestore.getInstance()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))

    private suspend fun construirContexto(): String {
        val productos = db.collection(Constants.COLLECTION_PRODUCTOS)
            .get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Producto::class.java)?.apply { id = doc.id } }
            .sortedBy { it.nombre.lowercase() }

        val ventasRecientes = db.collection(Constants.COLLECTION_VENTAS)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(30)
            .get().await()
            .documents
            .mapNotNull { it.toObject(Venta::class.java) }

        val cortesRecientes = db.collection(Constants.COLLECTION_CORTES)
            .orderBy("fechaCierre", Query.Direction.DESCENDING)
            .limit(5)
            .get().await()
            .documents
            .mapNotNull { it.toObject(CorteCaja::class.java) }

        val sb = StringBuilder()

        sb.append("== PRODUCTOS EN INVENTARIO (${productos.size}) ==\n")
        if (productos.isEmpty()) {
            sb.append("No hay productos registrados todavía.\n")
        } else {
            productos.forEach { p ->
                sb.append(
                    "- ${p.nombre}: precio compra $${p.precioCompra}, precio venta $${p.precioVenta}, " +
                        "cantidad disponible ${p.cantidad}" +
                        (if (p.cantidad <= Constants.UMBRAL_STOCK_BAJO) " (STOCK BAJO)" else "") + "\n"
                )
            }
        }

        sb.append("\n== ÚLTIMAS VENTAS (máx. 30 más recientes) ==\n")
        if (ventasRecientes.isEmpty()) {
            sb.append("No hay ventas registradas todavía.\n")
        } else {
            ventasRecientes.forEach { v ->
                val fecha = formatoFecha.format(v.fecha.toDate())
                sb.append("- $fecha: ${v.cantidad} x ${v.productoNombre} = $${v.montoTotal}\n")
            }
        }

        sb.append("\n== ÚLTIMOS CORTES DE CAJA (máx. 5 más recientes) ==\n")
        if (cortesRecientes.isEmpty()) {
            sb.append("No hay cortes de caja guardados todavía.\n")
        } else {
            cortesRecientes.forEach { c ->
                val fecha = formatoFecha.format(c.fechaCierre.toDate())
                sb.append(
                    "- Corte del $fecha por ${c.usuario}: total $${c.totalVentas}, " +
                        "${c.numeroVentas} ventas, ${c.totalArticulosVendidos} artículos\n"
                )
            }
        }

        return sb.toString()
    }

    private fun construirSystemPrompt(contexto: String): String = """
        Eres el asistente virtual de "Tiendita", una app para administrar una tienda de abarrotes.
        Respondes siempre en español, de forma breve, clara y amigable, como si hablaras con el
        dueño de la tienda. Usa ÚNICAMENTE los datos de la tienda que se muestran abajo para
        responder preguntas sobre productos, precios, existencias, ventas y cortes de caja.
        Puedes hacer cálculos simples (sumas, promedios, comparaciones) a partir de esos datos.
        Si te preguntan algo que no está en estos datos, dilo honestamente en vez de inventar
        información.

        DATOS ACTUALES DE LA TIENDA:
        $contexto
    """.trimIndent()

    /**
     * Envía la pregunta del usuario al proveedor principal; si falla, reintenta con el respaldo.
     * @return Pair(respuesta, nombreDelProveedorQueRespondio)
     */
    suspend fun preguntar(historial: List<ChatMessage>, mensajeUsuario: String): Pair<String, String> {
        val systemPrompt = construirSystemPrompt(construirContexto())

        return try {
            proveedorPrincipal.generar(systemPrompt, historial, mensajeUsuario) to proveedorPrincipal.nombre
        } catch (errorPrincipal: Exception) {
            try {
                proveedorRespaldo.generar(systemPrompt, historial, mensajeUsuario) to proveedorRespaldo.nombre
            } catch (errorRespaldo: Exception) {
                throw Exception(
                    "No se pudo obtener respuesta de ningún proveedor de IA.\n" +
                        "Gemini: ${errorPrincipal.message}\nGroq: ${errorRespaldo.message}"
                )
            }
        }
    }
}
