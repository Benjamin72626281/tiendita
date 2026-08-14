package com.example.tiendita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tiendita.caja.CorteCajaActivity
import com.example.tiendita.chatbot.ChatbotActivity
import com.example.tiendita.inventario.InventarioActivity
import com.example.tiendita.productos.ProductosActivity
import com.example.tiendita.ventas.VentasActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<android.view.View>(R.id.cardProductos).setOnClickListener {
            startActivity(Intent(this, ProductosActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardVentas).setOnClickListener {
            startActivity(Intent(this, VentasActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardInventario).setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardCorteCaja).setOnClickListener {
            startActivity(Intent(this, CorteCajaActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardChatbot).setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnCerrarSesion).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        pedirPermisoNotificaciones()
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permisoConcedido = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permisoConcedido) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
