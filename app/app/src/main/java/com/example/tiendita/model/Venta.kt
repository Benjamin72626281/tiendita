package com.example.tiendita.model

import com.google.firebase.Timestamp

/**
 * RF1: Control de ventas por producto, cantidad, fecha y monto total.
 */
data class Venta(
    var id: String = "",
    var productoId: String = "",
    var productoNombre: String = "",
    var cantidad: Long = 0L,
    var precioUnitario: Double = 0.0,
    var montoTotal: Double = 0.0,
    var fecha: Timestamp = Timestamp.now(),
    // Se llenan solo cuando la venta la hizo un cliente comprando directo
    // desde su cuenta (RF: compras de clientes). Quedan vacíos en las
    // ventas que registra el vendedor manualmente, como hasta ahora.
    var clienteNombre: String = "",
    var pedidoId: String = ""
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", 0L, 0.0, 0.0, Timestamp.now(), "", "")

    fun esDeCliente(): Boolean = clienteNombre.isNotBlank()

    fun toMap(): Map<String, Any> = mapOf(
        "productoId" to productoId,
        "productoNombre" to productoNombre,
        "cantidad" to cantidad,
        "precioUnitario" to precioUnitario,
        "montoTotal" to montoTotal,
        "fecha" to fecha,
        "clienteNombre" to clienteNombre,
        "pedidoId" to pedidoId
    )
}
