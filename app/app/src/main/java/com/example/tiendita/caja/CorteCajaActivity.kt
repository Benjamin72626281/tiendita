package com.example.tiendita.caja

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tiendita.R
import com.example.tiendita.model.CorteCaja
import com.example.tiendita.model.Venta
import com.example.tiendita.util.Constants
import com.example.tiendita.util.MoneyUtil
import com.example.tiendita.util.PdfUtil
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar

/**
 * Módulo Corte de caja - RF6: total de ingresos del periodo actual y RF7:
 * guardado y generación en PDF del corte al cerrar caja.
 *
 * Importante: la pantalla solo muestra las ventas ocurridas DESPUÉS del
 * último corte guardado (o desde el inicio del día si aún no hay ningún
 * corte hoy). Así, al cerrar caja, la ventana se "limpia" automáticamente
 * y esas ventas ya no se vuelven a contar en el siguiente corte.
 */
class CorteCajaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvTotal: android.widget.TextView
    private lateinit var tvSinVentas: android.widget.TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VentaAdapter
    private lateinit var btnCerrarCaja: MaterialButton
    private lateinit var btnHistorial: MaterialButton

    private var corteListener: ListenerRegistration? = null
    private var ventasListener: ListenerRegistration? = null

    private var ventasActuales: List<Venta> = emptyList()
    private var totalActual: Double = 0.0
    private var baselineActual: Timestamp = Timestamp.now()
    private var guardandoCorte: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corte_caja)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvTotal = findViewById(R.id.tvTotalCorte)
        tvSinVentas = findViewById(R.id.tvSinVentas)
        recyclerView = findViewById(R.id.rvVentasHoy)
        btnCerrarCaja = findViewById(R.id.btnCerrarCaja)
        btnHistorial = findViewById(R.id.btnHistorialCortes)

        adapter = VentaAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnCerrarCaja.setOnClickListener { confirmarCierre() }
        btnHistorial.setOnClickListener {
            startActivity(Intent(this, CorteHistorialActivity::class.java))
        }

        escucharUltimoCorte()
    }

    override fun onDestroy() {
        super.onDestroy()
        corteListener?.remove()
        ventasListener?.remove()
    }

    private fun inicioDelDiaDeHoy(): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    /**
     * Escucha el último corte guardado para saber a partir de qué momento
     * deben contarse las ventas (evita repetir ventas ya cerradas en un
     * corte anterior).
     */
    private fun escucharUltimoCorte() {
        corteListener = db.collection(Constants.COLLECTION_CORTES)
            .orderBy("fechaCierre", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val ultimoCorte = snapshot?.documents?.firstOrNull()
                    ?.toObject(CorteCaja::class.java)
                val inicioHoy = inicioDelDiaDeHoy()

                baselineActual = if (ultimoCorte != null && ultimoCorte.fechaCierre > inicioHoy) {
                    ultimoCorte.fechaCierre
                } else {
                    inicioHoy
                }

                suscribirVentasDesde(baselineActual)
            }
    }

    private fun suscribirVentasDesde(baseline: Timestamp) {
        ventasListener?.remove()
        ventasListener = db.collection(Constants.COLLECTION_VENTAS)
            .whereGreaterThan("fecha", baseline)
            .addSnapshotListener { snapshot, _ ->
                val ventas = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Venta::class.java)?.apply { id = doc.id }
                }.orEmpty().sortedByDescending { it.fecha }

                ventasActuales = ventas
                totalActual = ventas.sumOf { it.montoTotal }
                tvTotal.text = MoneyUtil.format(totalActual)

                adapter.actualizar(ventas)
                tvSinVentas.visibility = if (ventas.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (ventas.isEmpty()) View.GONE else View.VISIBLE

                if (!guardandoCorte) {
                    btnCerrarCaja.isEnabled = ventas.isNotEmpty()
                }
            }
    }

    private fun confirmarCierre() {
        if (ventasActuales.isEmpty()) {
            Toast.makeText(this, getString(R.string.sin_ventas_hoy), Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_corte_titulo)
            .setMessage(
                getString(
                    R.string.confirmar_corte_mensaje,
                    MoneyUtil.format(totalActual),
                    ventasActuales.size
                )
            )
            .setPositiveButton(R.string.btn_cerrar_caja) { _, _ -> guardarCorte() }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun guardarCorte() {
        guardandoCorte = true
        btnCerrarCaja.isEnabled = false
        val ventasCorte = ventasActuales

        val corte = CorteCaja(
            fechaApertura = baselineActual,
            fechaCierre = Timestamp.now(),
            totalVentas = ventasCorte.sumOf { it.montoTotal },
            numeroVentas = ventasCorte.size.toLong(),
            totalArticulosVendidos = ventasCorte.sumOf { it.cantidad },
            usuario = FirebaseAuth.getInstance().currentUser?.email ?: "",
            detalle = ventasCorte.map { it.toMap() }
        )

        db.collection(Constants.COLLECTION_CORTES)
            .add(corte.toMap())
            .addOnSuccessListener { docRef ->
                corte.id = docRef.id
                val uri = try {
                    PdfUtil.generarCortePdf(this, corte, ventasCorte)
                } catch (e: Exception) {
                    null
                }
                guardandoCorte = false
                btnCerrarCaja.isEnabled = true
                // El listener de "cortes" detectará este nuevo corte y
                // automáticamente recorrerá la lista de ventas a partir de
                // ahora, dejando la ventana en $0.00.
                if (uri != null) {
                    mostrarDialogoPdfListo(uri)
                } else {
                    Toast.makeText(this, getString(R.string.corte_guardado_sin_pdf), Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                guardandoCorte = false
                btnCerrarCaja.isEnabled = true
                Toast.makeText(this, getString(R.string.error_guardar_corte), Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarDialogoPdfListo(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.corte_guardado_titulo)
            .setMessage(R.string.corte_guardado_mensaje)
            .setPositiveButton(R.string.btn_abrir_pdf) { _, _ -> abrirPdf(uri) }
            .setNegativeButton(R.string.cerrar, null)
            .show()
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
