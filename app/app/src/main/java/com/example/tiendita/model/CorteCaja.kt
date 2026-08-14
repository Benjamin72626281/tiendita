package com.example.tiendita.model

import com.google.firebase.Timestamp

/**
 * RF6/RF7: Registro histórico de cortes de caja.
 * Cada corte guarda el resumen (total, número de ventas, artículos vendidos)
 * y el detalle de todas las ventas incluidas en el periodo cerrado.
 */
data class CorteCaja(
    var id: String = "",
    var fechaApertura: Timestamp = Timestamp.now(),
    var fechaCierre: Timestamp = Timestamp.now(),
    var totalVentas: Double = 0.0,
    var numeroVentas: Long = 0L,
    var totalArticulosVendidos: Long = 0L,
    var usuario: String = "",
    var detalle: List<Map<String, Any>> = emptyList()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", Timestamp.now(), Timestamp.now(), 0.0, 0L, 0L, "", emptyList())

    fun toMap(): Map<String, Any> = mapOf(
        "fechaApertura" to fechaApertura,
        "fechaCierre" to fechaCierre,
        "totalVentas" to totalVentas,
        "numeroVentas" to numeroVentas,
        "totalArticulosVendidos" to totalArticulosVendidos,
        "usuario" to usuario,
        "detalle" to detalle
    )
}
