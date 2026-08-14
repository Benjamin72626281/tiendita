import { auth, signOut } from "../firebase.js";

export function renderDashboard(container, navigate) {
  container.innerHTML = `
    <div class="topbar topbar-home">
      <div class="brand">
        <span class="brand-logo">🛒</span>
        <div>
          <h1>Tiendita</h1>
          <p class="brand-subtitle">Panel de administración</p>
        </div>
      </div>
      <button id="btn-logout" class="btn-secondary">Cerrar sesión</button>
    </div>
    <div class="grid-cards">
      <button class="card" data-ruta="productos">
        <span class="card-icon icon-azul">📦</span>
        <span>Productos</span>
      </button>
      <button class="card" data-ruta="ventas">
        <span class="card-icon icon-verde">🧾</span>
        <span>Registrar venta</span>
      </button>
      <button class="card" data-ruta="inventario">
        <span class="card-icon icon-morado">📊</span>
        <span>Inventario</span>
      </button>
      <button class="card" data-ruta="caja">
        <span class="card-icon icon-ambar">💰</span>
        <span>Corte de caja</span>
      </button>
      <button class="card card-ancho" data-ruta="chatbot">
        <span class="card-icon icon-rosa">🤖</span>
        <span>Asistente IA</span>
      </button>
    </div>
  `;

  container.querySelectorAll(".card").forEach((card) => {
    card.addEventListener("click", () => navigate(card.dataset.ruta));
  });

  container.querySelector("#btn-logout").addEventListener("click", async () => {
    await signOut(auth);
  });
}
