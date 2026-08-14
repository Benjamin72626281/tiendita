import { auth, db, onAuthStateChanged } from "./firebase.js";
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { renderLogin } from "./pages/login.js";
import { renderRegistro } from "./pages/registro.js";
import { renderDashboard } from "./pages/dashboard.js";
import { renderProductos } from "./pages/productos.js";
import { renderVentas } from "./pages/ventas.js";
import { renderInventario } from "./pages/inventario.js";
import { renderCaja } from "./pages/caja.js";
import { renderHistorial } from "./pages/historial.js";
import { renderChatbot } from "./pages/chatbot.js";
import { renderTiendaCliente } from "./pages/tienda.js";
import { COLLECTION_USUARIOS, ROL_CLIENTE } from "./utils/constants.js";
import { sesion } from "./utils/session.js";

const app = document.getElementById("app");

// RF3: Control de acceso al sistema mediante inicio de sesión. Según el rol
// guardado en Firestore ("vendedor" o "cliente"), se redirige al panel de
// administración o a la tienda de compra del cliente (igual que
// MainActivity.kt resuelve el rol y navega en la app Android).
const rutasVendedor = {
  dashboard: renderDashboard,
  productos: renderProductos,
  ventas: renderVentas,
  inventario: renderInventario,
  caja: renderCaja,
  historial: renderHistorial,
  chatbot: renderChatbot
};

function navigate(ruta) {
  window.location.hash = `#/${ruta}`;
}

function nombreRutaActual() {
  return (window.location.hash || "").replace("#/", "");
}

function renderSegunSesion() {
  if (sesion.rol === ROL_CLIENTE) {
    renderTiendaCliente(app, sesion.nombreCliente, navigate);
    return;
  }
  const nombreRuta = nombreRutaActual() || "dashboard";
  const render = rutasVendedor[nombreRuta] || renderDashboard;
  render(app, navigate);
}

async function resolverRolYRenderizar(user) {
  try {
    const snap = await getDoc(doc(db, COLLECTION_USUARIOS, user.uid));
    const datos = snap.exists() ? snap.data() : null;
    if (datos && datos.rol === ROL_CLIENTE) {
      sesion.rol = ROL_CLIENTE;
      sesion.nombreCliente = datos.nombre || "";
    } else {
      // Si no existe el documento (p. ej. cuentas del vendedor creadas
      // manualmente antes de que existiera este sistema de roles), se
      // asume "vendedor" para no romper el acceso del dueño de la tienda.
      sesion.rol = "vendedor";
    }
  } catch (e) {
    sesion.rol = "vendedor";
  }
  if (sesion.rol === ROL_CLIENTE && nombreRutaActual() !== "tienda") {
    window.location.hash = "#/tienda"; // dispara hashchange, que vuelve a renderizar
  } else {
    if (nombreRutaActual() === "registro") window.location.hash = "";
    renderSegunSesion();
  }
}

window.addEventListener("hashchange", () => {
  if (sesion.registroEnCurso) return;
  if (auth.currentUser) {
    renderSegunSesion();
  } else if (nombreRutaActual() === "registro") {
    renderRegistro(app, navigate);
  } else {
    renderLogin(app, navigate);
  }
});

onAuthStateChanged(auth, (user) => {
  if (sesion.registroEnCurso) return;
  if (user) {
    resolverRolYRenderizar(user);
  } else {
    sesion.rol = null;
    sesion.nombreCliente = "";
    if (nombreRutaActual() === "registro") {
      renderRegistro(app, navigate);
    } else {
      window.location.hash = "";
      renderLogin(app, navigate);
    }
  }
});
