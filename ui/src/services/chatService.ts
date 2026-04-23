import { API_BASE } from '@/utils/apiClient';
import type { ChatMeta, Reference } from '@/types/chat';

interface ChatChunkResult {
  type: 'chunk' | 'references' | 'meta' | 'done' | 'error';
  content?: string;
  references?: Reference[];
  meta?: ChatMeta;
  error?: string;
}

export async function* streamChat(
  message: string,
  signal?: AbortSignal
): AsyncGenerator<ChatChunkResult, void, unknown> {
  const resp = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
    signal,
  });

  if (!resp.ok) {
    yield { type: 'error', error: `请求失败: ${resp.status}` };
    return;
  }

  if (!resp.body) {
    yield { type: 'error', error: '响应体为空' };
    return;
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let currentEvent: string | null = null;

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

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
              yield { type: 'references', references: JSON.parse(data) };
            } catch {
              // ignore parse errors
            }
          } else if (currentEvent === 'meta') {
            try {
              yield { type: 'meta', meta: JSON.parse(data) as ChatMeta };
            } catch {
              // ignore
            }
          } else if (currentEvent === 'done') {
            yield { type: 'done' };
          } else {
            // plain text chunk
            yield { type: 'chunk', content: data };
          }
          currentEvent = null;
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}
