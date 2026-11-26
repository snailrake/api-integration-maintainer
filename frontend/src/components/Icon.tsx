import type { ReactNode } from 'react';

const drawings = {
  eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" /><circle cx="12" cy="12" r="2.5" /></>,
  eyeOff: <><path d="m3 3 18 18M10.5 10.5a2.2 2.2 0 0 0 3 3M9 6.5a11 11 0 0 1 3-.5c6.5 0 10 6 10 6a15 15 0 0 1-3.8 4M6 8a15 15 0 0 0-4 4s3.5 6 10 6a10 10 0 0 0 3.8-.7" /></>,
  arrow: <path d="M4 12h15m-5-5 5 5-5 5" />,
  brackets: <path d="M8 4H4v16h4m8-16h4v16h-4m-6-2 4-12" />,
  file: <><path d="M13 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10Zm0 0v7h7" /><path d="m10 13-2 2 2 2m4-4 2 2-2 2" /></>,
  branch: <><circle cx="6" cy="5" r="2" /><circle cx="18" cy="7" r="2" /><circle cx="6" cy="19" r="2" /><path d="M6 7v10m12-8a8 8 0 0 1-8 8H6" /></>,
  pullRequest: <><circle cx="6" cy="5" r="2" /><circle cx="6" cy="19" r="2" /><circle cx="18" cy="19" r="2" /><path d="M6 7v10M18 17v-6a5 5 0 0 0-5-5h-1m1-2-2 2 2 2" /></>,
  commit: <><circle cx="12" cy="12" r="3" /><path d="M3 12h6m6 0h6" /></>,
  check: <path d="m5 12 4 4L19 6" />,
  play: <path d="m8 5 11 7-11 7Z" fill="currentColor" stroke="none" />,
  pause: <path d="M8.5 6.5v11m7-11v11" strokeWidth="2.5" />,
  replay: <><path d="M4 6v5h5" /><path d="M4.5 10a8 8 0 1 1 .7 6.5" /></>,
  agent: <><path d="m4 20 1-5L16.5 3.5a1.4 1.4 0 0 1 2 0l2 2a1.4 1.4 0 0 1 0 2L9 19l-5 1Zm10-14 4 4" /></>,
} satisfies Record<string, ReactNode>;

type IconName = keyof typeof drawings;

export function Icon({ name, size = 16 }: { name: IconName; size?: number }) {
  return (
    <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round">
      {drawings[name]}
    </svg>
  );
}
