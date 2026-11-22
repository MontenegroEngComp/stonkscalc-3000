// Reações em formato de meme para cada resultado. Cada meme: { emoji, top, bottom, tone }.
// tone define a cor de fundo do "quadro" do meme: sun | mint | sky | pink | danger | night

const pick = (list) => list[Math.floor(Math.random() * list.length)];
const near = (v, target) => Math.abs(v - target) < 1e-9 * Math.max(1, Math.abs(target));

export const IDLE_MEME = {
  emoji: '🤔',
  top: 'NAZARÉ CONFUSA',
  bottom: 'esperando você digitar alguma conta',
  tone: 'sky',
};

export const LOADING_MEME = {
  emoji: '🧮',
  top: 'CALCULANDO...',
  bottom: 'o Java tá pensando, confia',
  tone: 'sky',
};

const OFFLINE_MEME = {
  emoji: '🔥🐶☕',
  top: 'THIS IS FINE',
  bottom: 'o backend Java está fora do ar',
  tone: 'danger',
};

const ERROR_MEMES = [
  { emoji: '🔥🐶☕', top: 'THIS IS FINE', bottom: 'deu erro, mas tá tudo sob controle' },
  { emoji: '🤯', top: 'NAZARÉ TENTANDO', bottom: 'entender essa expressão' },
  { emoji: '😐', top: 'EU NÃO SEI', bottom: 'o que você esperava com isso' },
  { emoji: '🫠', top: 'ERROU', bottom: 'faz o L e tenta de novo' },
  { emoji: '🙈', top: 'FINGE QUE', bottom: 'eu não vi esse erro' },
];

const NICE_MEMES = [
  { emoji: '🐕', top: 'MUITO CÁLCULO', bottom: 'tal ciência. uau.' },
  { emoji: '🧠', top: 'BIG BRAIN', bottom: 'nível: Einstein de pijama' },
  { emoji: '🫴', top: 'RECEBA!', bottom: 'resultado fresquinho' },
  { emoji: '🤝', top: 'CONFIA', bottom: 'a matemática nunca mente' },
  { emoji: '😎', top: 'O PAI TÁ ON', bottom: 'cálculo concluído com sucesso' },
  { emoji: '🧘', top: 'CALMA, CALABRESO', bottom: 'tá tudo calculado' },
  { emoji: '🐸', top: 'É ISSO AÍ', bottom: 'deu bom, sapão' },
  { emoji: '🚀', top: 'TO THE MOON', bottom: 'precisão de foguete' },
];

const TONES = ['sun', 'mint', 'sky', 'pink'];
const withTone = (m) => ({ tone: pick(TONES), ...m });

/**
 * @param {object} p
 * @param {number|null} p.value    valor numérico (null quando infinito)
 * @param {boolean} p.infinite
 * @param {string} p.error
 * @param {boolean} p.offline
 * @param {number|null} p.previous resultado anterior (para STONKS / NOT STONKS)
 * @param {string} p.expression
 */
export function pickMeme({ value, infinite, error, offline, previous, expression = '' }) {
  if (offline) return OFFLINE_MEME;
  if (error) {
    if (/indefinido|domínio/i.test(error)) {
      return { emoji: '🚫', top: 'A MATEMÁTICA', bottom: 'disse: não. fora do domínio.', tone: 'danger' };
    }
    return { ...pick(ERROR_MEMES), tone: 'danger' };
  }
  if (infinite) {
    return { emoji: '💥', top: 'VOCÊ DIVIDIU POR ZERO', bottom: 'o universo agradece o caos', tone: 'night' };
  }
  const v = value;
  const compact = expression.replace(/\s+/g, '');

  if (compact === '1+1') return withTone({ emoji: '🎓', top: '1 + 1 = 2', bottom: 'ou 11, se você for de humanas' });
  if (compact === '2+2') return withTone({ emoji: '🧑‍🏫', top: '2 + 2 = 4', bottom: 'menos 1 dá 3, quick maths' });
  if (near(v, 69)) return withTone({ emoji: '😏', top: 'NICE', bottom: 'número muito respeitável' });
  if (near(v, 42)) return withTone({ emoji: '🌌', top: 'A RESPOSTA', bottom: 'para a vida, o universo e tudo mais' });
  if (near(v, 0)) return { emoji: '🫏', top: 'QUE BURRO', bottom: 'DÁ ZERO PRA ELE', tone: 'pink' };
  if (near(v, 7)) return withTone({ emoji: '⚽', top: 'SIUUUUU', bottom: 'resultado camisa 7' });
  if (near(v, 10)) return withTone({ emoji: '⭐', top: 'NOTA 10', bottom: 'passou de ano direto' });
  if (near(v, 100)) return withTone({ emoji: '💯', top: 'CEM POR CENTO', bottom: 'sem margem de erro' });
  if (near(v, 404)) return withTone({ emoji: '🕵️', top: 'ERRO 404', bottom: 'brincadeira, deu 404 mesmo' });
  if (near(v, 1000)) return withTone({ emoji: '🔥', top: 'MIL GRAU', bottom: 'cálculo pegando fogo' });
  if (near(v, Math.PI)) return withTone({ emoji: '🥧', top: 'π É VIDA', bottom: '3,14159... e contando' });
  if (near(v, Math.E)) return withTone({ emoji: '🌿', top: 'EULER APROVA', bottom: 'e = 2,718... natural igual alface' });
  if (Math.abs(v) >= 1e6) return withTone({ emoji: '🤑', top: 'NÚMERO MILIONÁRIO', bottom: 'mais dígitos que meu salário' });
  if (Math.abs(v) > 9000) return { emoji: '💪', top: "IT'S OVER 9000!!!", bottom: 'o scouter explodiu', tone: 'sun' };
  if (v < 0) return { emoji: '📉', top: 'NEGATIVADO', bottom: 'igual meu nome no Serasa', tone: 'pink' };
  if (Math.abs(v) < 1e-3) return withTone({ emoji: '🔬', top: 'TÃO PEQUENO', bottom: 'precisa de microscópio pra ver' });
  if (previous != null && Number.isFinite(previous) && v !== previous) {
    return v > previous
      ? { emoji: '📈', top: 'STONKS', bottom: 'o resultado subiu', tone: 'mint' }
      : { emoji: '📉', top: 'NOT STONKS', bottom: 'o resultado caiu', tone: 'pink' };
  }
  return withTone(pick(NICE_MEMES));
}

/** Legenda divertida para o gráfico, com base na tendência da primeira função. */
export function graphMeme(series) {
  const s = series.find((x) => x.ys && x.ys.some((y) => y != null));
  if (!s) return { emoji: '🤷', text: 'Plota alguma coisa aí, o gráfico tá carente' };
  const ys = s.ys.filter((y) => y != null);
  const first = ys[0];
  const last = ys[ys.length - 1];
  if (/sin|cos/.test(s.expression)) return { emoji: '🌊', text: 'Ondinha bonita. Surfa, Medina!' };
  if (Math.abs(last - first) < 1e-9) return { emoji: '😐', text: 'Linha reta. Emoção zero.' };
  return last > first
    ? { emoji: '📈', text: 'STONKS' }
    : { emoji: '📉', text: 'NOT STONKS' };
}
