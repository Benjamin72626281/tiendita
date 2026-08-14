package com.example.tiendita.ventas

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.carrito.CarritoAdapter
import com.example.tiendita.model.ItemCarrito
import com.example.tiendita.model.Producto
import com.example.tiendita.model.Venta
import com.example.tiendita.util.Constants
import com.example.tiendita.util.MoneyUtil
import com.example.tiendita.util.NotificationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

/**
 * Módulo Ventas - RF1 (registro de ventas) y RF2 (actualización de inventario).
 *
 * El vendedor elige un producto en un menú desplegable, indica la cantidad
 * con el selector +/-, y lo agrega al carrito. Puede repetir esto con varios
 * productos y, al final, cobra todo junto con un solo botón.
 */
class VentasActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var productosListener: ListenerRegistration? = null

    private var productos: List<Producto> = emptyList()
    private val carrito = LinkedHashMap<String, Int>() // productoId -> cantidad, conserva orden de agregado

    private var productoSeleccionado: Producto? = null
    private var cantidadSeleccionada: Int = 1
    private var opcionesPorTexto: Map<String, Producto> = emptyMap()

    private lateinit var tvSinProductos: TextView
    private lateinit var cardAgregarProducto: androidx.cardview.widget.CardView
    private lateinit var dropdownProducto: MaterialAutoCompleteTextView
    private lateinit var tvStockSeleccionado: TextView
    private lateinit var tvCantidadSeleccionada: TextView
    private lateinit var btnMenosCantidad: ImageButton
    private lateinit var btnMasCantidad: ImageButton
    private lateinit var btnAgregarCarrito: MaterialButton

    private lateinit var rowTituloCarrito: View
    private lateinit var tvBadgeCarrito: TextView
    private lateinit var llCarritoVacio: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CarritoAdapter

    private lateinit var tvResumen: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvError: TextView
    private lateinit var btnCobrar: MaterialButton

    private var cobrando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventas)

        findViewById<android.widget.ImageButton>(R.id.btnAtrasVentas).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvSinProductos = findViewById(R.id.tvSinProductosVentas)
        cardAgregarProducto = findViewById(R.id.cardAgregarProducto)
        dropdownProducto = findViewById(R.id.dropdownProducto)
        tvStockSeleccionado = findViewById(R.id.tvStockSeleccionado)
        tvCantidadSeleccionada = findViewById(R.id.tvCantidadSeleccionada)
        btnMenosCantidad = findViewById(R.id.btnMenosCantidad)
        btnMasCantidad = findViewById(R.id.btnMasCantidad)
        btnAgregarCarrito = findViewById(R.id.btnAgregarCarrito)

        rowTituloCarrito = findViewById(R.id.rowTituloCarrito)
        tvBadgeCarrito = findViewById(R.id.tvBadgeCarrito)
        llCarritoVacio = findViewById(R.id.llCarritoVacio)
        recyclerView = findViewById(R.id.rvCarrito)
        tvResumen = findViewById(R.id.tvResumenVenta)
        tvTotal = findViewById(R.id.tvTotalVenta)
        tvError = findViewById(R.id.tvErrorVenta)
        btnCobrar = findViewById(R.id.btnCobrar)

        adapter = CarritoAdapter { productoId ->
            carrito.remove(productoId)
            refrescarCarrito()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        dropdownProducto.setOnItemClickListener { _, _, position, _ ->
            val texto = dropdownProducto.adapter.getItem(position) as String
            seleccionarProducto(opcionesPorTexto[texto])
        }

        btnMenosCantidad.setOnClickListener { cambiarCantidadSeleccionada(-1) }
        btnMasCantidad.setOnClickListener { cambiarCantidadSeleccionada(1) }
        btnAgregarCarrito.setOnClickListener { agregarProductoAlCarrito() }
        btnCobrar.setOnClickListener { confirmarCobro() }

        actualizarControlesSeleccion()
        refrescarCarrito()
        escucharProductos()
    }

    override fun onDestroy() {
        super.onDestroy()
        productosListener?.remove()
    }

    private fun escucharProductos() {
        productosListener = db.collection(Constants.COLLECTION_PRODUCTOS)
            .addSnapshotListener { snapshot, _ ->
                productos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Producto::class.java)?.apply { id = doc.id }
                }.orEmpty().sortedBy { it.nombre.lowercase() }

                val hayProductos = productos.isNotEmpty()
                tvSinProductos.visibility = if (hayProductos) View.GONE else View.VISIBLE
                cardAgregarProducto.visibility = if (hayProductos) View.VISIBLE else View.GONE
                rowTituloCarrito.visibility = if (hayProductos) View.VISIBLE else View.GONE

                construirOpcionesDropdown()

                // Si el producto elegido cambió de stock o ya no existe, se vuelve a validar.
                productoSeleccionado?.let { actual ->
                    val actualizado = productos.find { it.id == actual.id }
                    seleccionarProducto(actualizado)
                }

                // Ajusta el carrito si algún producto perdió stock o fue eliminado.
                val idsValidos = productos.associateBy { it.id }
                carrito.keys.toList().forEach { id ->
                    val stock = idsValidos[id]?.cantidad?.toInt() ?: 0
                    if (stock <= 0) {
                        carrito.remove(id)
                    } else if ((carrito[id] ?: 0) > stock) {
                        carrito[id] = stock
                    }
                }

                refrescarCarrito()
            }
    }

    private fun construirOpcionesDropdown() {
        val mapa = LinkedHashMap<String, Producto>()
        val opciones = productos.map { producto ->
            val texto = getString(
                R.string.producto_opcion_formato,
                producto.nombre,
                MoneyUtil.format(producto.precioVenta),
                producto.cantidad.toInt()
            )
            mapa[texto] = producto
            texto
        }
        opcionesPorTexto = mapa
        val adapterDropdown = ArrayAdapter(this, android.R.layout.simple_list_item_1, opciones)
        dropdownProducto.setAdapter(adapterDropdown)
    }

    private fun seleccionarProducto(producto: Producto?) {
        productoSeleccionado = producto
        if (producto == null) {
            dropdownProducto.setText("", false)
            tvStockSeleccionado.visibility = View.GONE
            cantidadSeleccionada = 1
            actualizarControlesSeleccion()
            return
        }

        val yaEnCarrito = carrito[producto.id] ?: 0
        val stockRestante = (producto.cantidad.toInt() - yaEnCarrito).coerceAtLeast(0)

        tvStockSeleccionado.visibility = View.VISIBLE
        tvStockSeleccionado.text = if (stockRestante > 0) {
            getString(R.string.disponibles_formato, stockRestante)
        } else {
            getString(R.string.sin_stock_disponible)
        }

        cantidadSeleccionada = if (stockRestante > 0) 1 else 0
        actualizarControlesSeleccion()
    }

    private fun stockRestanteDe(producto: Producto): Int {
        val yaEnCarrito = carrito[producto.id] ?: 0
        return (producto.cantidad.toInt() - yaEnCarrito).coerceAtLeast(0)
    }

    private fun cambiarCantidadSeleccionada(delta: Int) {
        val producto = productoSeleccionado ?: return
        val stockRestante = stockRestanteDe(producto)
        val nueva = cantidadSeleccionada + delta
        if (nueva < 1) return
        if (nueva > stockRestante) {
            Toast.makeText(this, R.string.stock_maximo_alcanzado, Toast.LENGTH_SHORT).show()
            return
        }
        cantidadSeleccionada = nueva
        actualizarControlesSeleccion()
    }

    private fun actualizarControlesSeleccion() {
        tvCantidadSeleccionada.text = cantidadSeleccionada.toString()
        val producto = productoSeleccionado
        val stockRestante = producto?.let { stockRestanteDe(it) } ?: 0

        btnMenosCantidad.isEnabled = producto != null && cantidadSeleccionada > 1
        btnMasCantidad.isEnabled = producto != null && cantidadSeleccionada < stockRestante
        btnAgregarCarrito.isEnabled = producto != null && stockRestante > 0 && cantidadSeleccionada in 1..stockRestante
    }

    private fun agregarProductoAlCarrito() {
        val producto = productoSeleccionado
        if (producto == null) {
            Toast.makeText(this, R.string.elige_producto_primero, Toast.LENGTH_SHORT).show()
            return
        }
        val stockRestante = stockRestanteDe(producto)
        if (stockRestante <= 0 || cantidadSeleccionada <= 0) return

        val cantidadAAgregar = cantidadSeleccionada.coerceAtMost(stockRestante)
        carrito[producto.id] = (carrito[producto.id] ?: 0) + cantidadAAgregar

        // Limpia la selección para elegir el siguiente producto.
        productoSeleccionado = null
        cantidadSeleccionada = 1
        dropdownProducto.setText("", false)
        tvStockSeleccionado.visibility = View.GONE
        actualizarControlesSeleccion()

        refrescarCarrito()
    }

    private fun itemsCarritoActuales(): List<ItemCarrito> {
        return carrito.mapNotNull { (productoId, cantidad) ->
            val producto = productos.find { it.id == productoId } ?: return@mapNotNull null
            if (cantidad <= 0) return@mapNotNull null
            ItemCarrito(producto, cantidad)
        }
    }

    private fun refrescarCarrito() {
        val items = itemsCarritoActuales()
        adapter.actualizar(items)

        val hayItems = items.isNotEmpty()
        llCarritoVacio.visibility = if (hayItems) View.GONE else View.VISIBLE
        recyclerView.visibility = if (hayItems) View.VISIBLE else View.GONE

        val totalArticulos = items.sumOf { it.cantidad }
        val total = items.sumOf { it.subtotal }

        tvBadgeCarrito.visibility = if (hayItems) View.VISIBLE else View.GONE
        tvBadgeCarrito.text = getString(R.string.articulos_formato, totalArticulos)
        tvResumen.text = getString(R.string.articulos_formato, totalArticulos)
        tvTotal.text = MoneyUtil.format(total)
        if (!cobrando) {
            btnCobrar.isEnabled = hayItems
        }
    }

    private fun confirmarCobro() {
        val items = itemsCarritoActuales()
        if (items.isEmpty()) return

        val resumen = items.joinToString("\n") { item ->
            "• ${item.cantidad} x ${item.producto.nombre} = ${MoneyUtil.format(item.subtotal)}"
        }
        val total = items.sumOf { it.subtotal }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_cobro_titulo)
            .setMessage(getString(R.string.confirmar_compra_mensaje, resumen, MoneyUtil.format(total)))
            .setPositiveButton(R.string.btn_cobrar) { _, _ -> cobrarVenta(items) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun cobrarVenta(items: List<ItemCarrito>) {
        cobrando = true
        btnCobrar.isEnabled = false
        tvError.visibility = View.GONE

        val pedidoId = UUID.randomUUID().toString()
        val fechaVenta = Timestamp.now()

        db.runTransaction { transaction ->
            // 1) Se leen TODOS los productos primero (Firestore exige que las
            // lecturas de una transacción ocurran antes que cualquier escritura).
            val snapshots = items.map { item ->
                transaction.get(db.collection(Constants.COLLECTION_PRODUCTOS).document(item.producto.id))
            }

            // 2) Se valida que siga habiendo stock suficiente de cada producto.
            items.forEachIndexed { index, item ->
                val stockActual = snapshots[index].getLong("cantidad") ?: 0L
                if (item.cantidad > stockActual) {
                    throw StockInsuficienteException(item.producto.nombre, stockActual)
                }
            }

            // 3) Se descuenta el stock y se registra una venta por cada producto,
            // todas con el mismo pedidoId para saber que fueron cobradas juntas.
            val nuevosStocks = mutableMapOf<String, Long>()
            items.forEachIndexed { index, item ->
                val stockActual = snapshots[index].getLong("cantidad") ?: 0L
                val nuevoStock = stockActual - item.cantidad
                nuevosStocks[item.producto.id] = nuevoStock
                transaction.update(
                    db.collection(Constants.COLLECTION_PRODUCTOS).document(item.producto.id),
                    "cantidad", nuevoStock
                )
                val venta = Venta(
                    productoId = item.producto.id,
                    productoNombre = item.producto.nombre,
                    cantidad = item.cantidad.toLong(),
                    precioUnitario = item.producto.precioVenta,
                    montoTotal = item.subtotal,
                    fecha = fechaVenta,
                    pedidoId = pedidoId
                )
                val ventaRef = db.collection(Constants.COLLECTION_VENTAS).document()
                transaction.set(ventaRef, venta.toMap())
            }
            nuevosStocks
        }.addOnSuccessListener { nuevosStocks ->
            cobrando = false
            carrito.clear()
            refrescarCarrito()
            Toast.makeText(this, R.string.venta_exitosa, Toast.LENGTH_SHORT).show()

            // RF4: notificar si el stock resultante de algún producto quedó bajo
            items.forEach { item ->
                val nuevoStock = nuevosStocks[item.producto.id] ?: return@forEach
                if (nuevoStock <= Constants.UMBRAL_STOCK_BAJO) {
                    NotificationHelper.notificarStockBajo(this, item.producto.nombre, nuevoStock, item.producto.id.hashCode())
                }
            }
        }.addOnFailureListener { error ->
            cobrando = false
            btnCobrar.isEnabled = itemsCarritoActuales().isNotEmpty()
            val mensaje = if (error is StockInsuficienteException) {
                getString(R.string.error_stock_insuficiente, error.stockDisponible)
            } else {
                getString(R.string.error_cobro)
            }
            mostrarError(mensaje)
        }
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private class StockInsuficienteException(val nombreProducto: String, val stockDisponible: Long) :
        Exception("Stock insuficiente para $nombreProducto")
}
