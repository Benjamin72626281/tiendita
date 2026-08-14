package com.example.tiendita.util

object Constants {
    // Colecciones de Firestore
    const val COLLECTION_PRODUCTOS = "productos"
    const val COLLECTION_VENTAS = "ventas"
    const val COLLECTION_CORTES = "cortes"
    const val COLLECTION_USUARIOS = "usuarios"

    // RF4: Nivel de stock bajo para notificar
    const val UMBRAL_STOCK_BAJO = 5L

    // Roles de usuario
    const val ROL_VENDEDOR = "vendedor"
    const val ROL_CLIENTE = "cliente"
}
