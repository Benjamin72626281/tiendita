package com.example.tiendita.inventario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.Producto
import com.example.tiendita.util.Constants

class InventarioAdapter : RecyclerView.Adapter<InventarioAdapter.InventarioViewHolder>() {

    private val productos = mutableListOf<Producto>()

    fun actualizar(nuevaLista: List<Producto>) {
        productos.clear()
        productos.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventario, parent, false)
        return InventarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventarioViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    inner class InventarioViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<android.widget.TextView>(R.id.tvNombreInventario)
        private val tvCantidad = itemView.findViewById<android.widget.TextView>(R.id.tvCantidadInventario)

        fun bind(producto: Producto) {
            tvNombre.text = producto.nombre
            tvCantidad.text = producto.cantidad.toString()

            val context = itemView.context
            if (producto.cantidad <= Constants.UMBRAL_STOCK_BAJO) {
                tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.stock_bajo))
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.stock_bajo_bg))
            } else {
                tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.stock_ok))
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
        }
    }
}
