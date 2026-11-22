/** Quadro de meme clássico: emoji gigante + texto Impact em cima e embaixo. */
export default function MemeCard({ meme, compact = false }) {
  return (
    <figure className={`meme tone-${meme.tone ?? 'sky'} ${compact ? 'compact' : ''}`} key={meme.top + meme.bottom}>
      <figcaption className="meme-text top">{meme.top}</figcaption>
      <div className="meme-emoji" aria-hidden="true">
        {meme.emoji}
      </div>
      <figcaption className="meme-text bottom">{meme.bottom}</figcaption>
    </figure>
  );
}
