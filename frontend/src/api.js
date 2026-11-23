// Cliente do motor matemático Java. Há dois modos, com a mesma API:
//  - servidor: chamadas HTTP para o backend Java (em dev, o Vite faz proxy de /api para a porta 8080);
//  - navegador: o mesmo motor Java compilado para JavaScript pelo TeaVM, rodando num Web Worker.
// O build do GitHub Pages usa VITE_ENGINE=browser. Nos outros casos, se o servidor estiver
// fora do ar, o cliente tenta o motor do navegador antes de desistir.
const API_URL = import.meta.env.VITE_API_URL ?? '';
const FORCE_BROWSER = import.meta.env.VITE_ENGINE === 'browser';

const OFFLINE = {
  ok: false,
  offline: true,
  error: 'O motor Java está fora do ar. Rode: .\\start.ps1 (ou cd backend && .\\build.ps1 -Run)',
};

// ---------------------------------------------------------------- motor no navegador
let worker = null;
let seq = 0;
const pending = new Map();

function getWorker() {
  if (worker) return worker;
  worker = new Worker(`${import.meta.env.BASE_URL}engine/worker.js`);
  worker.onmessage = ({ data }) => {
    const p = pending.get(data.id);
    if (!p) return;
    pending.delete(data.id);
    if (data.failed) p.reject(new Error(data.failed));
    else p.resolve(JSON.parse(data.out));
  };
  worker.onerror = (e) => {
    e.preventDefault();
    for (const p of pending.values()) p.reject(new Error('worker'));
    pending.clear();
    worker.terminate();
    worker = null;
  };
  return worker;
}

function browserRequest(path, body) {
  return new Promise((resolve, reject) => {
    const id = ++seq;
    pending.set(id, { resolve, reject });
    getWorker().postMessage({ id, path, body: body ? JSON.stringify(body) : '' });
  });
}

// ---------------------------------------------------------------- servidor
async function serverRequest(path, body) {
  const res = await fetch(API_URL + path, {
    method: body ? 'POST' : 'GET',
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const type = res.headers.get('content-type') ?? '';
  if (!type.includes('application/json')) throw new Error('sem servidor');
  return res.json();
}

async function request(path, body) {
  if (!FORCE_BROWSER) {
    try {
      return await serverRequest(path, body);
    } catch {
      /* servidor fora do ar: tenta o motor do navegador */
    }
  }
  try {
    return await browserRequest(path, body);
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
