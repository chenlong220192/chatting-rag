import { useEffect, useRef } from 'react';

export function useScrollToBottom(): React.RefObject<HTMLDivElement | null> {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    ref.current?.scrollIntoView({ behavior: 'smooth' });
  });

  return ref;
}
