package com.example.tiendita.model

import com.google.firebase.Timestamp
import com.example.tiendita.util.Constants

/**
 * RF3 (extendido): representa a cualquier persona con cuenta en el sistema,
 * ya sea el dueño/encargado (rol "vendedor") o un cliente que se registró
 * para comprar directamente desde la app (rol "cliente").
 *
 * El id del documento en Firestore es el mismo uid de Firebase Authentication.
 * Si un usuario autenticado NO tiene documento en "usuarios" (por ejemplo,
 * las cuentas del dueño creadas manualmente desde la consola de Firebase),
 * se le trata como "vendedor" por compatibilidad con cuentas existentes.
 */
data class Usuario(
    var uid: String = "",
    var nombre: String = "",
    var correo: String = "",
    var rol: String = Constants.ROL_VENDEDOR,
    var fechaRegistro: Timestamp = Timestamp.now()
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", Constants.ROL_VENDEDOR, Timestamp.now())

    fun esCliente(): Boolean = rol == Constants.ROL_CLIENTE

    fun toMap(): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "correo" to correo,
        "rol" to rol,
        "fechaRegistro" to fechaRegistro
    )
}
