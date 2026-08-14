// RF4: alerta de stock bajo. Usa las notificaciones del navegador (equivalente
// web a las notificaciones push de la app Android). Si el usuario no ha dado
// permiso, se lo pide la primera vez.
export function pedirPermisoNotificaciones() {
  if ("Notification" in window && Notification.permission === "default") {
    Notification.requestPermission().catch(() => {});
  }
}

const yaNotificados = new Set();

export function notificarStockBajo(nombreProducto, cantidad) {
  const clave = `${nombreProducto}-${cantidad}`;
  if (yaNotificados.has(clave)) return;
  yaNotificados.add(clave);

  if ("Notification" in window && Notification.permission === "granted") {
    new Notification("Stock bajo", {
      body: `${nombreProducto} tiene solo ${cantidad} unidades disponibles`
    });
  }
}
