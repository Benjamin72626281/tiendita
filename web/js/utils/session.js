// Estado compartido de sesión entre main.js y las páginas de autenticación
// (login/registro/tienda), para que el flujo de registro de un cliente
// pueda fijar directamente su rol sin esperar (y sin pisarse) con el
// listener global onAuthStateChanged de main.js.
export const sesion = {
  rol: null, // "vendedor" | "cliente" | null (aún sin resolver)
  nombreCliente: "",
  registroEnCurso: false
};
