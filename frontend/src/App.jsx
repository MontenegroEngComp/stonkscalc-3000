import { useCallback, useEffect, useState } from 'react';
import Calculator from './components/Calculator.jsx';
import Graph from './components/Graph.jsx';
import CalculusPanel from './components/CalculusPanel.jsx';
import Manual from './components/Manual.jsx';
import FloatingEmojis from './components/FloatingEmojis.jsx';
import { loadJSON, saveJSON } from './storage.js';
import { COLORS, makeFunction, nextColor } from './plotFunctions.js';

const TABS = [
  { id: 'calc', label: 'Calculadora', emoji: '🧮' },
  { id: 'graph', label: 'Gráficos', emoji: '📈' },
  { id: 'calculus', label: 'Cálculo', emoji: '∫' },
  { id: 'manual', label: 'Manual', emoji: '📜' },
];

export default function App() {
  const [tab, setTab] = useState(() => TABS.find((t) => `#${t.id}` === window.location.hash)?.id ?? 'calc');
  const [theme, setTheme] = useState(() => loadJSON('theme', 'light'));
  const [angleMode, setAngleMode] = useState(() => loadJSON('angleMode', 'RAD'));
  const [expr, setExpr] = useState('');
  const [functions, setFunctions] = useState(() => [makeFunction('sin(x)', COLORS[0]), makeFunction('x^2/4 - 2', COLORS[1])]);
  const [shade, setShade] = useState(null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    saveJSON('theme', theme);
  }, [theme]);
  useEffect(() => saveJSON('angleMode', angleMode), [angleMode]);
  useEffect(() => window.history.replaceState(null, '', `#${tab}`), [tab]);

  /** Envia uma expressão para o gráfico e muda de aba. */
  const plot = useCallback((expression, opts = {}) => {
    setFunctions((fs) => {
      if (fs.some((f) => f.expr === expression)) return fs;
      return [...fs, makeFunction(expression, nextColor(fs))];
    });
    setShade(opts.shade ?? null);
    setTab('graph');
  }, []);

  const insertIntoCalculator = useCallback((text) => {
    setExpr((e) => e + text);
    setTab('calc');
  }, []);

  return (
    <div className="app">
      <FloatingEmojis />
      <header className="topbar">
        <div className="brand">
          <span className="brand-emoji" aria-hidden="true">📈</span>
          <div>
            <h1>STONKSCALC 3000</h1>
            <p className="tagline">a calculadora científica que acredita em você</p>
          </div>
        </div>
        <div className="topbar-actions">
          <button
            className="chip"
            onClick={() => setAngleMode((m) => (m === 'RAD' ? 'DEG' : 'RAD'))}
            title="Alterna entre radianos e graus"
          >
            {angleMode === 'RAD' ? '📐 RAD' : '🎯 DEG'}
          </button>
          <button
            className="chip"
            onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}
            title="Alterna o tema"
          >
            {theme === 'dark' ? '☀️ Modo Dia' : '🦇 Modo Morcego'}
          </button>
        </div>
      </header>

      <nav className="tabs" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            aria-selected={tab === t.id}
            className={`tab ${tab === t.id ? 'active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            <span aria-hidden="true">{t.emoji}</span> {t.label}
          </button>
        ))}
      </nav>

      <main>
        {tab === 'calc' && (
          <Calculator expr={expr} setExpr={setExpr} angleMode={angleMode} setAngleMode={setAngleMode} onPlot={plot} />
        )}
        {tab === 'graph' && (
          <Graph
            functions={functions}
            setFunctions={setFunctions}
            angleMode={angleMode}
            shade={shade}
            setShade={setShade}
            theme={theme}
          />
        )}
        {tab === 'calculus' && <CalculusPanel angleMode={angleMode} onPlot={plot} />}
        {tab === 'manual' && <Manual onInsert={insertIntoCalculator} />}
      </main>

      <footer className="footer">
        Motor matemático em ☕ Java · Interface em ⚛️ React · Memes por conta da casa
      </footer>
    </div>
  );
}
