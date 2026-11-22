// localStorage com proteção (modo anônimo, armazenamento bloqueado etc.).
const PREFIX = 'stonkscalc:';

export function loadJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw == null ? fallback : JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function saveJSON(key, value) {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    /* sem armazenamento, sem problema */
  }
}
