package com.example.tiendita.productos

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.Producto
import com.example.tiendita.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Módulo Productos (Registro) - RF5.
 * CRUD completo contra Firestore, con actualización en tiempo real.
 */
class ProductosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSinProductos: android.widget.TextView
    private lateinit var adapter: ProductoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rvProductos)
        tvSinProductos = findViewById(R.id.tvSinProductos)
        val fab = findViewById<FloatingActionButton>(R.id.fabAgregarProducto)

        adapter = ProductoAdapter(
            onEditar = { producto -> abrirFormulario(producto.id) },
            onEliminar = { producto -> confirmarEliminar(producto) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener { abrirFormulario(null) }
    }

    override fun onStart() {
        super.onStart()
        listenerRegistration = db.collection(Constants.COLLECTION_PRODUCTOS)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Producto::class.java)?.apply { id = doc.id }
                }.orEmpty().sortedBy { it.nombre.lowercase() }

                adapter.actualizar(lista)
                tvSinProductos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onStop() {
        super.onStop()
        listenerRegistration?.remove()
    }

    private fun abrirFormulario(productoId: String?) {
        val intent = Intent(this, ProductoFormActivity::class.java)
        productoId?.let { intent.putExtra(ProductoFormActivity.EXTRA_PRODUCTO_ID, it) }
        startActivity(intent)
    }

    private fun confirmarEliminar(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_eliminar_titulo)
            .setMessage(getString(R.string.confirmar_eliminar_mensaje, producto.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                db.collection(Constants.COLLECTION_PRODUCTOS).document(producto.id).delete()
            }
            .show()
    }
}
