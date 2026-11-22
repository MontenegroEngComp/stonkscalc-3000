// Funções exibidas no gráfico: { id, expr, color, visible }
export const COLORS = ['#ff3d7f', '#2f6bff', '#00a86b', '#ff8c00', '#9b5de5', '#00a5c8', '#d62828', '#6a994e'];

let nextId = 1;

export const nextColor = (fs) =>
  COLORS.find((c) => !fs.some((f) => f.color === c)) ?? COLORS[fs.length % COLORS.length];

export const makeFunction = (expr, color) => ({ id: nextId++, expr, color, visible: true });
