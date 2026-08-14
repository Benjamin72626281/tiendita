import { auth, createUserWithEmailAndPassword, db } from "../firebase.js";
import { doc, setDoc, Timestamp } from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_USUARIOS, ROL_CLIENTE } from "../utils/constants.js";
import { sesion } from "../utils/session.js";

// Permite que un cliente cree su propia cuenta para comprar directo desde
// la página. El nombre que registra aquí es el mismo que después aparece
// en su tiquet de compra y en el detalle de ventas del corte de caja del
// vendedor (equivalente web de RegistroClienteActivity.kt en Android).
export function renderRegistro(container, navigate) {
  container.innerHTML = `
    <div class="login-wrap">
      <div class="login-card">
        <div class="login-logo">🛍️</div>
        <h1>Crear cuenta de cliente</h1>
        <p class="subtitle">Regístrate para comprar directo desde la tienda</p>
        <form id="form-registro">
          <label>Nombre completo</label>
          <input type="text" id="r-nombre" required autocomplete="name" />
          <label>Correo electrónico</label>
          <input type="email" id="r-email" required autocomplete="username" />
          <label>Contraseña</label>
          <input type="password" id="r-password" required autocomplete="new-password" />
          <label>Confirmar contraseña</label>
          <input type="password" id="r-confirmar" required autocomplete="new-password" />
          <p id="r-error" class="error-text" hidden></p>
          <button type="submit" id="r-btn" class="btn-primary">Crear cuenta</button>
        </form>
        <p class="link-secundario">
          <a href="#" id="r-ya-tengo">Ya tengo cuenta, iniciar sesión</a>
        </p>
      </div>
    </div>
  `;

  const form = container.querySelector("#form-registro");
  const btn = container.querySelector("#r-btn");
  const errorEl = container.querySelector("#r-error");

  container.querySelector("#r-ya-tengo").addEventListener("click", (e) => {
    e.preventDefault();
    navigate("");
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    errorEl.hidden = true;

    const nombre = container.querySelector("#r-nombre").value.trim();
    const email = container.querySelector("#r-email").value.trim();
    const password = container.querySelector("#r-password").value.trim();
    const confirmar = container.querySelector("#r-confirmar").value.trim();

    if (!nombre) return mostrarError("Escribe tu nombre completo");
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return mostrarError("Escribe un correo electrónico válido");
    }
    if (password.length < 6) return mostrarError("La contraseña debe tener al menos 6 caracteres");
    if (password !== confirmar) return mostrarError("Las contraseñas no coinciden");

    btn.disabled = true;
    btn.textContent = "Creando cuenta…";
    // Se avisa a main.js que no intervenga: este flujo fija el rol
    // directamente para evitar una carrera con onAuthStateChanged mientras
    // el documento del usuario todavía se está guardando en Firestore.
    sesion.registroEnCurso = true;

    try {
      const credencial = await createUserWithEmailAndPassword(auth, email, password);
      await setDoc(doc(db, COLLECTION_USUARIOS, credencial.user.uid), {
        nombre,
        correo: email,
        rol: ROL_CLIENTE,
        fechaRegistro: Timestamp.now()
      });
      sesion.rol = ROL_CLIENTE;
      sesion.nombreCliente = nombre;
      sesion.registroEnCurso = false;
      navigate("tienda");
    } catch (err) {
      sesion.registroEnCurso = false;
      btn.disabled = false;
      btn.textContent = "Crear cuenta";
      mostrarError(
        err.code === "auth/email-already-in-use"
          ? "Ese correo ya tiene una cuenta registrada"
          : "No se pudo crear la cuenta. Intenta de nuevo"
      );
    }
  });

  function mostrarError(msg) {
    errorEl.textContent = msg;
    errorEl.hidden = false;
  }
}
