import { db } from "../firebase.js";
import {
  collection,
  onSnapshot,
  addDoc,
  doc,
  setDoc,
  deleteDoc,
  getDoc
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_PRODUCTOS } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";

let unsubscribe = null;

export function renderProductos(container, navigate) {
  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="dashboard">← Volver</button>
      <h1>Productos</h1>
      <button id="fab-agregar" class="btn-primary">+ Agregar producto</button>
    </div>
    <p id="sin-productos" class="empty-text" hidden>Aún no hay productos registrados</p>
    <div id="lista-productos" class="lista"></div>
    <div id="modal-root"></div>
  `;

  container.querySelector("[data-ruta='dashboard']").addEventListener("click", () => navigate("dashboard"));
  container.querySelector("#fab-agregar").addEventListener("click", () => abrirFormulario(container, null));

  if (unsubscribe) unsubscribe();
  unsubscribe = onSnapshot(collection(db, COLLECTION_PRODUCTOS), (snapshot) => {
    const productos = snapshot.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => a.nombre.toLowerCase().localeCompare(b.nombre.toLowerCase()));

    const lista = container.querySelector("#lista-productos");
    const sinProductos = container.querySelector("#sin-productos");
    if (!lista) return; // el usuario navegó a otra página

    sinProductos.hidden = productos.length > 0;
    lista.innerHTML = productos
      .map(
        (p) => `
        <div class="item-card">
          <div class="item-info">
            <strong>${escapeHtml(p.nombre)}</strong>
            <span>Venta: ${formatMoney(p.precioVenta)} · Compra: ${formatMoney(p.precioCompra)}</span>
            <span>Cantidad: ${p.cantidad}${p.cantidad <= 5 ? " ⚠️ Stock bajo" : ""}</span>
          </div>
          <div class="item-actions">
            <button class="btn-secondary" data-editar="${p.id}">Editar</button>
            <button class="btn-danger" data-eliminar="${p.id}">Eliminar</button>
          </div>
        </div>`
      )
      .join("");

    lista.querySelectorAll("[data-editar]").forEach((btn) =>
      btn.addEventListener("click", () => abrirFormulario(container, btn.dataset.editar))
    );
    lista.querySelectorAll("[data-eliminar]").forEach((btn) =>
      btn.addEventListener("click", () => confirmarEliminar(container, productos.find((p) => p.id === btn.dataset.eliminar)))
    );
  });
}

async function abrirFormulario(container, productoId) {
  const modalRoot = container.querySelector("#modal-root");
  let producto = { nombre: "", precioCompra: "", precioVenta: "", cantidad: "" };
  if (productoId) {
    const snap = await getDoc(doc(db, COLLECTION_PRODUCTOS, productoId));
    if (snap.exists()) producto = snap.data();
  }

  modalRoot.innerHTML = `
    <div class="modal-overlay">
      <div class="modal">
        <h2>${productoId ? "Editar producto" : "Agregar producto"}</h2>
        <form id="form-producto">
          <label>Nombre del producto</label>
          <input type="text" id="p-nombre" value="${escapeAttr(producto.nombre)}" required />
          <label>Precio de compra (MXN)</label>
          <input type="number" id="p-precio-compra" step="0.01" min="0" value="${producto.precioCompra ?? ""}" required />
          <label>Precio de venta (MXN)</label>
          <input type="number" id="p-precio-venta" step="0.01" min="0" value="${producto.precioVenta ?? ""}" required />
          <label>Cantidad disponible</label>
          <input type="number" id="p-cantidad" step="1" min="0" value="${producto.cantidad ?? ""}" required />
          <p id="p-error" class="error-text" hidden></p>
          <div class="modal-actions">
            <button type="button" id="p-cancelar" class="btn-secondary">Cancelar</button>
            <button type="submit" class="btn-primary">Guardar</button>
          </div>
        </form>
      </div>
    </div>
  `;

  modalRoot.querySelector("#p-cancelar").addEventListener("click", () => (modalRoot.innerHTML = ""));
  modalRoot.querySelector("#form-producto").addEventListener("submit", async (e) => {
    e.preventDefault();
    const nombre = modalRoot.querySelector("#p-nombre").value.trim();
    const precioCompra = parseFloat(modalRoot.querySelector("#p-precio-compra").value);
    const precioVenta = parseFloat(modalRoot.querySelector("#p-precio-venta").value);
    const cantidad = parseInt(modalRoot.querySelector("#p-cantidad").value, 10);

    if (!nombre || isNaN(precioCompra) || isNaN(precioVenta) || isNaN(cantidad) || precioCompra < 0 || precioVenta < 0 || cantidad < 0) {
      const errorEl = modalRoot.querySelector("#p-error");
      errorEl.textContent = "Completa todos los campos correctamente";
      errorEl.hidden = false;
      return;
    }

    const data = { nombre, precioCompra, precioVenta, cantidad };
    try {
      if (productoId) {
        await setDoc(doc(db, COLLECTION_PRODUCTOS, productoId), data);
      } else {
        await addDoc(collection(db, COLLECTION_PRODUCTOS), data);
      }
      modalRoot.innerHTML = "";
    } catch (err) {
      const errorEl = modalRoot.querySelector("#p-error");
      errorEl.textContent = "No se pudo guardar. Intenta de nuevo";
      errorEl.hidden = false;
    }
  });
}

function confirmarEliminar(container, producto) {
  if (!producto) return;
  const modalRoot = container.querySelector("#modal-root");
  modalRoot.innerHTML = `
    <div class="modal-overlay">
      <div class="modal">
        <h2>Eliminar producto</h2>
        <p>¿Seguro que deseas eliminar "${escapeHtml(producto.nombre)}"? Esta acción no se puede deshacer.</p>
        <div class="modal-actions">
          <button id="del-cancelar" class="btn-secondary">Cancelar</button>
          <button id="del-confirmar" class="btn-danger">Eliminar</button>
        </div>
      </div>
    </div>
  `;
  modalRoot.querySelector("#del-cancelar").addEventListener("click", () => (modalRoot.innerHTML = ""));
  modalRoot.querySelector("#del-confirmar").addEventListener("click", async () => {
    await deleteDoc(doc(db, COLLECTION_PRODUCTOS, producto.id));
    modalRoot.innerHTML = "";
  });
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function escapeAttr(str = "") {
  return escapeHtml(str);
}
