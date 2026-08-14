# Tiendita — Panel web

Esta es la versión **web** de tu app "tiendita". Tiene los mismos módulos que
la app Android (Login, Productos, Ventas, Inventario, Corte de caja,
Asistente IA, y ahora también la **tienda de clientes** con registro y
compra propia) y usa **el mismo proyecto Firebase** (`tiendita-6771c`), así
que lo que hagas en una se ve reflejado en la otra en tiempo real — es la
misma base de datos.

No necesita build ni frameworks: es HTML/CSS/JavaScript puro, así que corre
en cualquier computadora con Node.js instalado, sin instalar Android Studio
ni nada parecido.

## 0. Requisito: Node.js

Si no lo tienes, descárgalo de https://nodejs.org (elige la versión "LTS") e
instálalo. Sirve solo para levantar un servidor local; la app en sí no usa
React ni ningún framework.

## 1. Descomprime el proyecto

Descomprime `tiendita-web.zip` en cualquier carpeta de tu computadora.

## 2. Habilita una app "Web" en tu proyecto Firebase

Tu app Android ya está conectada a `tiendita-6771c`. Para la web necesitas
registrar una app Web adicional en el MISMO proyecto (no se crea un
proyecto nuevo, ni se duplica la base de datos):

1. Abre https://console.firebase.google.com/project/tiendita-6771c/overview
2. Click en el ícono de engrane ⚙️ (arriba a la izquierda) → **Configuración
   del proyecto**.
3. Baja hasta "Tus apps". Si no tienes una app con el ícono `</>` (Web),
   click en **Agregar app** → elige el ícono `</>`.
4. Ponle un apodo, por ejemplo "tiendita-web" (NO marques "Configurar
   también Firebase Hosting" a menos que quieras publicarla en internet más
   adelante).
5. Click en **Registrar app**. Te va a mostrar un bloque de código con un
   objeto `firebaseConfig = { apiKey: ..., authDomain: ..., ... }`.
   **Copia ese objeto completo**, lo vas a necesitar en el paso 4.

## 3. Revisa las reglas de Firestore (deben permitir usuarios con sesión)

Si ya seguiste `CONECTAR_FIREBASE.md` de la app Android, esto ya está
hecho y puedes saltarte este paso. Si no, en el menú lateral: **Compilación →
Firestore Database → Reglas**, y confirma que diga:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Esto permite que la web (con un usuario logueado, igual que en Android) lea
y escriba los mismos `productos`, `ventas` y `cortes`.

## 4. Configura tus claves

Dentro de la carpeta `tiendita-web/js/`:

1. Copia el archivo `config.example.js` y renómbralo a **`config.js`**
   (mismo lugar, carpeta `js/`).
2. Ábrelo con cualquier editor de texto (Bloc de notas, VS Code, etc.) y
   pega:
   - El objeto `firebaseConfig` que copiaste en el paso 2.
   - Tus claves `GEMINI_API_KEY` y `GROQ_API_KEY` — las mismas que ya usas
     en `local.properties` de la app Android (si no las tienes a la mano,
     revisa `CONECTAR_CHATBOT.md` del proyecto Android para saber cómo
     conseguirlas).

`config.js` **no se sube a git** (ver `.gitignore`), así que ahí es seguro
pegar tus claves reales.

> ⚠️ **Importante sobre seguridad:** en una app web, cualquier clave dentro
> del código JavaScript que corre en el navegador es técnicamente visible
> para quien abra las herramientas de desarrollador. Esto es aceptable para
> correr la app en tu propia computadora o en una red interna de la tienda.
> Si más adelante quieres publicarla en internet para que cualquiera la
> use, lo recomendable es mover las llamadas a Gemini/Groq a una función en
> la nube (Cloud Function) para no exponer las claves — puedo ayudarte con
> eso cuando lo necesites.

## 5. Instala y corre el servidor local

Abre una terminal (CMD, PowerShell o Terminal) dentro de la carpeta
`tiendita-web` y corre:

```
npm install
npm start
```

Vas a ver un mensaje como `Accepting connections at http://localhost:5173`.
Abre esa dirección en tu navegador (Chrome, Edge, etc.).

## 6. Inicia sesión

Usa el mismo correo y contraseña que ya creaste en Firebase Authentication
para la app Android (por ejemplo `duenio@tiendita.com`). Es el mismo usuario
para ambas.

## 7. Ya está funcionando

- Todo lo que agregues/edites en **Productos**, **Ventas**, **Inventario** y
  **Corte de caja** desde la web se guarda en la misma base de datos que usa
  el celular, y aparece ahí al instante (y viceversa).
- El **Asistente IA** responde usando los datos reales de tu tienda, igual
  que en la app — usa Gemini primero y, si falla, Groq automáticamente.
- El corte de caja genera y descarga un PDF a tu carpeta de Descargas, igual
  que en Android.
- Desde la pantalla de inicio de sesión, cualquier persona puede darle clic a
  "¿Eres cliente? Crea tu cuenta para comprar" para registrarse como cliente.
  Al iniciar sesión como cliente, en vez del panel del vendedor se le muestra
  el catálogo de productos con un carrito (selector +/- por producto). Al
  pagar, se descuenta el stock, se registran las ventas con su nombre (y
  aparecen marcadas como "Cliente: ..." en el corte de caja del vendedor) y
  se descarga automáticamente su tiquet en PDF — igual que en la app Android.

## Cada vez que quieras volver a usarla

No necesitas repetir todo el proceso — solo:

```
npm start
```

dentro de la carpeta `tiendita-web`, y abre `http://localhost:5173`.

## Resumen de archivos importantes

| Archivo/carpeta | Qué es |
| --- | --- |
| `index.html` | Punto de entrada de la app |
| `js/config.js` | Tus claves (Firebase + Gemini/Groq) — tú lo creas, no viene en el zip |
| `js/firebase.js` | Conexión a Firebase (Auth + Firestore) |
| `js/pages/` | Cada pantalla: login, registro (cliente), dashboard, productos, ventas, inventario, caja, historial, chatbot, tienda (cliente) |
| `js/utils/chatRepository.js` | Lógica del asistente IA (contexto + Gemini/Groq) |
| `js/utils/pdf.js` | Generación del PDF del corte de caja y del tiquet de compra del cliente |
| `js/utils/session.js` | Estado del rol de sesión (vendedor/cliente) compartido entre páginas |
