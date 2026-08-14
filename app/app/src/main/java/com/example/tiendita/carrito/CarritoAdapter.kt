package com.example.tiendita.carrito

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.ItemCarrito
import com.example.tiendita.util.MoneyUtil

/**
 * Lista de productos ya agregados al carrito (después de elegirlos en el
 * menú desplegable). Cada fila muestra nombre, cantidad × precio unitario,
 * subtotal y un botón para quitar el producto por completo.
 */
class CarritoAdapter(
    private val onEliminar: (productoId: String) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.ViewHolder>() {

    private val items = mutableListOf<ItemCarrito>()

    fun actualizar(nuevaLista: List<ItemCarrito>) {
        items.clear()
        items.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carrito, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreItemCarrito)
        private val tvDetalle = itemView.findViewById<TextView>(R.id.tvDetalleItemCarrito)
        private val tvSubtotal = itemView.findViewById<TextView>(R.id.tvSubtotalItemCarrito)
        private val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminarItemCarrito)

        fun bind(item: ItemCarrito) {
            tvNombre.text = item.producto.nombre
            tvDetalle.text = itemView.context.getString(
                R.string.subtotal_item_formato,
                item.cantidad,
                MoneyUtil.format(item.producto.precioVenta)
            )
            tvSubtotal.text = MoneyUtil.format(item.subtotal)
            btnEliminar.setOnClickListener { onEliminar(item.producto.id) }
        }
    }
}
