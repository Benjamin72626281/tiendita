import { auth, signInWithEmailAndPassword } from "../firebase.js";

// RF3: control de acceso al sistema mediante inicio de sesión. Según el rol
// guardado en Firestore ("vendedor" o "cliente"), main.js redirige después
// al panel de administración o a la tienda de compra del cliente.
export function renderLogin(container, navigate) {
  container.innerHTML = `
    <div class="login-wrap">
      <div class="login-card">
        <div class="login-logo">🛒</div>
        <h1>Tiendita</h1>
        <p class="subtitle">Inicia sesión para administrar tu tienda</p>
        <form id="form-login">
          <label>Correo electrónico</label>
          <input type="email" id="email" required autocomplete="username" />
          <label>Contraseña</label>
          <input type="password" id="password" required autocomplete="current-password" />
          <p id="login-error" class="error-text" hidden></p>
          <button type="submit" id="btn-login" class="btn-primary">Ingresar</button>
        </form>
        <p class="link-secundario">
          <a href="#" id="link-crear-cuenta">¿Eres cliente? Crea tu cuenta para comprar</a>
        </p>
      </div>
    </div>
  `;

  container.querySelector("#link-crear-cuenta").addEventListener("click", (e) => {
    e.preventDefault();
    navigate("registro");
  });

  const form = container.querySelector("#form-login");
  const btn = container.querySelector("#btn-login");
  const errorEl = container.querySelector("#login-error");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = container.querySelector("#email").value.trim();
    const password = container.querySelector("#password").value.trim();

    if (!email || !password) {
      mostrarError("Completa correo y contraseña");
      return;
    }

    btn.disabled = true;
    btn.textContent = "Ingresando…";
    try {
      await signInWithEmailAndPassword(auth, email, password);
      // El observer onAuthStateChanged en main.js se encarga de redirigir.
    } catch (err) {
      mostrarError("No se pudo iniciar sesión. Verifica tus datos");
      btn.disabled = false;
      btn.textContent = "Ingresar";
    }
  });

  function mostrarError(msg) {
    errorEl.textContent = msg;
    errorEl.hidden = false;
  }
}
