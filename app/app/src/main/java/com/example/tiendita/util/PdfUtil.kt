package com.example.tiendita.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.tiendita.model.CorteCaja
import com.example.tiendita.model.Venta
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RF7: Genera el PDF del corte de caja (fecha, hora, ventas y total)
 * y lo guarda en la carpeta de Descargas del dispositivo para su descarga.
 */
object PdfUtil {

    private const val PAGE_WIDTH = 595   // Tamaño carta aprox. a 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private val locale = Locale.Builder().setLanguage("es").setRegion("MX").build()
    private val formatoArchivo = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val formatoLegible = SimpleDateFormat("dd/MM/yyyy hh:mm a", locale)
    private val formatoHora = SimpleDateFormat("hh:mm a", locale)

    /**
     * Genera el PDF de un corte de caja con el detalle de ventas incluido,
     * lo guarda en Descargas y regresa el Uri del archivo generado.
     */
    fun generarCortePdf(context: Context, corte: CorteCaja, ventas: List<Venta>): Uri? {
        val document = PdfDocument()

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val totalPaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val linePaint = Paint().apply { strokeWidth = 1f; color = Color.LTGRAY }
        val footerPaint = Paint().apply { textSize = 9f; color = Color.GRAY }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        fun nuevaPagina() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN
        }

        // Encabezado
        canvas.drawText("Tiendita - Corte de caja", MARGIN, y, titlePaint)
        y += 24f
        canvas.drawText("Fecha del corte: ${formatoLegible.format(corte.fechaCierre.toDate())}", MARGIN, y, subtitlePaint)
        y += 16f
        canvas.drawText(
            "Periodo cubierto: ${formatoLegible.format(corte.fechaApertura.toDate())} a ${formatoLegible.format(corte.fechaCierre.toDate())}",
            MARGIN, y, subtitlePaint
        )
        y += 16f
        if (corte.usuario.isNotBlank()) {
            canvas.drawText("Responsable: ${corte.usuario}", MARGIN, y, subtitlePaint)
            y += 16f
        }
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 22f

        // Encabezado de tabla
        canvas.drawText("Producto", MARGIN, y, headerPaint)
        canvas.drawText("Cant.", 295f, y, headerPaint)
        canvas.drawText("P. Unit.", 355f, y, headerPaint)
        canvas.drawText("Hora", 435f, y, headerPaint)
        canvas.drawText("Importe", 495f, y, headerPaint)
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 18f

        // Detalle de ventas
        for (venta in ventas) {
            if (y > PAGE_HEIGHT - MARGIN - 40f) {
                nuevaPagina()
            }
            val etiquetaBase = if (venta.esDeCliente()) {
                "${venta.productoNombre} (${venta.clienteNombre})"
            } else {
                venta.productoNombre
            }
            val nombre = if (etiquetaBase.length > 26) {
                etiquetaBase.take(23) + "..."
            } else {
                etiquetaBase
            }
            canvas.drawText(nombre, MARGIN, y, bodyPaint)
            canvas.drawText("${venta.cantidad}", 295f, y, bodyPaint)
            canvas.drawText(MoneyUtil.format(venta.precioUnitario), 355f, y, bodyPaint)
            canvas.drawText(formatoHora.format(venta.fecha.toDate()), 435f, y, bodyPaint)
            canvas.drawText(MoneyUtil.format(venta.montoTotal), 495f, y, bodyPaint)
            y += 18f
        }

        // Resumen y total
        if (y > PAGE_HEIGHT - MARGIN - 110f) {
            nuevaPagina()
        }
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 24f
        canvas.drawText("Numero de ventas: ${corte.numeroVentas}", MARGIN, y, bodyPaint)
        y += 18f
        canvas.drawText("Articulos vendidos: ${corte.totalArticulosVendidos}", MARGIN, y, bodyPaint)
        y += 26f
        canvas.drawText("TOTAL DEL CORTE: ${MoneyUtil.format(corte.totalVentas)}", MARGIN, y, totalPaint)
        y += 30f
        canvas.drawText(
            "Generado automaticamente por Tiendita el ${formatoLegible.format(java.util.Date())}",
            MARGIN, PAGE_HEIGHT - MARGIN, footerPaint
        )

        document.finishPage(page)

        val nombreArchivo = "corte_caja_${formatoArchivo.format(corte.fechaCierre.toDate())}.pdf"
        val uri = try {
            guardarEnDescargas(context, document, nombreArchivo)
        } finally {
            document.close()
        }
        return uri
    }

    /**
     * Genera el tiquet de compra de un cliente: su nombre, los productos
     * comprados con cantidad y precio, y el total. Se guarda en Descargas
     * igual que el corte de caja.
     */
    fun generarTicketPdf(
        context: Context,
        clienteNombre: String,
        ventas: List<Venta>,
        total: Double,
        fecha: Timestamp
    ): Uri? {
        val document = PdfDocument()

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val totalPaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val linePaint = Paint().apply { strokeWidth = 1f; color = Color.LTGRAY }
        val footerPaint = Paint().apply { textSize = 9f; color = Color.GRAY }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        fun nuevaPagina() {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN
        }

        // Encabezado
        canvas.drawText("Tiendita - Tiquet de compra", MARGIN, y, titlePaint)
        y += 24f
        canvas.drawText("Fecha: ${formatoLegible.format(fecha.toDate())}", MARGIN, y, subtitlePaint)
        y += 16f
        canvas.drawText("Cliente: $clienteNombre", MARGIN, y, subtitlePaint)
        y += 16f
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 22f

        // Encabezado de tabla
        canvas.drawText("Producto", MARGIN, y, headerPaint)
        canvas.drawText("Cant.", 335f, y, headerPaint)
        canvas.drawText("P. Unit.", 405f, y, headerPaint)
        canvas.drawText("Importe", 495f, y, headerPaint)
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 18f

        // Detalle de productos comprados
        for (venta in ventas) {
            if (y > PAGE_HEIGHT - MARGIN - 40f) {
                nuevaPagina()
            }
            val nombre = if (venta.productoNombre.length > 34) {
                venta.productoNombre.take(31) + "..."
            } else {
                venta.productoNombre
            }
            canvas.drawText(nombre, MARGIN, y, bodyPaint)
            canvas.drawText("${venta.cantidad}", 335f, y, bodyPaint)
            canvas.drawText(MoneyUtil.format(venta.precioUnitario), 405f, y, bodyPaint)
            canvas.drawText(MoneyUtil.format(venta.montoTotal), 495f, y, bodyPaint)
            y += 18f
        }

        // Total
        if (y > PAGE_HEIGHT - MARGIN - 80f) {
            nuevaPagina()
        }
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 26f
        canvas.drawText("TOTAL: ${MoneyUtil.format(total)}", MARGIN, y, totalPaint)
        y += 30f
        canvas.drawText(
            "Gracias por tu compra. Generado automaticamente por Tiendita el ${formatoLegible.format(java.util.Date())}",
            MARGIN, PAGE_HEIGHT - MARGIN, footerPaint
        )

        document.finishPage(page)

        val nombreArchivo = "ticket_${formatoArchivo.format(fecha.toDate())}.pdf"
        val uri = try {
            guardarEnDescargas(context, document, nombreArchivo)
        } finally {
            document.close()
        }
        return uri
    }

    private fun guardarEnDescargas(context: Context, document: PdfDocument, nombreArchivo: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, nombreArchivo)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }
}
