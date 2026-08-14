// RNF3: manejo de cifras en pesos mexicanos, con 2 decimales.
const formatter = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "MXN",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

export function formatMoney(amount) {
  return formatter.format(Number(amount) || 0);
}
