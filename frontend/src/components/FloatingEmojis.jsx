import { memo } from 'react';

const ITEMS = ['📈', '🤔', '∫', 'π', '🐸', '√', '🔥', 'Σ', '🚀', '∞', '🧠', 'dx', '📉', '🐕', 'e^x', '🥧'];

/** Emojis e símbolos flutuando no fundo, só de enfeite. */
function FloatingEmojis() {
  return (
    <div className="floaters" aria-hidden="true">
      {ITEMS.map((item, i) => (
        <span
          key={i}
          style={{
            left: `${(i * 61) % 100}%`,
            animationDelay: `${-(i * 3.7) % 30}s`,
            animationDuration: `${22 + ((i * 7) % 18)}s`,
            fontSize: `${1.2 + ((i * 13) % 10) / 6}rem`,
          }}
        >
          {item}
        </span>
      ))}
    </div>
  );
}

export default memo(FloatingEmojis);
