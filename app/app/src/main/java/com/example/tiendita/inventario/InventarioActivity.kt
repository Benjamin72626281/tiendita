package com.example.tiendita.inventario

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.Producto
import com.example.tiendita.util.Constants
import com.example.tiendita.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Módulo Inventario - RF2 y RF4 (alerta de stock bajo).
 */
class InventarioActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private val yaNotificados = mutableSetOf<String>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSinInventario: android.widget.TextView
    private lateinit var adapter: InventarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rvInventario)
        tvSinInventario = findViewById(R.id.tvSinInventario)
        adapter = InventarioAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = db.collection(Constants.COLLECTION_PRODUCTOS)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Producto::class.java)?.apply { id = doc.id }
                }.orEmpty().sortedBy { it.nombre.lowercase() }

                adapter.actualizar(lista)
                tvSinInventario.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE

                // RF4: notificar productos con stock bajo (una sola vez por sesión de pantalla)
                lista.filter { it.cantidad <= Constants.UMBRAL_STOCK_BAJO }
                    .forEach { producto ->
                        if (yaNotificados.add(producto.id)) {
                            NotificationHelper.notificarStockBajo(
                                this, producto.nombre, producto.cantidad, producto.id.hashCode()
                            )
                        }
                    }
            }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }
}
