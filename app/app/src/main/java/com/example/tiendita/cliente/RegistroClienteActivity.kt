package com.example.tiendita.cliente

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tiendita.R
import com.example.tiendita.model.Usuario
import com.example.tiendita.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Permite que un cliente cree su propia cuenta para comprar directo desde la
 * app. El nombre que registra aquí es el mismo que después aparece en su
 * tiquet de compra y en el detalle de ventas del corte de caja.
 */
class RegistroClienteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etNombre: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmar: TextInputEditText
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var progress: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvYaTengoCuenta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_cliente)

        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmailRegistro)
        etPassword = findViewById(R.id.etPasswordRegistro)
        etConfirmar = findViewById(R.id.etConfirmarPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progress = findViewById(R.id.progressRegistro)
        tvError = findViewById(R.id.tvRegistroError)
        tvYaTengoCuenta = findViewById(R.id.tvYaTengoCuenta)

        btnRegistrar.setOnClickListener { intentarRegistro() }
        tvYaTengoCuenta.setOnClickListener { finish() }
    }

    private fun intentarRegistro() {
        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()
        val confirmar = etConfirmar.text?.toString()?.trim().orEmpty()

        if (nombre.isEmpty()) {
            mostrarError(getString(R.string.error_nombre_vacio))
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError(getString(R.string.error_correo_invalido))
            return
        }
        if (password.length < 6) {
            mostrarError(getString(R.string.error_password_corta))
            return
        }
        if (password != confirmar) {
            mostrarError(getString(R.string.error_password_no_coincide))
            return
        }

        mostrarCargando(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrarCargando(false)
                    mostrarError(getString(R.string.error_registro))
                    return@addOnSuccessListener
                }
                val usuario = Usuario(
                    uid = uid,
                    nombre = nombre,
                    correo = email,
                    rol = Constants.ROL_CLIENTE
                )
                db.collection(Constants.COLLECTION_USUARIOS).document(uid)
                    .set(usuario.toMap())
                    .addOnSuccessListener {
                        mostrarCargando(false)
                        irATiendaCliente(nombre)
                    }
                    .addOnFailureListener {
                        mostrarCargando(false)
                        mostrarError(getString(R.string.error_registro))
                    }
            }
            .addOnFailureListener {
                mostrarCargando(false)
                mostrarError(getString(R.string.error_correo_en_uso))
            }
    }

    private fun mostrarCargando(cargando: Boolean) {
        progress.visibility = if (cargando) View.VISIBLE else View.GONE
        btnRegistrar.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private fun irATiendaCliente(nombreCliente: String) {
        val intent = Intent(this, ClienteTiendaActivity::class.java)
        intent.putExtra(ClienteTiendaActivity.EXTRA_NOMBRE_CLIENTE, nombreCliente)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
