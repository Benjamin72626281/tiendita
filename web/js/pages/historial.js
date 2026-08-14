import { db } from "../firebase.js";
import { collection, query, orderBy, onSnapshot } from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_CORTES } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";
import { generarCortePdf } from "../utils/pdf.js";

let unsubscribe = null;

// RF7: historial de todos los cortes de caja guardados.
export function renderHistorial(container, navigate) {
  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="caja">← Volver</button>
      <h1>Historial de cortes</h1>
    </div>
    <p id="h-sin-cortes" class="empty-text" hidden>Aún no hay cortes de caja guardados</p>
    <div id="h-lista" class="lista"></div>
  `;

  container.querySelector("[data-ruta='caja']").addEventListener("click", () => navigate("caja"));

  if (unsubscribe) unsubscribe();
  const q = query(collection(db, COLLECTION_CORTES), orderBy("fechaCierre", "desc"));
  unsubscribe = onSnapshot(q, (snapshot) => {
    const cortes = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
    const lista = container.querySelector("#h-lista");
    const sinCortes = container.querySelector("#h-sin-cortes");
    if (!lista) return;

    sinCortes.hidden = cortes.length > 0;
    lista.innerHTML = cortes
      .map(
        (c) => `
        <div class="item-card">
          <div class="item-info">
            <strong>${formatearFecha(c.fechaCierre.toDate())}</strong>
            <span>${c.numeroVentas} ventas · ${c.totalArticulosVendidos} artículos</span>
            <span>Responsable: ${escapeHtml(c.usuario || "-")}</span>
          </div>
          <div class="item-actions">
            <span class="cantidad-num">${formatMoney(c.totalVentas)}</span>
            <button class="btn-secondary" data-descargar="${c.id}">Descargar PDF</button>
          </div>
        </div>`
      )
      .join("");

    lista.querySelectorAll("[data-descargar]").forEach((btn) =>
      btn.addEventListener("click", () => {
        const corte = cortes.find((c) => c.id === btn.dataset.descargar);
        generarCortePdf(corte, corte.detalle || []);
      })
    );
  });
}

function formatearFecha(fecha) {
  return fecha.toLocaleString("es-MX", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}
function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
