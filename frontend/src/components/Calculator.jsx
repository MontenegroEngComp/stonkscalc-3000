import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import { IDLE_MEME, LOADING_MEME, pickMeme } from '../memes.js';
import { loadJSON, saveJSON } from '../storage.js';
import MemeCard from './MemeCard.jsx';

// l = rótulo, i = texto inserido, l2/i2 = versão com "2nd" ativo, a = ação especial
const SCI_KEYS = [
  { a: 'second', l: '2nd', cls: 'mod' },
  { i: '(', l: '(' },
  { i: ')', l: ')' },
  { i: ', ', l: ',' },
  { i: '%', l: '%', t: 'Porcentagem (50% = 0.5)' },

  { i: 'sin(', l: 'sin', i2: 'asin(', l2: 'sin⁻¹' },
  { i: 'cos(', l: 'cos', i2: 'acos(', l2: 'cos⁻¹' },
  { i: 'tan(', l: 'tan', i2: 'atan(', l2: 'tan⁻¹' },
  { i: 'ln(', l: 'ln', i2: 'e^(', l2: 'eˣ' },
  { i: 'log(', l: 'log', i2: '10^(', l2: '10ˣ' },

  { i: 'sinh(', l: 'sinh', i2: 'asinh(', l2: 'sinh⁻¹' },
  { i: 'cosh(', l: 'cosh', i2: 'acosh(', l2: 'cosh⁻¹' },
  { i: 'tanh(', l: 'tanh', i2: 'atanh(', l2: 'tanh⁻¹' },
  { i: 'sqrt(', l: '√x', i2: 'cbrt(', l2: '∛x' },
  { i: '^2', l: 'x²', i2: '^3', l2: 'x³' },

  { i: '^', l: 'xʸ', i2: 'root(', l2: 'ʸ√x' },
  { i: '^(-1)', l: 'x⁻¹', i2: 'abs(', l2: '|x|' },
  { i: '!', l: 'n!', i2: 'gamma(', l2: 'Γ(x)' },
  { i: 'pi', l: 'π', i2: 'e', l2: 'e' },
  { i: 'E', l: 'EE', t: 'Notação científica: 1.5E3 = 1500' },

  { i: 'nCr(', l: 'nCr', i2: 'nPr(', l2: 'nPr' },
  { i: 'mod(', l: 'mod', i2: 'gcd(', l2: 'mdc' },
  { i: 'rand()', l: 'rand', i2: 'randint(', l2: 'randint' },
  { i: 'floor(', l: '⌊x⌋', i2: 'ceil(', l2: '⌈x⌉' },
  { i: 'x', l: 'x', cls: 'var', t: 'Variável x: use para plotar funções' },
];

const NUM_KEYS = [
  { i: '7', l: '7', cls: 'num' },
  { i: '8', l: '8', cls: 'num' },
  { i: '9', l: '9', cls: 'num' },
  { a: 'back', l: '⌫', cls: 'warn', t: 'Apagar' },
  { a: 'clear', l: 'AC', cls: 'danger', t: 'Limpar tudo (deu ruim)' },
  { i: '4', l: '4', cls: 'num' },
  { i: '5', l: '5', cls: 'num' },
  { i: '6', l: '6', cls: 'num' },
  { i: '×', l: '×', cls: 'op' },
  { i: '÷', l: '÷', cls: 'op' },
  { i: '1', l: '1', cls: 'num' },
  { i: '2', l: '2', cls: 'num' },
  { i: '3', l: '3', cls: 'num' },
  { i: '+', l: '+', cls: 'op' },
  { i: '−', l: '−', cls: 'op' },
  { i: '0', l: '0', cls: 'num' },
  { i: '.', l: '.', cls: 'num' },
  { i: 'ans', l: 'ANS', cls: 'mod', t: 'Resposta anterior' },
  { a: 'eval', l: '=', sub: 'RECEBA', cls: 'equals wide' },
];

const OPERATOR_START = /^[+\-−×÷*/^!%]/;
const HAS_X = /(^|[^a-zA-Z_])x(?![a-zA-Z_(])/;

export default function Calculator({ expr, setExpr, angleMode, setAngleMode, onPlot }) {
  const inputRef = useRef(null);
  const [second, setSecond] = useState(false);
  const [result, setResult] = useState(null); // { display, value, infinite } | { error }
  const [meme, setMeme] = useState(IDLE_MEME);
  const [busy, setBusy] = useState(false);
  const [ans, setAns] = useState(() => loadJSON('ans', null));
  const [memory, setMemory] = useState(() => loadJSON('memory', 0));
  const [history, setHistory] = useState(() => loadJSON('history', []));
  const justEvaluated = useRef(false);

  useEffect(() => saveJSON('history', history.slice(0, 50)), [history]);
  useEffect(() => saveJSON('memory', memory), [memory]);
  useEffect(() => saveJSON('ans', ans), [ans]);

  const focusAt = (pos) => {
    requestAnimationFrame(() => {
      const el = inputRef.current;
      if (!el) return;
      el.focus();
      el.setSelectionRange(pos, pos);
    });
  };

  const insert = (text) => {
    const el = inputRef.current;
    let base = expr;
    let start = el ? el.selectionStart ?? base.length : base.length;
    let end = el ? el.selectionEnd ?? base.length : base.length;
    if (justEvaluated.current) {
      // depois do "=", operador continua a partir do resultado; qualquer outra coisa começa do zero
      base = OPERATOR_START.test(text) ? 'ans' : '';
      start = end = base.length;
      justEvaluated.current = false;
    }
    const next = base.slice(0, start) + text + base.slice(end);
    setExpr(next);
    focusAt(start + text.length);
  };

  const backspace = () => {
    justEvaluated.current = false;
    const el = inputRef.current;
    const start = el?.selectionStart ?? expr.length;
    const end = el?.selectionEnd ?? expr.length;
    if (start !== end) {
      setExpr(expr.slice(0, start) + expr.slice(end));
      focusAt(start);
    } else if (start > 0) {
      setExpr(expr.slice(0, start - 1) + expr.slice(start));
      focusAt(start - 1);
    }
  };

  const clear = () => {
    justEvaluated.current = false;
    setExpr('');
    setResult(null);
    setMeme(IDLE_MEME);
    focusAt(0);
  };

  const evaluate = async () => {
    if (!expr.trim() || busy) return;
    setBusy(true);
    setMeme(LOADING_MEME);
    const variables = ans != null ? { ans } : {};
    const r = await api.evaluate(expr, angleMode, variables);
    setBusy(false);
    if (r.ok) {
      const value = r.infinite ? null : r.value;
      setResult({ display: r.display, value, infinite: r.infinite });
      setMeme(pickMeme({ value, infinite: r.infinite, previous: ans, expression: expr }));
      if (value != null) setAns(value);
      setHistory((h) => [{ expr, display: r.display, mode: angleMode, at: Date.now() }, ...h].slice(0, 50));
      justEvaluated.current = true;
    } else {
      setResult({ error: r.error });
      if (/variável desconhecida: 'x'/i.test(r.error)) {
        setMeme({ emoji: '📈', top: 'ISSO É UMA FUNÇÃO', bottom: 'clica em "Plotar", campeão', tone: 'mint' });
      } else {
        setMeme(pickMeme({ error: r.error, offline: r.offline }));
      }
    }
  };

  const onKey = (k) => {
    if (k.a === 'second') return setSecond((s) => !s);
    if (k.a === 'back') return backspace();
    if (k.a === 'clear') return clear();
    if (k.a === 'eval') return evaluate();
    const useSecond = second && k.i2;
    insert(useSecond ? k.i2 : k.i);
    if (useSecond) setSecond(false);
  };

  const onInputKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      evaluate();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      clear();
    } else if (justEvaluated.current && e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      insert(e.key);
    }
  };

  const memoryValue = result && result.value != null ? result.value : null;
  const canPlot = HAS_X.test(expr);

  return (
    <section className="calc-layout">
      <div className="calc-main card">
        <MemeCard meme={meme} />

        <div className={`display ${result?.error ? 'has-error' : ''}`}>
          <div className="display-meta">
            <span className="badge">{angleMode}</span>
            {second && <span className="badge hot">2nd</span>}
            {memory !== 0 && <span className="badge" title={`Memória: ${memory}`}>M</span>}
            {canPlot && <span className="badge mint">f(x)</span>}
          </div>
          <input
            ref={inputRef}
            className="display-input"
            value={expr}
            onChange={(e) => {
              justEvaluated.current = false;
              setExpr(e.target.value);
            }}
            onKeyDown={onInputKeyDown}
            placeholder="digite ou use o teclado... ex: 2sin(pi/4)^2"
            spellCheck={false}
            autoComplete="off"
            aria-label="Expressão"
          />
          <output className="display-result" aria-live="polite">
            {busy ? '...' : result?.error ? result.error : result ? `= ${result.display}` : ' '}
          </output>
        </div>

        <div className="memory-row">
          <button className="key small" onClick={() => setMemory(0)} title="Limpar memória">MC</button>
          <button className="key small" onClick={() => insert(String(memory))} title="Recuperar memória">MR</button>
          <button className="key small" disabled={memoryValue == null} onClick={() => setMemory((m) => m + memoryValue)} title="Somar resultado à memória">M+</button>
          <button className="key small" disabled={memoryValue == null} onClick={() => setMemory((m) => m - memoryValue)} title="Subtrair resultado da memória">M−</button>
          <button className="key small" onClick={() => setAngleMode(angleMode === 'RAD' ? 'DEG' : 'RAD')} title="Radianos / graus">
            {angleMode === 'RAD' ? 'RAD→DEG' : 'DEG→RAD'}
          </button>
          <button className="key small plot" disabled={!canPlot} onClick={() => onPlot(expr)} title="Mandar para o gráfico">
            📈 Plotar
          </button>
        </div>

        <div className="keypad">
          <div className="keys sci">
            {SCI_KEYS.map((k, idx) => {
              const label = second && k.l2 ? k.l2 : k.l;
              return (
                <button
                  key={idx}
                  className={`key ${k.cls ?? 'fn'} ${k.a === 'second' && second ? 'on' : ''} ${second && k.l2 ? 'alt' : ''}`}
                  onClick={() => onKey(k)}
                  title={k.t}
                >
                  {label}
                </button>
              );
            })}
          </div>
          <div className="keys num">
            {NUM_KEYS.map((k, idx) => (
              <button key={idx} className={`key ${k.cls}`} onClick={() => onKey(k)} title={k.t} disabled={k.a === 'eval' && busy}>
                {k.l}
                {k.sub && <small>{k.sub}</small>}
              </button>
            ))}
          </div>
        </div>
      </div>

      <aside className="history card">
        <div className="history-head">
          <h2>📜 Histórico</h2>
          {history.length > 0 && (
            <button className="link" onClick={() => setHistory([])}>
              limpar
            </button>
          )}
        </div>
        {history.length === 0 ? (
          <p className="empty">
            Nada aqui ainda. <br />
            <span aria-hidden="true">🦗</span> <em>*barulho de grilo*</em>
          </p>
        ) : (
          <ul>
            {history.map((h) => (
              <li key={h.at}>
                <button
                  className="history-item"
                  onClick={() => {
                    justEvaluated.current = false;
                    setExpr(h.expr);
                    focusAt(h.expr.length);
                  }}
                  title="Reusar esta expressão"
                >
                  <span className="h-expr">{h.expr}</span>
                  <span className="h-res">= {h.display}</span>
                  {h.mode === 'DEG' && <span className="h-mode">DEG</span>}
                </button>
              </li>
            ))}
          </ul>
        )}
        <div className="tips">
          <strong>Dicas rápidas</strong>
          <ul>
            <li><kbd>Enter</kbd> calcula, <kbd>Esc</kbd> limpa</li>
            <li>Multiplicação implícita: <code>2x</code>, <code>3(1+2)</code></li>
            <li><code>integral(x^2, x, 0, 3)</code> funciona aqui também</li>
          </ul>
        </div>
      </aside>
    </section>
  );
}
