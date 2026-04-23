import type { ChatMessage, ChatMeta } from '@/types/chat';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export interface StreamCallbacks {
  onAnswer: (text: string) => void;
  onReferences: (references: ChatMessage['references']) => void;
  onMeta: (meta: ChatMeta) => void;
  onDone: () => void;
  onError: (error: Error) => void;
}

export async function streamChat(
  message: string,
  callbacks: StreamCallbacks
): Promise<void> {
  const { onAnswer, onReferences, onMeta, onDone, onError } = callbacks;

  try {
    const resp = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message }),
    });

    if (!resp.ok) {
      throw new Error(`请求失败: ${resp.status}`);
    }

    if (!resp.body) {
      throw new Error('响应没有可读流');
    }

    const reader = resp.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let currentEvent: string | null = null;
    let answer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const raw of lines) {
        const line = raw.trim();
        if (!line) continue;

        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim();
          continue;
        }

        if (line.startsWith('data:')) {
          const data = line.slice(5).trim();
          if (!data) continue;

          if (currentEvent === 'references') {
            try {
              onReferences(JSON.parse(data));
            } catch {
              // ignore parse error
            }
          } else if (currentEvent === 'meta') {
            try {
              onMeta(JSON.parse(data) as ChatMeta);
            } catch {
              // ignore parse error
            }
          } else if (currentEvent === 'done') {
            onDone();
          } else {
            // plain text chunk (e.g. MiniMax streaming)
            answer += data;
            onAnswer(answer);
          }
          currentEvent = null;
        }
      }
    }
  } catch (err) {
    onError(err instanceof Error ? err : new Error(String(err)));
  }
}
