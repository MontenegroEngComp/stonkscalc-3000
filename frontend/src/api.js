// Cliente da API Java. Em dev, o Vite faz proxy de /api para http://localhost:8080.
const BASE = import.meta.env.VITE_API_URL ?? '';

const OFFLINE = {
  ok: false,
  offline: true,
  error: 'O backend Java está fora do ar. Rode: cd backend && .\\build.ps1 -Run',
};

async function request(path, body) {
  try {
    const res = await fetch(BASE + path, {
      method: body ? 'POST' : 'GET',
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    try {
      return await res.json();
    } catch {
      return res.status >= 500 || res.status === 404 ? OFFLINE : { ok: false, error: 'Resposta inválida do servidor.' };
    }
  } catch {
    return OFFLINE;
  }
}

export const api = {
  evaluate: (expression, angleMode, variables) =>
    request('/api/evaluate', { expression, angleMode, variables }),
  derivative: (expression, { variable = 'x', order = 1, at } = {}) =>
    request('/api/derivative', { expression, variable, order, at: at === '' ? undefined : at }),
  plot: (functions, { xMin, xMax, samples, angleMode, roots }) =>
    request('/api/plot', { functions, xMin, xMax, samples, angleMode, roots }),
  roots: (expression, a, b, angleMode) => request('/api/roots', { expression, a, b, angleMode }),
  functions: () => request('/api/functions'),
};
