package com.example.tiendita.caja

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.CorteCaja
import com.example.tiendita.model.Venta
import com.example.tiendita.util.Constants
import com.example.tiendita.util.PdfUtil
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * RF7: Historial de todos los cortes de caja guardados en la base de datos.
 * Permite volver a generar y descargar el PDF de cualquier corte anterior.
 */
class CorteHistorialActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSinCortes: android.widget.TextView
    private lateinit var adapter: CorteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corte_historial)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        recyclerView = findViewById(R.id.rvCortes)
        tvSinCortes = findViewById(R.id.tvSinCortes)

        adapter = CorteAdapter { corte -> descargarPdfDeCorte(corte) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        cargarCortes()
    }

    private fun cargarCortes() {
        db.collection(Constants.COLLECTION_CORTES)
            .orderBy("fechaCierre", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val cortes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CorteCaja::class.java)?.apply { id = doc.id }
                }.orEmpty()

                adapter.actualizar(cortes)
                tvSinCortes.visibility = if (cortes.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (cortes.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun descargarPdfDeCorte(corte: CorteCaja) {
        val ventas = corte.detalle.map { m ->
            Venta(
                productoNombre = m["productoNombre"] as? String ?: "",
                cantidad = (m["cantidad"] as? Number)?.toLong() ?: 0L,
                precioUnitario = (m["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                montoTotal = (m["montoTotal"] as? Number)?.toDouble() ?: 0.0,
                fecha = m["fecha"] as? Timestamp ?: corte.fechaCierre
            )
        }
        val uri = try {
            PdfUtil.generarCortePdf(this, corte, ventas)
        } catch (e: Exception) {
            null
        }
        if (uri != null) {
            Toast.makeText(this, getString(R.string.pdf_descargado), Toast.LENGTH_SHORT).show()
            abrirPdf(uri)
        } else {
            Toast.makeText(this, getString(R.string.error_generar_pdf), Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirPdf(uri: Uri) {
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
}
