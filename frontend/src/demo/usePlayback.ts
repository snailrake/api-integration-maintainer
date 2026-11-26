import { useLayoutEffect, useRef, useState } from 'react';
import { chapters, cues, duration } from './story';

const motionPreference = matchMedia('(prefers-reduced-motion: reduce)');

function phaseAt(time: number) {
  const next = chapters.findIndex(chapter => chapter.from > time);
  return next < 0 ? chapters.length - 1 : Math.max(0, next - 1);
}

export function usePlayback() {
  const canvas = useRef<HTMLDivElement>(null);
  const progress = useRef<HTMLSpanElement>(null);
  const range = useRef<HTMLInputElement>(null);
  const actions = useRef({ toggle() {}, seek(_time: number) {}, pause() {} });
  const [playing, setPlaying] = useState(false);
  const [ended, setEnded] = useState(false);
  const [phase, setPhase] = useState(0);
  const [reduced, setReduced] = useState(motionPreference.matches);

  useLayoutEffect(() => {
    const slider = range.current!;
    const tracks = cues.map(cue => {
      const target = canvas.current!.querySelector(cue.selector);
      if (!target) throw new Error(`Missing animation target ${cue.selector}`);
      return { animation: new Animation(new KeyframeEffect(target, cue.keyframes, {
        delay: cue.delay, duration: cue.duration, easing: cue.easing, fill: 'both',
      })), end: cue.delay + cue.duration };
    });
    const clock = new Animation(new KeyframeEffect(progress.current, [
      { transform: 'scaleX(0)' }, { transform: 'scaleX(1)' },
    ], { duration, fill: 'both' }));
    tracks.push({ animation: clock, end: duration });

    let frame = 0;
    let running = false;
    let previousPhase = -1;
    let lastSample = -Infinity;
    const clampTime = (time: number) => Math.min(duration, Math.max(0, time));
    const currentTime = () => clampTime(Number(clock.currentTime ?? 0));

    function updateControls(time: number, force = false) {
      const next = phaseAt(time);
      if (next !== previousPhase) {
        previousPhase = next;
        setPhase(next);
        force = true;
      }
      if (force || time - lastSample >= 90) {
        slider.value = String(Math.round(time));
        const seconds = (time / 1000).toFixed(1).replace('.', ',');
        slider.setAttribute('aria-valuetext', `${seconds} с · ${chapters[next].label}`);
        lastSample = time;
      }
    }

    function seek(time: number) {
      const position = clampTime(time);
      cancelAnimationFrame(frame);
      for (const { animation } of tracks) {
        animation.pause();
        animation.currentTime = position;
      }
      running = false;
      updateControls(position, true);
      setPlaying(false);
      setEnded(position === duration);
    }

    function tick() {
      const time = currentTime();
      if (time === duration) {
        seek(time);
        return;
      }
      updateControls(time);
      frame = requestAnimationFrame(tick);
    }

    function pause() {
      if (running) seek(currentTime());
    }

    function toggle() {
      const time = currentTime();
      if (motionPreference.matches) {
        seek(chapters.find(chapter => chapter.still > time)?.still ?? chapters[0].still);
        return;
      }
      if (running) {
        pause();
        return;
      }
      const offset = time === duration ? 0 : time;
      if (time === duration) seek(0);
      const now = Number(document.timeline.currentTime);
      for (const { animation, end } of tracks) {
        if (offset < end) animation.startTime = now - offset / (animation.playbackRate || 1);
      }
      running = true;
      setPlaying(true);
      frame = requestAnimationFrame(tick);
    }

    function updatePreference() {
      setReduced(motionPreference.matches);
      if (motionPreference.matches) seek(chapters[phaseAt(currentTime())].still);
    }

    const form = document.querySelector('.auth');
    form?.addEventListener('focusin', pause);
    document.addEventListener('visibilitychange', pause);
    motionPreference.addEventListener('change', updatePreference);
    actions.current = {
      toggle, pause,
      seek: time => seek(motionPreference.matches ? chapters[phaseAt(time)].still : time),
    };
    seek(motionPreference.matches ? chapters[0].still : 0);

    return () => {
      cancelAnimationFrame(frame);
      form?.removeEventListener('focusin', pause);
      document.removeEventListener('visibilitychange', pause);
      motionPreference.removeEventListener('change', updatePreference);
      for (const { animation } of tracks) animation.cancel();
    };
  }, []);

  return { canvas, progress, range, actions, playing, ended, phase, reduced };
}
