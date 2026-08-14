import { formatMoney } from "./money.js";

// RF7: genera y descarga el PDF de un corte de caja. Usa jsPDF (cargado por
// CDN en index.html como window.jspdf.jsPDF), mismo contenido que genera
// PdfUtil.kt en la app Android.
export function generarCortePdf(corte, ventas) {
  const { jsPDF } = window.jspdf;
  const doc = new jsPDF();
  const fechaCierre = corte.fechaCierre?.toDate ? corte.fechaCierre.toDate() : new Date(corte.fechaCierre);
  const fechaApertura = corte.fechaApertura?.toDate ? corte.fechaApertura.toDate() : new Date(corte.fechaApertura);

  let y = 20;
  doc.setFontSize(16);
  doc.text("Tiendita — Corte de caja", 14, y);
  y += 10;
  doc.setFontSize(10);
  doc.text(`Periodo: ${formatearFecha(fechaApertura)}  →  ${formatearFecha(fechaCierre)}`, 14, y);
  y += 6;
  doc.text(`Responsable: ${corte.usuario || "-"}`, 14, y);
  y += 10;

  doc.setFontSize(12);
  doc.text(`Total: ${formatMoney(corte.totalVentas)}`, 14, y);
  y += 6;
  doc.text(`Número de ventas: ${corte.numeroVentas}`, 14, y);
  y += 6;
  doc.text(`Artículos vendidos: ${corte.totalArticulosVendidos}`, 14, y);
  y += 10;

  doc.setFontSize(11);
  doc.text("Detalle de ventas:", 14, y);
  y += 7;
  doc.setFontSize(9);

  ventas.forEach((v) => {
    if (y > 280) {
      doc.addPage();
      y = 20;
    }
    const fecha = v.fecha?.toDate ? v.fecha.toDate() : new Date(v.fecha);
    doc.text(
      `${formatearFecha(fecha)}  —  ${v.cantidad} x ${v.productoNombre}  =  ${formatMoney(v.montoTotal)}`,
      14,
      y
    );
    y += 6;
  });

  const nombreArchivo = `corte-${fechaCierre.toISOString().slice(0, 19).replace(/[:T]/g, "-")}.pdf`;
  doc.save(nombreArchivo);
}

function formatearFecha(fecha) {
  return fecha.toLocaleString("es-MX", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

// RF (tienda de clientes): genera y descarga el tiquet de compra de un
// cliente, mismo contenido que genera PdfUtil.generarTicketPdf en Android.
export function generarTicketPdf(clienteNombre, ventas, total, fecha) {
  const { jsPDF } = window.jspdf;
  const doc = new jsPDF();
  const fechaCompra = fecha?.toDate ? fecha.toDate() : new Date(fecha);

  let y = 20;
  doc.setFontSize(16);
  doc.text("Tiendita — Tiquet de compra", 14, y);
  y += 10;
  doc.setFontSize(10);
  doc.text(`Fecha: ${formatearFecha(fechaCompra)}`, 14, y);
  y += 6;
  doc.text(`Cliente: ${clienteNombre}`, 14, y);
  y += 10;

  doc.setFontSize(11);
  doc.text("Productos:", 14, y);
  y += 7;
  doc.setFontSize(9);

  ventas.forEach((v) => {
    if (y > 280) {
      doc.addPage();
      y = 20;
    }
    doc.text(
      `${v.cantidad} x ${v.productoNombre}  —  ${formatMoney(v.precioUnitario)} c/u  =  ${formatMoney(v.montoTotal)}`,
      14,
      y
    );
    y += 6;
  });

  y += 6;
  doc.setFontSize(13);
  doc.text(`TOTAL: ${formatMoney(total)}`, 14, y);
  y += 10;
  doc.setFontSize(9);
  doc.text("Gracias por tu compra.", 14, y);

  const nombreArchivo = `ticket-${fechaCompra.toISOString().slice(0, 19).replace(/[:T]/g, "-")}.pdf`;
  doc.save(nombreArchivo);
}
