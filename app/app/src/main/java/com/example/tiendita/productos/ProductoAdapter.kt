package com.example.tiendita.productos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.Producto
import com.example.tiendita.util.MoneyUtil

class ProductoAdapter(
    private val onEditar: (Producto) -> Unit,
    private val onEliminar: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    private val productos = mutableListOf<Producto>()

    fun actualizar(nuevaLista: List<Producto>) {
        productos.clear()
        productos.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    inner class ProductoViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<android.widget.TextView>(R.id.tvNombreProducto)
        private val tvPrecios = itemView.findViewById<android.widget.TextView>(R.id.tvPreciosProducto)
        private val tvCantidad = itemView.findViewById<android.widget.TextView>(R.id.tvCantidadProducto)
        private val btnEditar = itemView.findViewById<android.widget.ImageButton>(R.id.btnEditarProducto)
        private val btnEliminar = itemView.findViewById<android.widget.ImageButton>(R.id.btnEliminarProducto)

        fun bind(producto: Producto) {
            tvNombre.text = producto.nombre
            tvPrecios.text = "Compra: ${MoneyUtil.format(producto.precioCompra)}   Venta: ${MoneyUtil.format(producto.precioVenta)}"
            tvCantidad.text = "Disponibles: ${producto.cantidad}"

            btnEditar.setOnClickListener { onEditar(producto) }
            btnEliminar.setOnClickListener { onEliminar(producto) }
        }
    }
}
