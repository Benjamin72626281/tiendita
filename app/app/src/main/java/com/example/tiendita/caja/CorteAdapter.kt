package com.example.tiendita.caja

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.CorteCaja
import com.example.tiendita.util.MoneyUtil
import java.text.SimpleDateFormat
import java.util.Locale

class CorteAdapter(
    private val onDescargar: (CorteCaja) -> Unit
) : RecyclerView.Adapter<CorteAdapter.CorteViewHolder>() {

    private val cortes = mutableListOf<CorteCaja>()
    private val formatoFecha = SimpleDateFormat(
        "dd/MM/yyyy hh:mm a",
        Locale.Builder().setLanguage("es").setRegion("MX").build()
    )

    fun actualizar(nuevaLista: List<CorteCaja>) {
        cortes.clear()
        cortes.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CorteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_corte, parent, false)
        return CorteViewHolder(view)
    }

    override fun onBindViewHolder(holder: CorteViewHolder, position: Int) {
        holder.bind(cortes[position])
    }

    override fun getItemCount(): Int = cortes.size

    inner class CorteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById<android.widget.TextView>(R.id.tvFechaCorte)
        private val tvDetalle = itemView.findViewById<android.widget.TextView>(R.id.tvDetalleCorte)
        private val tvTotal = itemView.findViewById<android.widget.TextView>(R.id.tvTotalCorteItem)
        private val btnDescargar = itemView.findViewById<android.widget.ImageButton>(R.id.btnDescargarCorte)

        fun bind(corte: CorteCaja) {
            tvFecha.text = formatoFecha.format(corte.fechaCierre.toDate())
            tvDetalle.text = itemView.context.getString(
                R.string.detalle_corte_item,
                corte.numeroVentas,
                corte.totalArticulosVendidos
            )
            tvTotal.text = MoneyUtil.format(corte.totalVentas)
            btnDescargar.setOnClickListener { onDescargar(corte) }
        }
    }
}
