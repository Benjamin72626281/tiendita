import { db, auth, signOut } from "../firebase.js";
import {
  collection,
  doc,
  onSnapshot,
  runTransaction,
  Timestamp
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_PRODUCTOS, COLLECTION_VENTAS } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";
import { generarTicketPdf } from "../utils/pdf.js";

let unsubscribe = null;

// Pantalla de compra para clientes: pueden ver el catálogo, elegir cuántas
// unidades quieren de cada producto y pagar todo junto. Al pagar se genera
// un tiquet en PDF con su nombre y los productos comprados, y las ventas
// quedan guardadas con su nombre para que aparezcan también en el corte de
// caja del vendedor (equivalente web de ClienteTiendaActivity.kt).
export function renderTiendaCliente(container, nombreCliente) {
  container.innerHTML = `
    <div class="topbar topbar-home">
      <div class="brand">
        <span class="brand-logo">🛍️</span>
        <div>
          <h1>Hola, ${escapeHtml(nombreCliente)}</h1>
          <p class="brand-subtitle">Elige los productos que quieras comprar</p>
        </div>
      </div>
      <button id="btn-logout-cliente" class="btn-secondary">Cerrar sesión</button>
    </div>
    <p id="t-sin-productos" class="empty-text" hidden>Por ahora no hay productos disponibles</p>
    <div id="t-lista" class="lista lista-cliente"></div>
    <div id="modal-root"></div>
    <div id="carrito-bar" class="carrito-bar" hidden>
      <div class="carrito-resumen">
        <span id="carrito-articulos" class="carrito-articulos">0 artículos</span>
        <strong id="carrito-total" class="carrito-total">${formatMoney(0)}</strong>
      </div>
      <button id="btn-pagar" class="btn-primary btn-pagar" disabled>Pagar</button>
    </div>
  `;

  container.querySelector("#btn-logout-cliente").addEventListener("click", async () => {
    await signOut(auth);
  });

  let productos = [];
  const carrito = new Map(); // productoId -> cantidad seleccionada
  let comprando = false;

  const listaEl = container.querySelector("#t-lista");
  const sinProductosEl = container.querySelector("#t-sin-productos");
  const carritoBar = container.querySelector("#carrito-bar");
  const carritoArticulos = container.querySelector("#carrito-articulos");
  const carritoTotal = container.querySelector("#carrito-total");
  const btnPagar = container.querySelector("#btn-pagar");

  if (unsubscribe) unsubscribe();
  unsubscribe = onSnapshot(collection(db, COLLECTION_PRODUCTOS), (snapshot) => {
    productos = snapshot.docs
      .map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => a.nombre.toLowerCase().localeCompare(b.nombre.toLowerCase()));

    if (!listaEl.isConnected) return; // el cliente ya navegó fuera (cerró sesión)

    // Si algún producto seleccionado ya no existe o bajó de stock, se ajusta.
    carrito.forEach((cantidad, productoId) => {
      const producto = productos.find((p) => p.id === productoId);
      const stock = producto ? producto.cantidad : 0;
      if (!producto || stock <= 0) carrito.delete(productoId);
      else if (cantidad > stock) carrito.set(productoId, stock);
    });

    sinProductosEl.hidden = productos.length > 0;
    listaEl.hidden = productos.length === 0;
    pintarLista();
    actualizarResumen();
  });

  function pintarLista() {
    listaEl.innerHTML = productos
      .map((p) => {
        const stock = p.cantidad;
        const actual = carrito.get(p.id) || 0;
        return `
        <div class="item-card item-cliente">
          <div class="item-info">
            <strong>${escapeHtml(p.nombre)}</strong>
            <span>${formatMoney(p.precioVenta)}</span>
            <span>Disponibles: ${stock}</span>
          </div>
          <div class="selector-cantidad">
            <button type="button" class="btn-selector" data-menos="${p.id}" ${actual <= 0 ? "disabled" : ""}>−</button>
            <span class="selector-num">${actual}</span>
            <button type="button" class="btn-selector" data-mas="${p.id}" ${actual >= stock ? "disabled" : ""}>+</button>
          </div>
        </div>`;
      })
      .join("");

    listaEl.querySelectorAll("[data-menos]").forEach((btn) =>
      btn.addEventListener("click", () => cambiarCantidad(btn.dataset.menos, -1))
    );
    listaEl.querySelectorAll("[data-mas]").forEach((btn) =>
      btn.addEventListener("click", () => cambiarCantidad(btn.dataset.mas, 1))
    );
  }

  function cambiarCantidad(productoId, delta) {
    const producto = productos.find((p) => p.id === productoId);
    if (!producto) return;
    const actual = carrito.get(productoId) || 0;
    const nueva = actual + delta;
    if (nueva <= 0) carrito.delete(productoId);
    else if (nueva <= producto.cantidad) carrito.set(productoId, nueva);
    pintarLista();
    actualizarResumen();
  }

  function itemsCarritoActuales() {
    const items = [];
    carrito.forEach((cantidad, productoId) => {
      const producto = productos.find((p) => p.id === productoId);
      if (!producto || cantidad <= 0) return;
      items.push({ producto, cantidad, subtotal: producto.precioVenta * cantidad });
    });
    return items;
  }

  function actualizarResumen() {
    const items = itemsCarritoActuales();
    const totalArticulos = items.reduce((s, i) => s + i.cantidad, 0);
    const total = items.reduce((s, i) => s + i.subtotal, 0);

    carritoBar.hidden = items.length === 0;
    carritoArticulos.textContent = `${totalArticulos} artículo${totalArticulos === 1 ? "" : "s"}`;
    carritoTotal.textContent = formatMoney(total);
    if (!comprando) btnPagar.disabled = items.length === 0;
  }

  btnPagar.addEventListener("click", () => {
    const items = itemsCarritoActuales();
    if (items.length === 0) return;
    abrirConfirmacion(items);
  });

  function abrirConfirmacion(items) {
    const total = items.reduce((s, i) => s + i.subtotal, 0);
    const resumen = items
      .map((i) => `${i.cantidad} x ${escapeHtml(i.producto.nombre)} = ${formatMoney(i.subtotal)}`)
      .join("<br>");

    const modalRoot = container.querySelector("#modal-root");
    modalRoot.innerHTML = `
      <div class="modal-overlay">
        <div class="modal">
          <h2>Confirmar compra</h2>
          <p>${resumen}</p>
          <p class="modal-total"><strong>Total: ${formatMoney(total)}</strong></p>
          <p>¿Deseas confirmar tu compra?</p>
          <div class="modal-actions">
            <button id="cp-cancelar" class="btn-secondary">Cancelar</button>
            <button id="cp-confirmar" class="btn-primary">Pagar</button>
          </div>
        </div>
      </div>
    `;
    modalRoot.querySelector("#cp-cancelar").addEventListener("click", () => (modalRoot.innerHTML = ""));
    modalRoot.querySelector("#cp-confirmar").addEventListener("click", async () => {
      modalRoot.innerHTML = "";
      await realizarCompra(items);
    });
  }

  async function realizarCompra(items) {
    comprando = true;
    btnPagar.disabled = true;

    const pedidoId = window.crypto?.randomUUID ? window.crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const fechaCompra = Timestamp.now();

    try {
      await runTransaction(db, async (transaction) => {
        // 1) Se leen TODOS los productos primero (Firestore exige que las
        // lecturas de una transacción ocurran antes que cualquier escritura).
        const refsProductos = items.map((i) => doc(db, COLLECTION_PRODUCTOS, i.producto.id));
        const snapshots = await Promise.all(refsProductos.map((ref) => transaction.get(ref)));

        // 2) Se valida que siga habiendo stock suficiente de cada producto
        // (por si alguien más compró justo antes).
        items.forEach((item, index) => {
          const stockActual = snapshots[index].data()?.cantidad ?? 0;
          if (item.cantidad > stockActual) {
            throw new Error(`stock_insuficiente:${item.producto.nombre}:${stockActual}`);
          }
        });

        // 3) Se descuenta el stock y se registra una venta por cada
        // producto, todas con el mismo pedidoId y el nombre del cliente.
        items.forEach((item, index) => {
          const stockActual = snapshots[index].data()?.cantidad ?? 0;
          transaction.update(refsProductos[index], { cantidad: stockActual - item.cantidad });

          const ventaRef = doc(collection(db, COLLECTION_VENTAS));
          transaction.set(ventaRef, {
            productoId: item.producto.id,
            productoNombre: item.producto.nombre,
            cantidad: item.cantidad,
            precioUnitario: item.producto.precioVenta,
            montoTotal: item.subtotal,
            fecha: fechaCompra,
            clienteNombre: nombreCliente,
            pedidoId
          });
        });
      });

      carrito.clear();
      comprando = false;
      pintarLista();
      actualizarResumen();

      try {
        generarTicketPdf(
          nombreCliente,
          items.map((i) => ({
            productoNombre: i.producto.nombre,
            cantidad: i.cantidad,
            precioUnitario: i.producto.precioVenta,
            montoTotal: i.subtotal
          })),
          items.reduce((s, i) => s + i.subtotal, 0),
          fechaCompra
        );
        mostrarExito();
      } catch (e) {
        mostrarExito("Tu compra se registró, pero no se pudo generar el tiquet.");
      }
    } catch (err) {
      comprando = false;
      actualizarResumen();
      let mensaje = "No se pudo completar la compra. Intenta de nuevo";
      if (String(err.message).startsWith("stock_insuficiente:")) {
        const [, nombreProd, stock] = err.message.split(":");
        mensaje = `Ya no hay suficiente stock de ${nombreProd} (disponible: ${stock}). Ajusta tu carrito e intenta de nuevo`;
      }
      alert(mensaje);
    }
  }

  function mostrarExito(mensajeAlterno) {
    const modalRoot = container.querySelector("#modal-root");
    modalRoot.innerHTML = `
      <div class="modal-overlay">
        <div class="modal">
          <h2>¡Compra realizada!</h2>
          <p>${mensajeAlterno || "Tu compra se registró correctamente y tu tiquet se descargó."}</p>
          <div class="modal-actions">
            <button id="ce-ok" class="btn-primary">Cerrar</button>
          </div>
        </div>
      </div>
    `;
    modalRoot.querySelector("#ce-ok").addEventListener("click", () => (modalRoot.innerHTML = ""));
  }
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
