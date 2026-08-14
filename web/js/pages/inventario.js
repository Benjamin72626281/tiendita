import { db } from "../firebase.js";
import { collection, onSnapshot } from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_PRODUCTOS, UMBRAL_STOCK_BAJO } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";
import { notificarStockBajo, pedirPermisoNotificaciones } from "../utils/notificaciones.js";

let unsubscribe = null;

export function renderInventario(container, navigate) {
  pedirPermisoNotificaciones();

  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="dashboard">← Volver</button>
      <h1>Inventario</h1>
    </div>
    <p id="sin-inventario" class="empty-text" hidden>No hay productos en inventario</p>
    <div id="lista-inventario" class="lista"></div>
  `;

  container.querySelector("[data-ruta='dashboard']").addEventListener("click", () => navigate("dashboard"));

  if (unsubscribe) unsubscribe();
  unsubscribe = onSnapshot(collection(db, COLLECTION_PRODUCTOS), (snapshot) => {
    const productos = snapshot.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => a.nombre.toLowerCase().localeCompare(b.nombre.toLowerCase()));

    const lista = container.querySelector("#lista-inventario");
    const sinInventario = container.querySelector("#sin-inventario");
    if (!lista) return;

    sinInventario.hidden = productos.length > 0;
    lista.innerHTML = productos
      .map(
        (p) => `
        <div class="item-card ${p.cantidad <= UMBRAL_STOCK_BAJO ? "item-alerta" : ""}">
          <div class="item-info">
            <strong>${escapeHtml(p.nombre)}</strong>
            <span>Precio de venta: ${formatMoney(p.precioVenta)}</span>
          </div>
          <div class="item-cantidad">
            <span class="cantidad-num">${p.cantidad}</span>
            ${p.cantidad <= UMBRAL_STOCK_BAJO ? '<span class="badge-alerta">Stock bajo</span>' : ""}
          </div>
        </div>`
      )
      .join("");

    productos
      .filter((p) => p.cantidad <= UMBRAL_STOCK_BAJO)
      .forEach((p) => notificarStockBajo(p.nombre, p.cantidad));
  });
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
