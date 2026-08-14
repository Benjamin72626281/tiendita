import { preguntar } from "../utils/chatRepository.js";

let mensajes = [];

export function renderChatbot(container, navigate) {
  mensajes = [{ role: "model", texto: "¡Hola! Soy el asistente de tu tienda. Pregúntame sobre tus productos, precios, existencias, ventas o cortes de caja." }];

  container.innerHTML = `
    <div class="topbar">
      <button class="btn-back" data-ruta="dashboard">← Volver</button>
      <h1>Asistente IA</h1>
    </div>
    <div class="chat-wrap">
      <div id="chat-lista" class="chat-lista"></div>
      <div id="chat-cargando" class="chat-cargando" hidden>Escribiendo…</div>
      <form id="chat-form" class="chat-form">
        <input type="text" id="chat-input" placeholder="Escribe tu pregunta…" autocomplete="off" />
        <button type="submit" id="chat-enviar">Enviar</button>
      </form>
    </div>
  `;

  container.querySelector("[data-ruta='dashboard']").addEventListener("click", () => navigate("dashboard"));

  const chatLista = container.querySelector("#chat-lista");
  const input = container.querySelector("#chat-input");
  const form = container.querySelector("#chat-form");
  const cargando = container.querySelector("#chat-cargando");
  const btnEnviar = container.querySelector("#chat-enviar");

  pintarMensajes();

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const texto = input.value.trim();
    if (!texto) return;

    const historialPrevio = mensajes.slice();
    mensajes.push({ role: "user", texto });
    input.value = "";
    pintarMensajes();
    cargando.hidden = false;
    btnEnviar.disabled = true;

    try {
      const { respuesta } = await preguntar(historialPrevio.slice(-12), texto);
      mensajes.push({ role: "model", texto: respuesta });
    } catch (err) {
      mensajes.push({ role: "model", texto: `No pude obtener una respuesta. Detalle: ${err.message}` });
    } finally {
      cargando.hidden = true;
      btnEnviar.disabled = false;
      pintarMensajes();
    }
  });

  function pintarMensajes() {
    chatLista.innerHTML = mensajes
      .map(
        (m) => `<div class="chat-burbuja ${m.role === "user" ? "chat-usuario" : "chat-bot"}">${escapeHtml(m.texto).replace(/\n/g, "<br>")}</div>`
      )
      .join("");
    chatLista.scrollTop = chatLista.scrollHeight;
  }
}

function escapeHtml(str = "") {
  return str.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
