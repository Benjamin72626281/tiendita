import { db, auth } from "../firebase.js";
import {
  collection,
  query,
  orderBy,
  limit,
  where,
  onSnapshot,
  addDoc,
  Timestamp
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_VENTAS, COLLECTION_CORTES } from "../utils/constants.js";
import { formatMoney } from "../utils/money.js";
import { generarCortePdf } from "../utils/pdf.js";

let corteListenerUnsub = null;
let ventasListenerUnsub = null;

// Módulo Corte de caja - RF6/RF7. Solo muestra las ventas ocurridas DESPUÉS
// del último corte guardado (o desde el inicio del día si aún no hay corte
// hoy), igual que la app Android.
export function renderCaja(container, navigate) {
  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="dashboard">← Volver</button>
      <h1>Corte de caja</h1>
      <button id="btn-historial" class="btn-secondary">Ver historial</button>
    </div>
    <div class="form-card">
      <p class="total-label">Total de ingresos de hoy</p>
      <p id="c-total" class="total-grande">${formatMoney(0)}</p>
      <p id="c-sin-ventas" class="empty-text" hidden>Aún no hay ventas registradas hoy</p>
      <div id="c-lista" class="lista"></div>
      <button id="c-btn-cerrar" class="btn-primary" disabled>Cerrar caja y guardar corte</button>
    </div>
    <div id="modal-root"></div>
  `;

  container.querySelector("[data-ruta='dashboard']").addEventListener("click", () => navigate("dashboard"));
  container.querySelector("#btn-historial").addEventListener("click", () => navigate("historial"));

  let ventasActuales = [];
  let totalActual = 0;
  let baselineActual = inicioDelDiaDeHoy();
  let guardandoCorte = false;

  const totalEl = container.querySelector("#c-total");
  const sinVentasEl = container.querySelector("#c-sin-ventas");
  const listaEl = container.querySelector("#c-lista");
  const btnCerrar = container.querySelector("#c-btn-cerrar");

  function suscribirVentasDesde(baseline) {
    if (ventasListenerUnsub) ventasListenerUnsub();
    const q = query(collection(db, COLLECTION_VENTAS), where("fecha", ">", baseline));
    ventasListenerUnsub = onSnapshot(q, (snapshot) => {
      const ventas = snapshot.docs
        .map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => b.fecha.toMillis() - a.fecha.toMillis());

      ventasActuales = ventas;
      totalActual = ventas.reduce((sum, v) => sum + v.montoTotal, 0);
      if (!totalEl.isConnected) return;

      totalEl.textContent = formatMoney(totalActual);
      sinVentasEl.hidden = ventas.length > 0;
      listaEl.innerHTML = ventas
        .map(
          (v) => `
          <div class="item-card">
            <div class="item-info">
              <strong>${escapeHtml(v.productoNombre)}</strong>
              <span>${v.cantidad} unidades · ${formatearFecha(v.fecha.toDate())}${
                v.clienteNombre ? ` · Cliente: ${escapeHtml(v.clienteNombre)}` : ""
              }</span>
            </div>
            <div class="item-cantidad"><span class="cantidad-num">${formatMoney(v.montoTotal)}</span></div>
          </div>`
        )
        .join("");

      if (!guardandoCorte) btnCerrar.disabled = ventas.length === 0;
    });
  }

  if (corteListenerUnsub) corteListenerUnsub();
  const qCorte = query(collection(db, COLLECTION_CORTES), orderBy("fechaCierre", "desc"), limit(1));
  corteListenerUnsub = onSnapshot(qCorte, (snapshot) => {
    const ultimoCorte = snapshot.docs[0]?.data();
    const inicioHoy = inicioDelDiaDeHoy();
    baselineActual = ultimoCorte && ultimoCorte.fechaCierre.toMillis() > inicioHoy.toMillis() ? ultimoCorte.fechaCierre : inicioHoy;
    suscribirVentasDesde(baselineActual);
  });

  btnCerrar.addEventListener("click", () => {
    if (ventasActuales.length === 0) return;
    abrirConfirmacion();
  });

  function abrirConfirmacion() {
    const modalRoot = container.querySelector("#modal-root");
    modalRoot.innerHTML = `
      <div class="modal-overlay">
        <div class="modal">
          <h2>Cerrar caja</h2>
          <p>Se guardará el corte con ${formatMoney(totalActual)} en ${ventasActuales.length} ventas. Podrás descargar el PDF después. ¿Deseas continuar?</p>
          <div class="modal-actions">
            <button id="cc-cancelar" class="btn-secondary">Cancelar</button>
            <button id="cc-confirmar" class="btn-primary">Cerrar caja y guardar corte</button>
          </div>
        </div>
      </div>
    `;
    modalRoot.querySelector("#cc-cancelar").addEventListener("click", () => (modalRoot.innerHTML = ""));
    modalRoot.querySelector("#cc-confirmar").addEventListener("click", async () => {
      modalRoot.innerHTML = "";
      await guardarCorte();
    });
  }

  async function guardarCorte() {
    guardandoCorte = true;
    btnCerrar.disabled = true;
    const ventasCorte = ventasActuales;

    const corte = {
      fechaApertura: baselineActual,
      fechaCierre: Timestamp.now(),
      totalVentas: ventasCorte.reduce((s, v) => s + v.montoTotal, 0),
      numeroVentas: ventasCorte.length,
      totalArticulosVendidos: ventasCorte.reduce((s, v) => s + v.cantidad, 0),
      usuario: auth.currentUser?.email || "",
      detalle: ventasCorte.map((v) => ({
        productoId: v.productoId,
        productoNombre: v.productoNombre,
        cantidad: v.cantidad,
        precioUnitario: v.precioUnitario,
        montoTotal: v.montoTotal,
        fecha: v.fecha
      }))
    };

    try {
      await addDoc(collection(db, COLLECTION_CORTES), corte);
      guardandoCorte = false;
      try {
        generarCortePdf(corte, ventasCorte);
        mostrarModalExito();
      } catch (e) {
        mostrarModalExito("El corte se guardó, pero no se pudo generar el PDF");
      }
    } catch (err) {
      guardandoCorte = false;
      btnCerrar.disabled = false;
      alert("No se pudo guardar el corte. Intenta de nuevo");
    }
  }

  function mostrarModalExito(mensajeAlterno) {
    const modalRoot = container.querySelector("#modal-root");
    modalRoot.innerHTML = `
      <div class="modal-overlay">
        <div class="modal">
          <h2>Corte guardado</h2>
          <p>${mensajeAlterno || "El corte se guardó correctamente en la base de datos y su PDF se descargó."}</p>
          <div class="modal-actions">
            <button id="cc-ok" class="btn-primary">Cerrar</button>
          </div>
        </div>
      </div>
    `;
    modalRoot.querySelector("#cc-ok").addEventListener("click", () => (modalRoot.innerHTML = ""));
  }
}

function inicioDelDiaDeHoy() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return Timestamp.fromDate(d);
}

function formatearFecha(fecha) {
  return fecha.toLocaleString("es-MX", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
