package com.example.tiendita.model

/**
 * Representa un producto ya agregado al carrito, junto con la cantidad
 * elegida. Se usa tanto en el carrito del vendedor (VentasActivity) como
 * en la tienda del cliente (ClienteTiendaActivity).
 */
data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int
) {
    val subtotal: Double
        get() = producto.precioVenta * cantidad
}
