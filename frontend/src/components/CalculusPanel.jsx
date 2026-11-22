import { useState } from 'react';
import { api } from '../api.js';
import { pickMeme } from '../memes.js';
import MemeCard from './MemeCard.jsx';

/** Moldura comum dos cartões de cálculo: formulário + resultado + meme. */
function Tool({ emoji, title, subtitle, onRun, busy, result, children, actions }) {
  return (
    <form
      className="card tool"
      onSubmit={(e) => {
        e.preventDefault();
        onRun();
      }}
    >
      <header>
        <h3>
          <span aria-hidden="true">{emoji}</span> {title}
        </h3>
        <p className="muted">{subtitle}</p>
      </header>
      <div className="tool-fields">{children}</div>
      <div className="tool-actions">
        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Calculando...' : 'Calcular'}
        </button>
        {actions}
      </div>
      {result && (
        <div className={`tool-result ${result.error ? 'err' : ''}`}>
          <MemeCard meme={result.meme} compact />
          <div className="tool-output">{result.error ? <p>{result.error}</p> : result.body}</div>
        </div>
      )}
    </form>
  );
}

function Field({ label, value, onChange, placeholder, width }) {
  return (
    <label className="field" style={width ? { flex: `0 0 ${width}` } : undefined}>
      <span>{label}</span>
      <input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} spellCheck={false} />
    </label>
  );
}

function useTool() {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const run = async (fn) => {
    setBusy(true);
    try {
      setResult(await fn());
    } finally {
      setBusy(false);
    }
  };
  const fail = (r) => ({ error: r.error, meme: pickMeme({ error: r.error, offline: r.offline }) });
  return { busy, result, run, fail };
}

const wrap = (s) => `(${s})`;

// ------------------------------------------------------------------ Derivada
function DerivativeTool({ onPlot }) {
  const [f, setF] = useState('x^3 * sin(x)');
  const [order, setOrder] = useState('1');
  const [at, setAt] = useState('');
  const t = useTool();
  const primes = (n) => "′".repeat(Math.min(n, 3)) + (n > 3 ? `⁽${n}⁾` : '');

  const run = () =>
    t.run(async () => {
      const n = parseInt(order, 10) || 1;
      const r = await api.derivative(f, { order: n, at: at.trim() || undefined });
      if (!r.ok) return t.fail(r);
      return {
        derivative: r.derivative,
        meme: r.valueAt != null ? pickMeme({ value: r.valueAt, expression: '' }) : { emoji: '🧠', top: 'DERIVOU', bottom: 'Newton e Leibniz brigando pelo crédito', tone: 'mint' },
        body: (
          <>
            {r.steps.map((s, i) => (
              <p key={i} className="formula">
                f{primes(i + 1)}(x) = <code>{s}</code>
              </p>
            ))}
            {r.valueAtDisplay != null && (
              <p className="formula">
                f{primes(n)}({at}) = <strong>{r.valueAtDisplay}</strong>
              </p>
            )}
          </>
        ),
      };
    });

  return (
    <Tool
      emoji="🧠"
      title="Derivada simbólica"
      subtitle="Regra da cadeia, do produto, do quociente… tudo no automático."
      onRun={run}
      busy={t.busy}
      result={t.result}
      actions={
        t.result?.derivative && (
          <button type="button" className="btn" onClick={() => { onPlot(f); onPlot(t.result.derivative); }}>
            📈 Plotar f e f′
          </button>
        )
      }
    >
      <Field label="f(x)" value={f} onChange={setF} placeholder="x^2 * ln(x)" />
      <Field label="ordem" value={order} onChange={setOrder} width="80px" />
      <Field label="no ponto (opcional)" value={at} onChange={setAt} placeholder="ex: pi/2" width="150px" />
    </Tool>
  );
}

// ------------------------------------------------------------------ Integral
function IntegralTool({ angleMode, onPlot }) {
  const [f, setF] = useState('e^(-x^2)');
  const [a, setA] = useState('-inf');
  const [b, setB] = useState('inf');
  const t = useTool();

  const bound = async (s) => {
    const r = await api.evaluate(s, angleMode);
    if (!r.ok) return NaN;
    return r.infinite ? (r.display.startsWith('-') ? -Infinity : Infinity) : r.value;
  };

  const run = () =>
    t.run(async () => {
      const r = await api.evaluate(`integral(${wrap(f)}, x, ${wrap(a)}, ${wrap(b)})`, angleMode);
      if (!r.ok) return t.fail(r);
      return {
        value: r.display,
        meme: pickMeme({ value: r.value, infinite: r.infinite, expression: '' }),
        body: (
          <p className="formula big">
            ∫<sub>{a}</sub><sup>{b}</sup> {f} dx = <strong>{r.display}</strong>
          </p>
        ),
      };
    });

  const showArea = async () => {
    const [na, nb] = await Promise.all([bound(a), bound(b)]);
    onPlot(f, { shade: { expr: f, a: Math.min(na, nb), b: Math.max(na, nb), value: t.result?.value } });
  };

  return (
    <Tool
      emoji="∫"
      title="Integral definida"
      subtitle="Gauss–Kronrod adaptativo. Aceita limites infinitos (inf, -inf)."
      onRun={run}
      busy={t.busy}
      result={t.result}
      actions={t.result?.value && <button type="button" className="btn" onClick={showArea}>📈 Ver a área</button>}
    >
      <Field label="f(x)" value={f} onChange={setF} />
      <Field label="de a =" value={a} onChange={setA} width="110px" />
      <Field label="até b =" value={b} onChange={setB} width="110px" />
    </Tool>
  );
}

// ------------------------------------------------------------------ Limite
function LimitTool({ angleMode, onPlot }) {
  const [f, setF] = useState('sin(x)/x');
  const [a, setA] = useState('0');
  const [side, setSide] = useState('0');
  const t = useTool();
  const arrow = side === '1' ? '⁺' : side === '-1' ? '⁻' : '';

  const run = () =>
    t.run(async () => {
      const sideArg = side === '0' ? '' : `, ${side}`;
      const r = await api.evaluate(`limit(${wrap(f)}, x, ${wrap(a)}${sideArg})`, angleMode);
      if (!r.ok) return t.fail(r);
      return {
        meme: pickMeme({ value: r.value, infinite: r.infinite, expression: '' }),
        body: (
          <p className="formula big">
            lim<sub>x→{a}{arrow}</sub> {f} = <strong>{r.display}</strong>
          </p>
        ),
      };
    });

  return (
    <Tool
      emoji="🎯"
      title="Limite"
      subtitle="Extrapolação de Richardson pelos dois lados. Use inf para x → ∞."
      onRun={run}
      busy={t.busy}
      result={t.result}
      actions={<button type="button" className="btn" onClick={() => onPlot(f)}>📈 Plotar f</button>}
    >
      <Field label="f(x)" value={f} onChange={setF} />
      <Field label="x →" value={a} onChange={setA} width="100px" />
      <label className="field" style={{ flex: '0 0 140px' }}>
        <span>lado</span>
        <select value={side} onChange={(e) => setSide(e.target.value)}>
          <option value="0">os dois</option>
          <option value="-1">esquerda (⁻)</option>
          <option value="1">direita (⁺)</option>
        </select>
      </label>
    </Tool>
  );
}

// ------------------------------------------------------------------ Somatório / produtório
function SeriesTool({ angleMode }) {
  const [kind, setKind] = useState('sum');
  const [f, setF] = useState('1/k^2');
  const [from, setFrom] = useState('1');
  const [to, setTo] = useState('1000');
  const t = useTool();

  const run = () =>
    t.run(async () => {
      const r = await api.evaluate(`${kind}(${wrap(f)}, k, ${wrap(from)}, ${wrap(to)})`, angleMode);
      if (!r.ok) return t.fail(r);
      return {
        meme: pickMeme({ value: r.value, infinite: r.infinite, expression: '' }),
        body: (
          <p className="formula big">
            {kind === 'sum' ? 'Σ' : 'Π'}
            <sub>k={from}</sub>
            <sup>{to}</sup> {f} = <strong>{r.display}</strong>
          </p>
        ),
      };
    });

  return (
    <Tool emoji="Σ" title="Somatório / Produtório" subtitle="Use k como índice. Até 10 milhões de termos." onRun={run} busy={t.busy} result={t.result}>
      <label className="field" style={{ flex: '0 0 120px' }}>
        <span>tipo</span>
        <select value={kind} onChange={(e) => setKind(e.target.value)}>
          <option value="sum">Σ soma</option>
          <option value="prod">Π produto</option>
        </select>
      </label>
      <Field label="f(k)" value={f} onChange={setF} />
      <Field label="k de" value={from} onChange={setFrom} width="80px" />
      <Field label="até" value={to} onChange={setTo} width="100px" />
    </Tool>
  );
}

// ------------------------------------------------------------------ Equações
function EquationTool({ angleMode, onPlot }) {
  const [left, setLeft] = useState('x^3');
  const [right, setRight] = useState('2x + 1');
  const [a, setA] = useState('-10');
  const [b, setB] = useState('10');
  const t = useTool();
  const expr = right.trim() ? `${wrap(left)} - ${wrap(right)}` : left;

  const run = () =>
    t.run(async () => {
      const r = await api.roots(expr, a, b, angleMode);
      if (!r.ok) return t.fail(r);
      if (r.roots.length === 0) {
        return {
          meme: { emoji: '🕳️', top: 'NENHUMA SOLUÇÃO', bottom: 'nesse intervalo, pelo menos', tone: 'pink' },
          body: <p>Não achei soluções reais entre {a} e {b}.</p>,
        };
      }
      return {
        meme: r.roots.length > 3
          ? { emoji: '🤹', top: `${r.roots.length} SOLUÇÕES`, bottom: 'tem raiz pra todo lado', tone: 'sun' }
          : { emoji: '🎯', top: 'ACHEI!', bottom: r.roots.length === 1 ? 'uma solução' : `${r.roots.length} soluções`, tone: 'mint' },
        body: (
          <ul className="solutions">
            {r.display.map((d, i) => (
              <li key={i}>
                x<sub>{i + 1}</sub> = <strong>{d}</strong>
              </li>
            ))}
          </ul>
        ),
      };
    });

  return (
    <Tool
      emoji="⚖️"
      title="Resolver equação"
      subtitle="Encontra todas as soluções reais de f(x) = g(x) no intervalo."
      onRun={run}
      busy={t.busy}
      result={t.result}
      actions={<button type="button" className="btn" onClick={() => onPlot(expr)}>📈 Plotar f − g</button>}
    >
      <Field label="f(x)" value={left} onChange={setLeft} />
      <span className="equals-sign">=</span>
      <Field label="g(x)" value={right} onChange={setRight} placeholder="0" />
      <Field label="de" value={a} onChange={setA} width="80px" />
      <Field label="até" value={b} onChange={setB} width="80px" />
    </Tool>
  );
}

export default function CalculusPanel({ angleMode, onPlot }) {
  return (
    <section className="calculus-grid">
      <DerivativeTool onPlot={onPlot} />
      <IntegralTool angleMode={angleMode} onPlot={onPlot} />
      <LimitTool angleMode={angleMode} onPlot={onPlot} />
      <SeriesTool angleMode={angleMode} />
      <EquationTool angleMode={angleMode} onPlot={onPlot} />
    </section>
  );
}
