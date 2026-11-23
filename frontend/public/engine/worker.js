// Web Worker que roda o motor Java compilado para JavaScript (stonkscalc.js, gerado pelo TeaVM).
// Fica em segundo plano para que cálculos pesados (gráficos, integrais) não travem a interface.
let loaded = false;

self.onmessage = (event) => {
  const { id, path, body } = event.data;
  try {
    if (!loaded) {
      importScripts('stonkscalc.js');
      self.main([]);
      loaded = true;
    }
    self.postMessage({ id, out: self.stonkscalcHandle(path, body) });
  } catch (err) {
    self.postMessage({ id, failed: String(err) });
  }
};
