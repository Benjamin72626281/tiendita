package com.example.tiendita

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import android.widget.ProgressBar
import android.widget.TextView
import com.example.tiendita.cliente.ClienteTiendaActivity
import com.example.tiendita.cliente.RegistroClienteActivity
import com.example.tiendita.model.Usuario
import com.example.tiendita.util.Constants
import com.google.firebase.firestore.FirebaseFirestore

/**
 * RF3: Control de acceso al sistema mediante inicio de sesión.
 * Según el rol guardado en Firestore ("vendedor" o "cliente"), redirige al
 * panel de administración o a la tienda de compra del cliente.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressLogin: ProgressBar
    private lateinit var tvLoginError: TextView
    private lateinit var tvCrearCuenta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)
        tvLoginError = findViewById(R.id.tvLoginError)
        tvCrearCuenta = findViewById(R.id.tvCrearCuenta)

        btnLogin.setOnClickListener { intentarLogin() }
        tvCrearCuenta.setOnClickListener {
            startActivity(android.content.Intent(this, RegistroClienteActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Si ya había una sesión activa, se resuelve su rol y se navega directo.
        auth.currentUser?.let { resolverRolYNavegar(it.uid) }
    }

    private fun intentarLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || password.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError(getString(R.string.error_campos_vacios))
            return
        }

        mostrarCargando(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrarCargando(false)
                    mostrarError(getString(R.string.error_login))
                } else {
                    resolverRolYNavegar(uid)
                }
            }
            .addOnFailureListener {
                mostrarCargando(false)
                mostrarError(getString(R.string.error_login))
            }
    }

    /**
     * Busca el documento del usuario en la colección "usuarios" para saber su
     * rol. Si no existe (cuentas del vendedor creadas manualmente en Firebase
     * antes de que existiera este sistema de roles), se asume "vendedor" para
     * no romper el acceso del dueño/encargado de la tienda.
     */
    private fun resolverRolYNavegar(uid: String) {
        mostrarCargando(true)
        db.collection(Constants.COLLECTION_USUARIOS).document(uid).get()
            .addOnSuccessListener { doc ->
                mostrarCargando(false)
                val usuario = doc.toObject(Usuario::class.java)
                if (usuario != null && usuario.esCliente()) {
                    irATiendaCliente(usuario.nombre)
                } else {
                    irADashboard()
                }
            }
            .addOnFailureListener {
                // Si falla la consulta (p. ej. sin conexión), se deja pasar
                // como vendedor para no bloquear el acceso al dueño de la tienda.
                mostrarCargando(false)
                irADashboard()
            }
    }

    private fun mostrarCargando(cargando: Boolean) {
        progressLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !cargando
    }

    private fun mostrarError(mensaje: String) {
        tvLoginError.text = mensaje
        tvLoginError.visibility = View.VISIBLE
    }

    private fun irADashboard() {
        startActivity(android.content.Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun irATiendaCliente(nombreCliente: String) {
        val intent = android.content.Intent(this, ClienteTiendaActivity::class.java)
        intent.putExtra(ClienteTiendaActivity.EXTRA_NOMBRE_CLIENTE, nombreCliente)
        startActivity(intent)
        finish()
    }
}
