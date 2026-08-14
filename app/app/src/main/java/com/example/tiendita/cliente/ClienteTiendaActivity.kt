package com.example.tiendita.cliente

import android.content.Intent
import android.net.Uri
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
import com.example.tiendita.MainActivity
import com.example.tiendita.R
import com.example.tiendita.carrito.CarritoAdapter
import com.example.tiendita.model.ItemCarrito
import com.example.tiendita.model.Producto
import com.example.tiendita.model.Venta
import com.example.tiendita.util.Constants
import com.example.tiendita.util.MoneyUtil
import com.example.tiendita.util.PdfUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

/**
 * Pantalla de compra para clientes: eligen un producto en un menú desplegable,
 * indican cuántas unidades quieren con el selector +/- y lo agregan al
 * carrito. Pueden repetir esto con varios productos y pagar todo junto al
 * final. Al pagar se genera un tiquet en PDF con su nombre y los productos
 * comprados, y las ventas quedan guardadas con su nombre para que aparezcan
 * también en el corte de caja del vendedor.
 */
class ClienteTiendaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMBRE_CLIENTE = "extra_nombre_cliente"
    }

    private val db = FirebaseFirestore.getInstance()
    private var productosListener: ListenerRegistration? = null

    private lateinit var nombreCliente: String
    private var productos: List<Producto> = emptyList()
    private val carrito = LinkedHashMap<String, Int>() // productoId -> cantidad, conserva orden de agregado

    private var productoSeleccionado: Producto? = null
    private var cantidadSeleccionada: Int = 1
    private var opcionesPorTexto: Map<String, Producto> = emptyMap()

    private lateinit var tvSaludo: TextView
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

    private lateinit var tvResumenCarrito: TextView
    private lateinit var tvTotalCarrito: TextView
    private lateinit var btnPagar: MaterialButton
    private lateinit var btnCerrarSesion: MaterialButton

    private var comprando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente_tienda)

        nombreCliente = intent.getStringExtra(EXTRA_NOMBRE_CLIENTE).orEmpty()

        tvSaludo = findViewById(R.id.tvSaludoCliente)
        tvSinProductos = findViewById(R.id.tvSinProductosCliente)
        cardAgregarProducto = findViewById(R.id.cardAgregarProductoCliente)
        dropdownProducto = findViewById(R.id.dropdownProductoCliente)
        tvStockSeleccionado = findViewById(R.id.tvStockSeleccionadoCliente)
        tvCantidadSeleccionada = findViewById(R.id.tvCantidadSeleccionadaCliente)
        btnMenosCantidad = findViewById(R.id.btnMenosCantidadCliente)
        btnMasCantidad = findViewById(R.id.btnMasCantidadCliente)
        btnAgregarCarrito = findViewById(R.id.btnAgregarCarritoCliente)

        rowTituloCarrito = findViewById(R.id.rowTituloCarritoCliente)
        tvBadgeCarrito = findViewById(R.id.tvBadgeCarritoCliente)
        llCarritoVacio = findViewById(R.id.llCarritoVacioCliente)
        recyclerView = findViewById(R.id.rvCarritoCliente)
        tvResumenCarrito = findViewById(R.id.tvResumenCarrito)
        tvTotalCarrito = findViewById(R.id.tvTotalCarrito)
        btnPagar = findViewById(R.id.btnPagar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionCliente)

        tvSaludo.text = getString(R.string.saludo_cliente_formato, nombreCliente)

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
        btnPagar.setOnClickListener { confirmarCompra() }
        btnCerrarSesion.setOnClickListener { cerrarSesion() }

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

                productoSeleccionado?.let { actual ->
                    val actualizado = productos.find { it.id == actual.id }
                    seleccionarProducto(actualizado)
                }

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
        tvResumenCarrito.text = getString(R.string.articulos_formato, totalArticulos)
        tvTotalCarrito.text = MoneyUtil.format(total)
        if (!comprando) {
            btnPagar.isEnabled = hayItems
        }
    }

    private fun confirmarCompra() {
        val items = itemsCarritoActuales()
        if (items.isEmpty()) return

        val resumen = items.joinToString("\n") { item ->
            "• ${item.cantidad} x ${item.producto.nombre} = ${MoneyUtil.format(item.subtotal)}"
        }
        val total = items.sumOf { it.subtotal }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_compra_titulo)
            .setMessage(getString(R.string.confirmar_compra_mensaje, resumen, MoneyUtil.format(total)))
            .setPositiveButton(R.string.btn_pagar) { _, _ -> realizarCompra(items) }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun realizarCompra(items: List<ItemCarrito>) {
        comprando = true
        btnPagar.isEnabled = false

        val pedidoId = UUID.randomUUID().toString()
        val fechaCompra = Timestamp.now()

        db.runTransaction { transaction ->
            // 1) Se leen TODOS los productos primero (Firestore exige que las
            // lecturas de una transacción ocurran antes que cualquier escritura).
            val snapshots = items.map { item ->
                transaction.get(db.collection(Constants.COLLECTION_PRODUCTOS).document(item.producto.id))
            }

            // 2) Se valida que siga habiendo stock suficiente de cada producto
            // (por si alguien más compró justo antes).
            items.forEachIndexed { index, item ->
                val stockActual = snapshots[index].getLong("cantidad") ?: 0L
                if (item.cantidad > stockActual) {
                    throw StockInsuficienteException(item.producto.nombre, stockActual)
                }
            }

            // 3) Se descuenta el stock y se registra una venta por cada
            // producto, todas con el mismo pedidoId y el nombre del cliente.
            items.forEachIndexed { index, item ->
                val stockActual = snapshots[index].getLong("cantidad") ?: 0L
                val nuevoStock = stockActual - item.cantidad
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
                    fecha = fechaCompra,
                    clienteNombre = nombreCliente,
                    pedidoId = pedidoId
                )
                val ventaRef = db.collection(Constants.COLLECTION_VENTAS).document()
                transaction.set(ventaRef, venta.toMap())
            }
            null
        }.addOnSuccessListener {
            comprando = false
            carrito.clear()
            refrescarCarrito()

            val ventasParaTicket = items.map { item ->
                Venta(
                    productoId = item.producto.id,
                    productoNombre = item.producto.nombre,
                    cantidad = item.cantidad.toLong(),
                    precioUnitario = item.producto.precioVenta,
                    montoTotal = item.subtotal,
                    fecha = fechaCompra,
                    clienteNombre = nombreCliente,
                    pedidoId = pedidoId
                )
            }
            val total = items.sumOf { it.subtotal }
            val uri = try {
                PdfUtil.generarTicketPdf(this, nombreCliente, ventasParaTicket, total, fechaCompra)
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                mostrarDialogoCompraExitosa(uri)
            } else {
                Toast.makeText(this, getString(R.string.compra_exitosa_sin_ticket), Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { error ->
            comprando = false
            btnPagar.isEnabled = itemsCarritoActuales().isNotEmpty()
            val mensaje = if (error is StockInsuficienteException) {
                getString(R.string.error_stock_insuficiente_cliente, error.nombreProducto, error.stockDisponible)
            } else {
                getString(R.string.error_compra)
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoCompraExitosa(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.compra_exitosa_titulo)
            .setMessage(R.string.compra_exitosa_mensaje)
            .setPositiveButton(R.string.btn_ver_ticket) { _, _ -> abrirTicket(uri) }
            .setNegativeButton(R.string.cerrar, null)
            .show()
    }

    private fun abrirTicket(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_abrir_pdf), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private class StockInsuficienteException(val nombreProducto: String, val stockDisponible: Long) :
        Exception("Stock insuficiente para $nombreProducto")
}
