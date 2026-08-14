// Colecciones de Firestore (las MISMAS que usa la app Android, para que
// ambas apps compartan exactamente la misma base de datos).
export const COLLECTION_PRODUCTOS = "productos";
export const COLLECTION_VENTAS = "ventas";
export const COLLECTION_CORTES = "cortes";
export const COLLECTION_USUARIOS = "usuarios";

// RF4: nivel de stock bajo para notificar/alertar.
export const UMBRAL_STOCK_BAJO = 5;

// Roles de usuario (igual que Constants.kt en la app Android).
export const ROL_VENDEDOR = "vendedor";
export const ROL_CLIENTE = "cliente";
