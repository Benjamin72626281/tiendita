package com.example.tiendita.model

/**
 * RF5: Registro de productos con nombre, precio de compra, precio de venta y cantidad disponible.
 */
data class Producto(
    var id: String = "",
    var nombre: String = "",
    var precioCompra: Double = 0.0,
    var precioVenta: Double = 0.0,
    var cantidad: Long = 0L
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", 0.0, 0.0, 0L)

    fun toMap(): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "precioCompra" to precioCompra,
        "precioVenta" to precioVenta,
        "cantidad" to cantidad
    )
}
