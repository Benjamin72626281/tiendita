# Cómo activar el Asistente IA (chatbot) de "tiendita"

Ya está todo programado: pantalla de chat, lectura de productos/ventas/cortes
desde Firestore, y conexión con **Gemini** (proveedor principal) y **Groq**
(respaldo automático). Solo te faltan 2 cosas: conseguir las API keys y
pegarlas en un archivo. Sigue estos pasos en orden.

## 1. Consigue tu API key de Gemini (gratis)

1. Entra a https://aistudio.google.com/apikey
2. Inicia sesión con tu cuenta de Google.
3. Click en **"Create API key"** (o "Crear clave de API").
4. Copia la clave que te muestra (empieza algo así: `AIzaSy...`).

## 2. Consigue tu API key de Groq (gratis)

1. Entra a https://console.groq.com/keys
2. Inicia sesión / regístrate.
3. Click en **"Create API Key"**.
4. Ponle un nombre (por ejemplo "tiendita") y copia la clave que te muestra
   (empieza algo así: `gsk_...`). **Solo se muestra una vez**, guárdala.

## 3. Pega las claves en `local.properties`

Abre el archivo `local.properties` (está en la raíz del proyecto, junto a
`settings.gradle.kts`) y busca estas dos líneas al final:

```
GEMINI_API_KEY=PON_AQUI_TU_API_KEY_DE_GEMINI
GROQ_API_KEY=PON_AQUI_TU_API_KEY_DE_GROQ
```

Reemplaza los valores por tus claves reales, por ejemplo:

```
GEMINI_API_KEY=AIzaSyD-xxxxxxxxxxxxxxxxxxxxxxxxxxx
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

`local.properties` **nunca se sube a git** (ya está en `.gitignore`), así que
tus claves quedan seguras y solo en tu computadora. Tampoco quedan escritas
en ningún archivo del código fuente: se leen desde `local.properties` y se
inyectan en tiempo de compilación mediante `BuildConfig` (ver
`app/build.gradle.kts`).

## 4. Sincroniza y corre el proyecto

1. Abre Android Studio con el proyecto ya abierto (o vuelve a abrirlo si lo
   cerraste).
2. Click en **"Sync Now"** / **"Sync Project with Gradle Files"** (el ícono
   del elefante con la flecha) para que tome las nuevas dependencias
   (OkHttp y Coroutines) y regenere `BuildConfig` con tus claves.
3. Conecta un dispositivo/emulador y presiona **Run ▶**.
4. Entra con tu usuario, ve al Dashboard y toca la tarjeta verde
   **"Asistente IA"**.

## 5. Prueba el chatbot

Algunas preguntas que puedes hacerle (usa tus datos reales de Firestore):

- "¿Cuántos productos tengo registrados?"
- "¿Qué producto tiene el precio de venta más alto?"
- "¿Cuánto vendí en total hoy?" / "¿Cuáles fueron mis últimas 5 ventas?"
- "¿Qué productos están con stock bajo?"
- "¿Cuánto fue el total del último corte de caja?"

El asistente responde solo con base en los datos reales que están en tu
Firestore (productos, últimas 30 ventas y últimos 5 cortes de caja), así que
si le preguntas algo fuera de eso te dirá que no tiene esa información en
vez de inventar una respuesta.

## ¿Cómo funciona el respaldo automático (fallback)?

Cada vez que envías un mensaje, la app:

1. Intenta responder con **Gemini** primero.
2. Si Gemini falla por cualquier motivo (se acabaron los intentos/cuota
   gratuita del día, error de red, error del servidor, etc.), la app
   **automáticamente** vuelve a intentar la misma pregunta con **Groq**, sin
   que tengas que hacer nada.
3. Solo si ambos fallan, verás un mensaje de error en el chat con el detalle
   de lo que pasó (útil para diagnosticar, por ejemplo, si olvidaste pegar
   alguna de las dos claves).

Esta lógica está en:
`app/src/main/java/com/example/tiendita/chatbot/ai/ChatRepository.kt`

Si más adelante quieres cambiar el modelo usado de cada proveedor, están
aquí:
- `app/src/main/java/com/example/tiendita/chatbot/ai/GeminiProvider.kt`
  (por defecto usa `gemini-2.0-flash`, que tiene buena cuota gratuita).
- `app/src/main/java/com/example/tiendita/chatbot/ai/GroqProvider.kt`
  (por defecto usa `llama-3.3-70b-versatile`).

## Archivos que se agregaron para el chatbot

| Archivo | Qué hace |
| --- | --- |
| `chatbot/ChatbotActivity.kt` | Pantalla del chat (envía mensajes, muestra historial) |
| `chatbot/ChatAdapter.kt` | Dibuja las burbujas de usuario/asistente en el RecyclerView |
| `chatbot/ai/AiProvider.kt` | Contrato común para cualquier proveedor de IA |
| `chatbot/ai/GeminiProvider.kt` | Llama a la API de Gemini |
| `chatbot/ai/GroqProvider.kt` | Llama a la API de Groq (respaldo) |
| `chatbot/ai/ChatRepository.kt` | Arma el contexto desde Firestore + aplica el fallback Gemini→Groq |
| `model/ChatMessage.kt` | Modelo simple de mensaje (rol + texto) |
| `res/layout/activity_chatbot.xml` | Layout de la pantalla del chat |
| `res/layout/item_chat_usuario.xml` / `item_chat_bot.xml` | Burbujas del chat |

## Notas y límites a tener en cuenta

- Los planes gratuitos de Gemini y Groq tienen límites de solicitudes por
  minuto/día. El fallback ayuda a que casi nunca te quedes sin respuesta,
  pero si ambos se agotan al mismo tiempo, verás el mensaje de error en el
  chat.
- Cada pregunta vuelve a leer Firestore (productos, últimas 30 ventas y
  últimos 5 cortes) para que el asistente siempre tenga datos actualizados.
- Si cambias de dispositivo o reinstalas el proyecto en otra computadora,
  recuerda repetir el paso 3 (`local.properties` no viaja con git).
