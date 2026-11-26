import { useLayoutEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import { LoginForm } from './components/LoginForm';
import { Icon } from './components/Icon';
import '@fontsource-variable/golos-text/index.css';
import '@fontsource/ibm-plex-mono/latin-400.css';
import '@fontsource/ibm-plex-mono/latin-500.css';
import './styles/tokens.css';
import './styles/login.css';
import { aperture, chapters, duration, example } from './demo/story';
import { CompactScene, Scene } from './demo/Scene';
import { usePlayback } from './demo/usePlayback';
import './styles/panel.css';
import './styles/scene.css';
import './styles/graphite.css';

function DemoPanel() {
  const { canvas, progress, range, actions, playing, ended, phase, reduced } = usePlayback();
  const viewport = useRef<HTMLDivElement>(null);
  const transcript = useRef<HTMLDialogElement>(null);
  const textButton = useRef<HTMLButtonElement>(null);
  const current = chapters[phase];
  const playLabel = reduced ? ended ? 'К началу' : 'Следующий кадр'
    : playing ? 'Пауза' : ended ? 'Повторить' : 'Воспроизвести';
  const playIcon = playing ? 'pause' : ended ? 'replay' : reduced ? 'arrow' : 'play';

  useLayoutEffect(() => {
    const view = viewport.current!;
    const stage = canvas.current!;
    const fit = () => {
      const scale = Math.min(view.clientWidth / aperture.width, view.clientHeight / aperture.height, 1.13);
      stage.style.transform = `translate(-50%, -50%) scale(${scale})`;
    };
    const resize = new ResizeObserver(fit);
    resize.observe(view);
    fit();
    return () => resize.disconnect();
  }, [canvas]);

  return (
    <section className="a3-panel" aria-label="Демонстрация API Maintainer" data-phase={phase}>
      <header className="a3-header">
        <span className="a3-repository"><Icon name="branch" size={17} /><code>{example.repository}</code></span>
        <span className="a3-demo"><span />Демо</span>
      </header>
      <div className="a3-aperture" ref={viewport} aria-hidden="true">
        <div className="a3-canvas" ref={canvas}>
          <div className="a3-window"><Scene /></div>
        </div>
        <div className="a3-compact">
          <CompactScene phase={phase} />
        </div>
      </div>
      <div className="a3-transport">
        <div className="a3-scrub">
          <span className="a3-scrub-track" aria-hidden="true"><span ref={progress} /></span>
          <input ref={range} type="range" min={0} max={duration} step={1} defaultValue={0}
            aria-label="Позиция воспроизведения" onInput={event => actions.current.seek(event.currentTarget.valueAsNumber)}
            onKeyDown={event => {
              if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) return;
              event.preventDefault();
              const direction = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 1;
              const next = chapters[Math.max(0, Math.min(chapters.length - 1, phase + direction))];
              actions.current.seek(reduced ? next.still : event.currentTarget.valueAsNumber + 250 * direction);
            }} />
        </div>
        <div className="a3-controls">
          <span className="a3-phase" role="status">{current.label}</span>
          <button ref={textButton} className="a3-text" type="button" aria-haspopup="dialog" aria-controls="a3-transcript"
            onClick={() => { actions.current.pause(); transcript.current!.showModal(); }}>Сценарий</button>
          <button type="button" className="a3-play" onClick={() => actions.current.toggle()}>
            <Icon name={playIcon} size={17} />{playLabel}
          </button>
        </div>
      </div>
      <dialog id="a3-transcript" ref={transcript} aria-labelledby="a3-transcript-title" onClose={() => textButton.current?.focus()}>
        <div className="a3-dialog-heading">
          <h2 id="a3-transcript-title">Демо-сценарий</h2>
          <button type="button" onClick={() => transcript.current!.close()}>Закрыть</button>
        </div>
        <ol>
          {chapters.map(chapter => <li key={chapter.from}><strong>{chapter.label}</strong><p>{chapter.text}</p></li>)}
        </ol>
      </dialog>
    </section>
  );
}

createRoot(document.getElementById('root')!).render(<main className="login-a3"><LoginForm /><DemoPanel /></main>);
