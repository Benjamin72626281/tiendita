import { db } from "../firebase.js";
import {
  collection,
  getDocs,
  query,
  orderBy,
  limit
} from "https://www.gstatic.com/firebasejs/10.13.2/firebase-firestore.js";
import { COLLECTION_PRODUCTOS, COLLECTION_VENTAS, COLLECTION_CORTES, UMBRAL_STOCK_BAJO } from "./constants.js";
import { GEMINI_API_KEY, GROQ_API_KEY } from "../config.js";

// Arma un resumen en texto de los datos reales de la tienda (productos,
// ventas recientes y cortes de caja) guardados en Firestore, y se lo pasa
// como contexto al modelo de IA. Usa Gemini como proveedor principal y Groq
// como respaldo automático — igual que ChatRepository.kt en la app Android.

async function construirContexto() {
  const productosSnap = await getDocs(collection(db, COLLECTION_PRODUCTOS));
  const productos = productosSnap.docs
    .map((d) => d.data())
    .sort((a, b) => a.nombre.toLowerCase().localeCompare(b.nombre.toLowerCase()));

  const ventasSnap = await getDocs(query(collection(db, COLLECTION_VENTAS), orderBy("fecha", "desc"), limit(30)));
  const ventas = ventasSnap.docs.map((d) => d.data());

  const cortesSnap = await getDocs(query(collection(db, COLLECTION_CORTES), orderBy("fechaCierre", "desc"), limit(5)));
  const cortes = cortesSnap.docs.map((d) => d.data());

  const fmt = (fecha) =>
    fecha.toDate().toLocaleString("es-MX", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });

  let texto = `== PRODUCTOS EN INVENTARIO (${productos.length}) ==\n`;
  if (productos.length === 0) {
    texto += "No hay productos registrados todavía.\n";
  } else {
    productos.forEach((p) => {
      texto += `- ${p.nombre}: precio compra $${p.precioCompra}, precio venta $${p.precioVenta}, cantidad disponible ${p.cantidad}${
        p.cantidad <= UMBRAL_STOCK_BAJO ? " (STOCK BAJO)" : ""
      }\n`;
    });
  }

  texto += `\n== ÚLTIMAS VENTAS (máx. 30 más recientes) ==\n`;
  if (ventas.length === 0) {
    texto += "No hay ventas registradas todavía.\n";
  } else {
    ventas.forEach((v) => {
      texto += `- ${fmt(v.fecha)}: ${v.cantidad} x ${v.productoNombre} = $${v.montoTotal}\n`;
    });
  }

  texto += `\n== ÚLTIMOS CORTES DE CAJA (máx. 5 más recientes) ==\n`;
  if (cortes.length === 0) {
    texto += "No hay cortes de caja guardados todavía.\n";
  } else {
    cortes.forEach((c) => {
      texto += `- Corte del ${fmt(c.fechaCierre)} por ${c.usuario}: total $${c.totalVentas}, ${c.numeroVentas} ventas, ${c.totalArticulosVendidos} artículos\n`;
    });
  }

  return texto;
}

function construirSystemPrompt(contexto) {
  return `Eres el asistente virtual de "Tiendita", una app para administrar una tienda de abarrotes.
Respondes siempre en español, de forma breve, clara y amigable, como si hablaras con el
dueño de la tienda. Usa ÚNICAMENTE los datos de la tienda que se muestran abajo para
responder preguntas sobre productos, precios, existencias, ventas y cortes de caja.
Puedes hacer cálculos simples (sumas, promedios, comparaciones) a partir de esos datos.
Si te preguntan algo que no está en estos datos, dilo honestamente en vez de inventar
información.

DATOS ACTUALES DE LA TIENDA:
${contexto}`;
}

async function generarGemini(systemPrompt, historial, mensajeUsuario) {
  if (!GEMINI_API_KEY || GEMINI_API_KEY.startsWith("PON_AQUI")) {
    throw new Error("Falta configurar GEMINI_API_KEY en js/config.js");
  }
  const modelo = "gemini-2.0-flash";
  const contents = historial.map((m) => ({
    role: m.role === "user" ? "user" : "model",
    parts: [{ text: m.texto }]
  }));
  contents.push({ role: "user", parts: [{ text: mensajeUsuario }] });

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelo}:generateContent?key=${GEMINI_API_KEY}`;
  const resp = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      system_instruction: { parts: [{ text: systemPrompt }] },
      contents,
      generationConfig: { temperature: 0.4, maxOutputTokens: 700 }
    })
  });
  const bodyText = await resp.text();
  if (!resp.ok) throw new Error(`Gemini respondió con error ${resp.status}: ${bodyText}`);
  const json = JSON.parse(bodyText);
  const texto = json.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!texto) throw new Error(`Gemini no devolvió una respuesta válida: ${bodyText}`);
  return texto.trim();
}

async function generarGroq(systemPrompt, historial, mensajeUsuario) {
  if (!GROQ_API_KEY || GROQ_API_KEY.startsWith("PON_AQUI")) {
    throw new Error("Falta configurar GROQ_API_KEY en js/config.js");
  }
  const modelo = "llama-3.3-70b-versatile";
  const mensajes = [{ role: "system", content: systemPrompt }];
  historial.forEach((m) => mensajes.push({ role: m.role === "user" ? "user" : "assistant", content: m.texto }));
  mensajes.push({ role: "user", content: mensajeUsuario });

  const resp = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${GROQ_API_KEY}` },
    body: JSON.stringify({ model: modelo, messages: mensajes, temperature: 0.4, max_tokens: 700 })
  });
  const bodyText = await resp.text();
  if (!resp.ok) throw new Error(`Groq respondió con error ${resp.status}: ${bodyText}`);
  const json = JSON.parse(bodyText);
  const texto = json.choices?.[0]?.message?.content;
  if (!texto) throw new Error(`Groq no devolvió una respuesta válida: ${bodyText}`);
  return texto.trim();
}

// Envía la pregunta del usuario al proveedor principal (Gemini); si falla,
// reintenta automáticamente con el respaldo (Groq).
export async function preguntar(historial, mensajeUsuario) {
  const systemPrompt = construirSystemPrompt(await construirContexto());

  try {
    return { respuesta: await generarGemini(systemPrompt, historial, mensajeUsuario), proveedor: "Gemini" };
  } catch (errorPrincipal) {
    try {
      return { respuesta: await generarGroq(systemPrompt, historial, mensajeUsuario), proveedor: "Groq" };
    } catch (errorRespaldo) {
      throw new Error(
        `No se pudo obtener respuesta de ningún proveedor de IA.\nGemini: ${errorPrincipal.message}\nGroq: ${errorRespaldo.message}`
      );
    }
  }
}
