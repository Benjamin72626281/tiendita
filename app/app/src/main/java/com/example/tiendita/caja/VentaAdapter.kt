package com.example.tiendita.caja

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.Venta
import com.example.tiendita.util.MoneyUtil
import java.text.SimpleDateFormat
import java.util.Locale

class VentaAdapter : RecyclerView.Adapter<VentaAdapter.VentaViewHolder>() {

    private val ventas = mutableListOf<Venta>()
    private val formatoHora = SimpleDateFormat("hh:mm a", Locale.Builder().setLanguage("es").setRegion("MX").build())

    fun actualizar(nuevaLista: List<Venta>) {
        ventas.clear()
        ventas.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        holder.bind(ventas[position])
    }

    override fun getItemCount(): Int = ventas.size

    inner class VentaViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<android.widget.TextView>(R.id.tvNombreVenta)
        private val tvDetalle = itemView.findViewById<android.widget.TextView>(R.id.tvDetalleVenta)
        private val tvMonto = itemView.findViewById<android.widget.TextView>(R.id.tvMontoVenta)

        fun bind(venta: Venta) {
            tvNombre.text = venta.productoNombre
            val hora = formatoHora.format(venta.fecha.toDate())
            tvDetalle.text = if (venta.esDeCliente()) {
                "Cantidad: ${venta.cantidad}  •  $hora  •  Cliente: ${venta.clienteNombre}"
            } else {
                "Cantidad: ${venta.cantidad}  •  $hora"
            }
            tvMonto.text = MoneyUtil.format(venta.montoTotal)
        }
    }
}
