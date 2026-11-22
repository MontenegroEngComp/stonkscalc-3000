import { useEffect, useState } from 'react';
import { api } from '../api.js';

const CONSTANTS = [
  ['pi  ou  π', 'π ≈ 3,14159'],
  ['e', 'Número de Euler ≈ 2,71828'],
  ['tau', 'τ = 2π'],
  ['phi', 'Proporção áurea φ ≈ 1,618'],
  ['inf', 'Infinito (para integrais e limites)'],
  ['ans', 'Resultado anterior'],
];

const SYNTAX = [
  ['2x, 3(1+2), 2pi', 'Multiplicação implícita'],
  ['2^3^2', 'Potência é associativa à direita (= 2^9)'],
  ['5!', 'Fatorial'],
  ['50%', 'Porcentagem (= 0.5)'],
  ['1.5E3', 'Notação científica (E maiúsculo)'],
  ['√2, sin x', 'Função de 1 argumento sem parênteses'],
  ['(1 + 2', 'Parênteses no final fecham sozinhos'],
];

/** Manual das funções, carregado do backend Java. Clique numa função para inseri-la na calculadora. */
export default function Manual({ onInsert }) {
  const [groups, setGroups] = useState(null);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    api.functions().then((r) => {
      if (!r.ok) return setError(r.error);
      const g = {};
      for (const f of r.functions) (g[f.category] ??= []).push(f);
      setGroups(g);
    });
  }, []);

  const q = filter.trim().toLowerCase();
  const match = (f) => !q || f.name.includes(q) || f.description.toLowerCase().includes(q);

  return (
    <section className="manual">
      <div className="card manual-intro">
        <h2>📜 Manual do Stonks</h2>
        <p>
          Tudo que a calculadora sabe fazer. <strong>Clique numa função</strong> para jogá-la direto na calculadora.
          Os nomes em português também funcionam: <code>sen</code>, <code>tg</code>, <code>raiz</code>, <code>mdc</code>, <code>mmc</code>…
        </p>
        <input className="search" placeholder="🔎 procurar função..." value={filter} onChange={(e) => setFilter(e.target.value)} />
      </div>

      {error && <p className="card notice">⚠️ {error}</p>}
      {!groups && !error && <p className="card">Carregando o conhecimento milenar... 🧙</p>}

      <div className="manual-grid">
        {groups &&
          Object.entries(groups).map(([cat, fns]) => {
            const shown = fns.filter(match);
            if (shown.length === 0) return null;
            return (
              <div className="card manual-group" key={cat}>
                <h3>{cat}</h3>
                <ul>
                  {shown.map((f) => (
                    <li key={f.name}>
                      <button className="fn-pill" onClick={() => onInsert(f.name + '(')} title="Inserir na calculadora">
                        <code>{f.signature}</code>
                        <span>{f.description}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}

        <div className="card manual-group">
          <h3>Constantes</h3>
          <ul>
            {CONSTANTS.map(([c, d]) => (
              <li key={c} className="plain">
                <code>{c}</code> <span>{d}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="card manual-group">
          <h3>Sintaxe esperta</h3>
          <ul>
            {SYNTAX.map(([c, d]) => (
              <li key={c} className="plain">
                <code>{c}</code> <span>{d}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
