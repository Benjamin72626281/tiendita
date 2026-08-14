import { db } from "../firebase.js";
import {
  collection,
  doc,
  onSnapshot,
  runTransaction,
  Timestamp
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_PRODUCTOS, COLLECTION_VENTAS, UMBRAL_STOCK_BAJO } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";
import { notificarStockBajo } from "../utils/notificaciones.js";

let unsubscribe = null;
let productos = [];

export function renderVentas(container, navigate) {
  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="dashboard">← Volver</button>
      <h1>Registrar venta</h1>
    </div>
    <form id="form-venta" class="form-card">
      <label>Producto</label>
      <select id="v-producto"></select>
      <label>Cantidad a vender</label>
      <input type="number" id="v-cantidad" min="1" step="1" value="1" />
      <p class="total-label">Total: <strong id="v-total">${formatMoney(0)}</strong></p>
      <p id="v-error" class="error-text" hidden></p>
      <button type="submit" id="v-btn" class="btn-primary">Registrar venta</button>
    </form>
  `;

  container.querySelector("[data-ruta='dashboard']").addEventListener("click", () => navigate("dashboard"));

  const select = container.querySelector("#v-producto");
  const cantidadInput = container.querySelector("#v-cantidad");
  const totalEl = container.querySelector("#v-total");
  const errorEl = container.querySelector("#v-error");
  const btn = container.querySelector("#v-btn");

  function actualizarTotal() {
    const p = productos[select.selectedIndex];
    const cantidad = parseInt(cantidadInput.value, 10) || 0;
    totalEl.textContent = formatMoney((p?.precioVenta ?? 0) * cantidad);
  }

  if (unsubscribe) unsubscribe();
  unsubscribe = onSnapshot(collection(db, COLLECTION_PRODUCTOS), (snapshot) => {
    productos = snapshot.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => a.nombre.toLowerCase().localeCompare(b.nombre.toLowerCase()));

    if (!select.isConnected) return;

    if (productos.length === 0) {
      select.innerHTML = `<option>Registra al menos un producto antes de vender</option>`;
      btn.disabled = true;
    } else {
      select.innerHTML = productos
        .map((p) => `<option value="${p.id}">${escapeHtml(p.nombre)} (disp: ${p.cantidad})</option>`)
        .join("");
      btn.disabled = false;
    }
    actualizarTotal();
  });

  select.addEventListener("change", actualizarTotal);
  cantidadInput.addEventListener("input", actualizarTotal);

  container.querySelector("#form-venta").addEventListener("submit", async (e) => {
    e.preventDefault();
    errorEl.hidden = true;

    const producto = productos[select.selectedIndex];
    const cantidad = parseInt(cantidadInput.value, 10);

    if (!producto) return mostrarError("Selecciona un producto");
    if (!cantidad || cantidad <= 0) return mostrarError("Ingresa una cantidad válida");
    if (cantidad > producto.cantidad) return mostrarError(`Stock insuficiente. Disponible: ${producto.cantidad}`);

    btn.disabled = true;
    try {
      const productoRef = doc(db, COLLECTION_PRODUCTOS, producto.id);
      const ventaRef = doc(collection(db, COLLECTION_VENTAS));

      const nuevoStock = await runTransaction(db, async (transaction) => {
        const snap = await transaction.get(productoRef);
        const stockActual = snap.data()?.cantidad ?? 0;
        if (cantidad > stockActual) throw new Error("stock_insuficiente");
        const nuevo = stockActual - cantidad;
        transaction.update(productoRef, { cantidad: nuevo });
        transaction.set(ventaRef, {
          productoId: producto.id,
          productoNombre: producto.nombre,
          cantidad,
          precioUnitario: producto.precioVenta,
          montoTotal: producto.precioVenta * cantidad,
          fecha: Timestamp.now(),
          // Vacíos: esta venta la registró el vendedor manualmente, no un
          // cliente comprando desde su cuenta (ver js/pages/tienda.js).
          clienteNombre: "",
          pedidoId: ""
        });
        return nuevo;
      });

      cantidadInput.value = "1";
      mostrarExito("Venta registrada correctamente");

      // RF4: notificar si el stock resultante es bajo
      if (nuevoStock <= UMBRAL_STOCK_BAJO) {
        notificarStockBajo(producto.nombre, nuevoStock);
      }
    } catch (err) {
      mostrarError(`Stock insuficiente. Disponible: ${producto.cantidad}`);
    } finally {
      btn.disabled = false;
    }
  });

  function mostrarError(msg) {
    errorEl.className = "error-text";
    errorEl.textContent = msg;
    errorEl.hidden = false;
  }
  function mostrarExito(msg) {
    errorEl.className = "success-text";
    errorEl.textContent = msg;
    errorEl.hidden = false;
    setTimeout(() => (errorEl.hidden = true), 2500);
  }
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
