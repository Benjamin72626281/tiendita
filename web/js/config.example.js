// ============================================================================
// COPIA este archivo y renómbralo a "config.js" (en esta misma carpeta js/).
// "config.js" NO se sube a git (mira .gitignore) porque ahí van tus claves
// reales. Este archivo de ejemplo sí se sube, para que sepas qué formato usar.
// ============================================================================

// 1) Configuración de tu proyecto Firebase (el MISMO que usa la app Android
//    "tiendita-6771c"). La sacas en:
//    Firebase Console -> ⚙️ Configuración del proyecto -> Tus apps ->
//    (si no tienes una app "Web" todavía, créala con el ícono </>) ->
//    "Configuración del SDK" -> copia el objeto firebaseConfig de ahí.
export const firebaseConfig = {
  apiKey: "PON_AQUI_TU_API_KEY",
  // Estos 3 campos ya son los del proyecto real de la app Android
  // (tiendita-6771c), tal como aparecen en app/google-services.json.
  // No los cambies: son los mismos para que web y Android compartan
  // exactamente la misma base de datos.
  authDomain: "tiendita-6771c.firebaseapp.com",
  projectId: "tiendita-6771c",
  storageBucket: "tiendita-6771c.firebasestorage.app",
  messagingSenderId: "138847313210",
  appId: "PON_AQUI_TU_APP_ID"
};

// 2) Claves del asistente de IA (chatbot). Las mismas que ya usas en la app
//    Android (local.properties -> GEMINI_API_KEY / GROQ_API_KEY).
export const GEMINI_API_KEY = "PON_AQUI_TU_API_KEY_DE_GEMINI";
export const GROQ_API_KEY = "PON_AQUI_TU_API_KEY_DE_GROQ";
