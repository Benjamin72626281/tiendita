package com.example.tiendita.productos

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tiendita.R
import com.example.tiendita.model.Producto
import com.example.tiendita.util.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class ProductoFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCTO_ID = "extra_producto_id"
    }

    private val db = FirebaseFirestore.getInstance()
    private var productoId: String? = null

    private lateinit var etNombre: TextInputEditText
    private lateinit var etPrecioCompra: TextInputEditText
    private lateinit var etPrecioVenta: TextInputEditText
    private lateinit var etCantidad: TextInputEditText
    private lateinit var tvError: android.widget.TextView
    private lateinit var btnGuardar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_producto_form)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etNombre = findViewById(R.id.etNombre)
        etPrecioCompra = findViewById(R.id.etPrecioCompra)
        etPrecioVenta = findViewById(R.id.etPrecioVenta)
        etCantidad = findViewById(R.id.etCantidad)
        tvError = findViewById(R.id.tvErrorForm)
        btnGuardar = findViewById(R.id.btnGuardarProducto)

        productoId = intent.getStringExtra(EXTRA_PRODUCTO_ID)
        if (productoId != null) {
            title = getString(R.string.title_producto_form)
            cargarProducto(productoId!!)
        }

        btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarProducto(id: String) {
        db.collection(Constants.COLLECTION_PRODUCTOS).document(id).get()
            .addOnSuccessListener { doc ->
                val producto = doc.toObject(Producto::class.java) ?: return@addOnSuccessListener
                etNombre.setText(producto.nombre)
                etPrecioCompra.setText(producto.precioCompra.toString())
                etPrecioVenta.setText(producto.precioVenta.toString())
                etCantidad.setText(producto.cantidad.toString())
            }
    }

    private fun guardar() {
        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        val precioCompra = etPrecioCompra.text?.toString()?.trim()?.toDoubleOrNull()
        val precioVenta = etPrecioVenta.text?.toString()?.trim()?.toDoubleOrNull()
        val cantidad = etCantidad.text?.toString()?.trim()?.toLongOrNull()

        if (nombre.isEmpty() || precioCompra == null || precioVenta == null || cantidad == null
            || precioCompra < 0 || precioVenta < 0 || cantidad < 0
        ) {
            tvError.text = getString(R.string.error_campos_producto)
            tvError.visibility = View.VISIBLE
            return
        }
        tvError.visibility = View.GONE
        btnGuardar.isEnabled = false

        val producto = Producto(
            id = productoId.orEmpty(),
            nombre = nombre,
            precioCompra = precioCompra,
            precioVenta = precioVenta,
            cantidad = cantidad
        )

        val coleccion = db.collection(Constants.COLLECTION_PRODUCTOS)
        val tarea = if (productoId != null) {
            coleccion.document(productoId!!).set(producto.toMap())
        } else {
            coleccion.add(producto.toMap())
        }

        tarea.addOnSuccessListener { finish() }
            .addOnFailureListener {
                btnGuardar.isEnabled = true
                tvError.text = getString(R.string.error_campos_producto)
                tvError.visibility = View.VISIBLE
            }
    }
}
