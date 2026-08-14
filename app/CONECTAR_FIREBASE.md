# Cómo conectar "tiendita" a tu proyecto Firebase

La app ya está programada por completo (Login, Productos, Ventas, Inventario y
Corte de caja). Solo falta conectarla a TU proyecto Firebase (el que se ve en
tu captura: **tiendita-6771c**). Sigue estos pasos exactamente en este orden.

## 1. Registrar la app Android en Firebase

1. Abre https://console.firebase.google.com/project/tiendita-6771c/overview
2. Click en **"Agregar app"** → elige el ícono de **Android**.
3. En "Nombre del paquete de Android" escribe exactamente:
   `com.example.tiendita`
4. (Opcional) Alias de la app: "tiendita".
5. Click en **Registrar app**.
6. Firebase te va a ofrecer descargar un archivo llamado **google-services.json**.
   Descárgalo.

## 2. Colocar google-services.json en el proyecto

Copia el archivo `google-services.json` que descargaste y pégalo dentro de la
carpeta:

```
tiendita/app/google-services.json
```

(al mismo nivel que `app/build.gradle.kts`, NO dentro de `src`).

Salta las pantallas de "Agregar SDK de Firebase" y "Agregar código de
inicialización" del asistente de Firebase — eso ya está hecho en este
proyecto (el plugin `google-services` y las dependencias ya están en los
archivos `build.gradle.kts`).

## 3. Activar Authentication (para el login, RF3)

1. En el menú lateral de Firebase: **Compilación → Authentication**.
2. Click en **Comenzar**.
3. En la pestaña "Sign-in method", habilita el proveedor **Correo
   electrónico/contraseña** (Email/Password).
4. Ve a la pestaña **Users** y click en **Agregar usuario**. Crea el usuario
   del dueño/encargado, por ejemplo:
   - Correo: `duenio@tiendita.com`
   - Contraseña: la que tú definas (mínimo 6 caracteres)

   Con esas credenciales entrará a la app.

## 4. Activar Firestore Database (para productos, ventas e inventario)

1. En el menú lateral: **Compilación → Firestore Database**.
2. Click en **Crear base de datos**.
3. Elige la ubicación (por ejemplo `us-central` o la más cercana a México).
4. Modo de inicio: elige **Modo de producción** (más seguro). Luego entra a
   la pestaña **Reglas** y reemplaza el contenido con esto para que solo
   usuarios con sesión iniciada puedan leer/escribir:

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

5. Click en **Publicar**.

No necesitas crear las colecciones `productos` ni `ventas` a mano: la app
las crea automáticamente en Firestore la primera vez que agregas un producto
o registras una venta.

## 5. Abrir y correr el proyecto

1. Abre Android Studio → **Open** → selecciona la carpeta `tiendita`
   (la que contiene `settings.gradle.kts`).
2. Deja que Gradle sincronice (puede tardar la primera vez porque descarga
   las librerías de Firebase).
3. Conecta un dispositivo/emulador y presiona **Run ▶**.
4. En la pantalla de login, entra con el correo y contraseña que creaste en
   el paso 3.

## Qué hace cada módulo (mapeo con el documento de requisitos)

| Módulo en la app        | Requisito cubierto |
| ------------------------ | ------------------- |
| Login (MainActivity)     | RF3 – control de acceso |
| Productos (CRUD)         | RF5 – registro de productos |
| Ventas                   | RF1 – control de ventas, RF2 – descuenta inventario automáticamente |
| Inventario               | RF2 – entradas/salidas, RF4 – notificación de stock bajo (⩽ 5 unidades, configurable en `util/Constants.kt`) |
| Corte de caja            | RF6 – total de ingresos del día |
| Firestore con persistencia offline | RNF2 – funciona sin internet y sincroniza al reconectar |
| `util/MoneyUtil.kt`      | RNF3 – pesos mexicanos con 2 decimales |
| Layouts Material simples | RNF4 – fácil de usar para una sola persona |
| Compatible con celular/tablet (ConstraintLayout responsivo) | RNF1 |

## Notas

- El umbral de "stock bajo" está en `app/src/main/java/com/example/tiendita/util/Constants.kt`
  (`UMBRAL_STOCK_BAJO = 5`). Cámbialo si tu cliente quiere otro número.
- Firestore ya trae persistencia offline activada por defecto en Android, así
  que aunque la tienda no tenga internet en ese momento, se puede seguir
  vendiendo/registrando y se sincroniza solo cuando vuelva la conexión (RNF2).
- Si más adelante quieres que varios empleados tengan su propio usuario,
  simplemente agrega más usuarios en Authentication → Users.
