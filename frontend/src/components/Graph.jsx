import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api.js';
import { graphMeme } from '../memes.js';
import { makeFunction, nextColor } from '../plotFunctions.js';

const MAX_FUNCTIONS = 8;
const DEFAULT_SPAN = 20;

function niceStep(raw) {
  const p = Math.pow(10, Math.floor(Math.log10(raw)));
  const m = raw / p;
  return (m < 1.5 ? 1 : m < 3.5 ? 2 : m < 7.5 ? 5 : 10) * p;
}

function fmtTick(v, step) {
  if (Math.abs(v) < step / 1e6) return '0';
  const digits = Math.max(0, -Math.floor(Math.log10(step)));
  const s = v.toFixed(Math.min(digits, 10));
  return Math.abs(v) >= 1e5 || (Math.abs(v) < 1e-4 && v !== 0) ? v.toExponential(1) : s;
}

const fmtVal = (v) => {
  if (v == null || !Number.isFinite(v)) return '—';
  if (Math.abs(v) >= 1e6 || (Math.abs(v) < 1e-4 && v !== 0)) return v.toExponential(4);
  return String(Math.round(v * 1e6) / 1e6);
};

export default function Graph({ functions, setFunctions, angleMode, shade, setShade, theme }) {
  const wrapRef = useRef(null);
  const canvasRef = useRef(null);
  const [size, setSize] = useState({ w: 0, h: 0 });
  const [view, setView] = useState(null);
  const [data, setData] = useState(null);
  const [hover, setHover] = useState(null);
  const [showRoots, setShowRoots] = useState(true);
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState(null);
  const drag = useRef(null);
  const reqId = useRef(0);

  // ---------------------------------------------------------------- tamanho do canvas
  useEffect(() => {
    const el = wrapRef.current;
    const ro = new ResizeObserver(([entry]) => {
      const w = Math.floor(entry.contentRect.width);
      const h = Math.floor(entry.contentRect.height);
      setSize({ w, h });
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const resetView = useCallback(() => {
    if (!size.w) return;
    const halfY = (DEFAULT_SPAN / 2) * (size.h / size.w);
    setView({ xMin: -DEFAULT_SPAN / 2, xMax: DEFAULT_SPAN / 2, yMin: -halfY, yMax: halfY });
  }, [size.w, size.h]);

  useEffect(() => {
    if (size.w && !view) resetView();
  }, [size.w, view, resetView]);

  // ---------------------------------------------------------------- busca dos pontos no backend Java
  const visible = useMemo(() => functions.filter((f) => f.visible && f.expr.trim()), [functions]);
  const visibleKey = visible.map((f) => f.id + ':' + f.expr).join('|');

  useEffect(() => {
    if (!view || !size.w) return;
    const span = view.xMax - view.xMin;
    const xMin = view.xMin - span / 2;
    const xMax = view.xMax + span / 2;
    const samples = Math.min(3000, Math.max(300, size.w * 2));
    const id = ++reqId.current;
    const t = setTimeout(async () => {
      if (visible.length === 0) {
        setData({ xMin, xMax, samples, series: [] });
        return;
      }
      const r = await api.plot(visible.map((f) => f.expr), { xMin, xMax, samples, angleMode, roots: showRoots });
      if (id !== reqId.current) return; // resposta velha, ignora
      if (!r.ok) {
        setNotice(r.error);
        return;
      }
      setNotice(null);
      const series = r.series.map((s, i) => ({ ...s, fn: visible[i] }));
      const errs = {};
      series.forEach((s) => s.error && (errs[s.fn.id] = s.error));
      setErrors(errs);
      setData({ xMin: r.xMin, xMax: r.xMax, samples: r.samples, series });
    }, 120);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visibleKey, view, size.w, angleMode, showRoots]);

  // ---------------------------------------------------------------- desenho
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !view || !size.w) return;
    const dpr = window.devicePixelRatio || 1;
    const { w, h } = size;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    const css = getComputedStyle(canvas);
    const col = (name) => css.getPropertyValue(name).trim();
    const X = (x) => ((x - view.xMin) / (view.xMax - view.xMin)) * w;
    const Y = (y) => h - ((y - view.yMin) / (view.yMax - view.yMin)) * h;

    ctx.fillStyle = col('--graph-bg');
    ctx.fillRect(0, 0, w, h);

    // grade
    const stepX = niceStep((view.xMax - view.xMin) / Math.max(4, w / 90));
    const stepY = niceStep((view.yMax - view.yMin) / Math.max(3, h / 70));
    ctx.lineWidth = 1;
    ctx.strokeStyle = col('--graph-grid');
    ctx.beginPath();
    for (let x = Math.ceil(view.xMin / stepX) * stepX; x <= view.xMax; x += stepX) {
      ctx.moveTo(Math.round(X(x)) + 0.5, 0);
      ctx.lineTo(Math.round(X(x)) + 0.5, h);
    }
    for (let y = Math.ceil(view.yMin / stepY) * stepY; y <= view.yMax; y += stepY) {
      ctx.moveTo(0, Math.round(Y(y)) + 0.5);
      ctx.lineTo(w, Math.round(Y(y)) + 0.5);
    }
    ctx.stroke();

    // eixos
    const ax = Math.min(Math.max(X(0), 0), w);
    const ay = Math.min(Math.max(Y(0), 0), h);
    ctx.strokeStyle = col('--graph-axis');
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(0, ay);
    ctx.lineTo(w, ay);
    ctx.moveTo(ax, 0);
    ctx.lineTo(ax, h);
    ctx.stroke();

    // números dos eixos
    ctx.fillStyle = col('--graph-label');
    ctx.font = '11px "JetBrains Mono", monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    const labelY = ay > h - 18 ? ay - 16 : ay + 4;
    for (let x = Math.ceil(view.xMin / stepX) * stepX; x <= view.xMax; x += stepX) {
      if (Math.abs(x) < stepX / 2) continue;
      ctx.fillText(fmtTick(x, stepX), X(x), labelY);
    }
    ctx.textAlign = ax > w - 40 ? 'right' : 'left';
    ctx.textBaseline = 'middle';
    const labelX = ax > w - 40 ? ax - 5 : ax + 5;
    for (let y = Math.ceil(view.yMin / stepY) * stepY; y <= view.yMax; y += stepY) {
      if (Math.abs(y) < stepY / 2) continue;
      ctx.fillText(fmtTick(y, stepY), labelX, Y(y));
    }

    if (!data) return;
    const dx = (data.xMax - data.xMin) / (data.samples - 1);
    const xAt = (i) => data.xMin + i * dx;
    const clampPx = (v) => Math.max(-1e5, Math.min(1e5, v));

    // área da integral
    if (shade) {
      const s = data.series.find((x) => x.expression === shade.expr && x.ys);
      if (s) {
        ctx.fillStyle = s.fn.color + '44';
        ctx.beginPath();
        let open = false;
        let lastX = 0;
        for (let i = 0; i < s.ys.length; i++) {
          const x = xAt(i);
          const y = s.ys[i];
          if (x < shade.a || x > shade.b || y == null) continue;
          if (!open) {
            ctx.moveTo(X(x), Y(0));
            open = true;
          }
          ctx.lineTo(X(x), clampPx(Y(y)));
          lastX = x;
        }
        if (open) {
          ctx.lineTo(X(lastX), Y(0));
          ctx.closePath();
          ctx.fill();
        }
      }
    }

    // curvas
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    for (const s of data.series) {
      if (!s.ys) continue;
      ctx.strokeStyle = s.fn.color;
      ctx.lineWidth = 3;
      ctx.beginPath();
      let prev = null;
      for (let i = 0; i < s.ys.length; i++) {
        const y = s.ys[i];
        if (y == null) {
          prev = null;
          continue;
        }
        const px = X(xAt(i));
        const py = clampPx(Y(y));
        if (prev == null || Math.abs(py - prev) > h * 2) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
        prev = py;
      }
      ctx.stroke();

      if (showRoots && s.roots) {
        for (const r of s.roots) {
          const px = X(r);
          if (px < -10 || px > w + 10) continue;
          ctx.fillStyle = col('--graph-bg');
          ctx.strokeStyle = s.fn.color;
          ctx.lineWidth = 2.5;
          ctx.beginPath();
          ctx.arc(px, Y(0), 5, 0, Math.PI * 2);
          ctx.fill();
          ctx.stroke();
        }
      }
    }

    // cursor
    if (hover) {
      ctx.strokeStyle = col('--graph-axis');
      ctx.setLineDash([4, 4]);
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(hover.px, 0);
      ctx.lineTo(hover.px, h);
      ctx.stroke();
      ctx.setLineDash([]);
      const i = Math.round((hover.x - data.xMin) / dx);
      for (const s of data.series) {
        const y = s.ys?.[i];
        if (y == null) continue;
        ctx.fillStyle = s.fn.color;
        ctx.beginPath();
        ctx.arc(hover.px, clampPx(Y(y)), 5, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }, [data, view, size, hover, shade, showRoots, theme]);

  // ---------------------------------------------------------------- interação: arrastar e zoom
  const pxToX = (px) => view.xMin + (px / size.w) * (view.xMax - view.xMin);
  const pxToY = (py) => view.yMax - (py / size.h) * (view.yMax - view.yMin);

  const zoom = useCallback(
    (factor, px = size.w / 2, py = size.h / 2) => {
      setView((v) => {
        if (!v) return v;
        const cx = v.xMin + (px / size.w) * (v.xMax - v.xMin);
        const cy = v.yMax - (py / size.h) * (v.yMax - v.yMin);
        const span = (v.xMax - v.xMin) * factor;
        if (span < 1e-9 || span > 1e9) return v;
        return {
          xMin: cx - (cx - v.xMin) * factor,
          xMax: cx + (v.xMax - cx) * factor,
          yMin: cy - (cy - v.yMin) * factor,
          yMax: cy + (v.yMax - cy) * factor,
        };
      });
    },
    [size.w, size.h],
  );

  useEffect(() => {
    const canvas = canvasRef.current;
    const onWheel = (e) => {
      e.preventDefault();
      const rect = canvas.getBoundingClientRect();
      zoom(Math.exp(e.deltaY * 0.0015), e.clientX - rect.left, e.clientY - rect.top);
    };
    canvas.addEventListener('wheel', onWheel, { passive: false });
    return () => canvas.removeEventListener('wheel', onWheel);
  }, [zoom]);

  const onPointerDown = (e) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    drag.current = { x: e.clientX, y: e.clientY, view };
  };
  const onPointerMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const px = e.clientX - rect.left;
    const py = e.clientY - rect.top;
    if (drag.current) {
      const d = drag.current;
      const sx = ((e.clientX - d.x) / size.w) * (d.view.xMax - d.view.xMin);
      const sy = ((e.clientY - d.y) / size.h) * (d.view.yMax - d.view.yMin);
      setView({ xMin: d.view.xMin - sx, xMax: d.view.xMax - sx, yMin: d.view.yMin + sy, yMax: d.view.yMax + sy });
      setHover(null);
    } else if (view) {
      setHover({ px, py, x: pxToX(px), y: pxToY(py) });
    }
  };
  const onPointerUp = () => {
    drag.current = null;
  };

  /** Ajusta o eixo y para caber as curvas visíveis (ignorando picos de assíntotas). */
  const fitY = () => {
    if (!data || !view) return;
    const dx = (data.xMax - data.xMin) / (data.samples - 1);
    const vals = [];
    for (const s of data.series) {
      s.ys?.forEach((y, i) => {
        const x = data.xMin + i * dx;
        if (y != null && x >= view.xMin && x <= view.xMax) vals.push(y);
      });
    }
    if (vals.length < 2) return;
    vals.sort((a, b) => a - b);
    let lo = vals[Math.floor(vals.length * 0.02)];
    let hi = vals[Math.ceil(vals.length * 0.98) - 1];
    if (hi - lo < 1e-9) {
      lo -= 1;
      hi += 1;
    }
    const pad = (hi - lo) * 0.1;
    setView({ ...view, yMin: lo - pad, yMax: hi + pad });
  };

  // ---------------------------------------------------------------- lista de funções
  const update = (id, patch) => setFunctions((fs) => fs.map((f) => (f.id === id ? { ...f, ...patch } : f)));
  const remove = (id) => setFunctions((fs) => fs.filter((f) => f.id !== id));
  const add = (expr = '') =>
    setFunctions((fs) => (fs.length >= MAX_FUNCTIONS ? fs : [...fs, makeFunction(expr, nextColor(fs))]));

  const addDerivative = async (f) => {
    const r = await api.derivative(f.expr);
    if (!r.ok) {
      setNotice(r.error);
      return;
    }
    if (angleMode === 'DEG') setNotice('Aviso: a derivada simbólica considera radianos.');
    add(r.derivative);
  };

  const hoverValues =
    hover && data
      ? data.series.map((s) => {
          const dx = (data.xMax - data.xMin) / (data.samples - 1);
          const i = Math.round((hover.x - data.xMin) / dx);
          return { fn: s.fn, y: s.ys?.[i] ?? null };
        })
      : [];

  const meme = data ? graphMeme(data.series) : null;
  const allRoots = data?.series.flatMap((s) =>
    (s.roots ?? []).filter((r) => view && r >= view.xMin && r <= view.xMax).map((r) => ({ r, fn: s.fn })),
  );

  return (
    <section className="graph-layout">
      <div className="card graph-side">
        <h2>📈 Stonks Plotter</h2>
        <p className="muted">Escreva funções de <code>x</code>. Arraste para mover, use a rodinha para zoom.</p>
        <ul className="fn-list">
          {functions.map((f, idx) => (
            <li key={f.id} className={errors[f.id] ? 'has-error' : ''}>
              <div className="fn-row">
                <button
                  className={`swatch ${f.visible ? '' : 'off'}`}
                  style={{ '--c': f.color }}
                  onClick={() => update(f.id, { visible: !f.visible })}
                  title={f.visible ? 'Esconder' : 'Mostrar'}
                  aria-label={f.visible ? 'Esconder função' : 'Mostrar função'}
                />
                <label className="fn-label" htmlFor={`fn-${f.id}`}>
                  f<sub>{idx + 1}</sub>(x) =
                </label>
                <input
                  id={`fn-${f.id}`}
                  value={f.expr}
                  onChange={(e) => update(f.id, { expr: e.target.value })}
                  placeholder="ex: x^3 - 2x"
                  spellCheck={false}
                />
                <button className="icon-btn" onClick={() => addDerivative(f)} disabled={!f.expr.trim()} title="Adicionar a derivada f'(x)">
                  f′
                </button>
                <button className="icon-btn" onClick={() => remove(f.id)} title="Remover" aria-label="Remover função">
                  ✕
                </button>
              </div>
              {errors[f.id] && <p className="fn-error">{errors[f.id]}</p>}
            </li>
          ))}
        </ul>
        <button className="btn" onClick={() => add()} disabled={functions.length >= MAX_FUNCTIONS}>
          + Nova função
        </button>

        <div className="presets">
          <span className="muted">Clássicos:</span>
          {['tan(x)', '1/x', 'e^(-x^2)', 'sin(x)/x', 'abs(x)', 'floor(x)', 'sqrt(x)', 'ln(x)'].map((p) => (
            <button key={p} className="chip small" onClick={() => add(p)}>
              {p}
            </button>
          ))}
        </div>

        <label className="check">
          <input type="checkbox" checked={showRoots} onChange={(e) => setShowRoots(e.target.checked)} /> Mostrar raízes (zeros)
        </label>
        {shade && (
          <div className="shade-info">
            Área de <code>{shade.expr}</code> de {shade.a} até {shade.b}
            {shade.value && <> = <strong>{shade.value}</strong></>}
            <button className="link" onClick={() => setShade(null)}>remover</button>
          </div>
        )}
        {allRoots && allRoots.length > 0 && showRoots && (
          <div className="roots">
            <strong>Raízes visíveis</strong>
            <ul>
              {allRoots.slice(0, 12).map(({ r, fn }, i) => (
                <li key={i}>
                  <span className="dot" style={{ background: fn.color }} /> x ≈ {fmtVal(r)}
                </li>
              ))}
            </ul>
          </div>
        )}
        {notice && <p className="notice">⚠️ {notice}</p>}
      </div>

      <div className="card graph-card">
        <div className="graph-toolbar">
          <button className="key small" onClick={() => zoom(0.7)} title="Aproximar">＋</button>
          <button className="key small" onClick={() => zoom(1 / 0.7)} title="Afastar">－</button>
          <button className="key small" onClick={resetView} title="Voltar ao início">⟲</button>
          <button className="key small" onClick={fitY} title="Ajustar o eixo y às curvas">↕ Enquadrar</button>
          {meme && (
            <span className="graph-meme">
              <span aria-hidden="true">{meme.emoji}</span> {meme.text}
            </span>
          )}
        </div>
        <div className="canvas-wrap" ref={wrapRef}>
          <canvas
            ref={canvasRef}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            onPointerLeave={() => setHover(null)}
          />
          {hover && (
            <div className="tooltip" style={{ left: Math.min(hover.px + 14, size.w - 170), top: Math.max(hover.py - 10, 4) }}>
              <div>x = {fmtVal(hover.x)}</div>
              {hoverValues.map(({ fn, y }) => (
                <div key={fn.id}>
                  <span className="dot" style={{ background: fn.color }} /> {fmtVal(y)}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
