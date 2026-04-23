import { create } from 'zustand';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export interface ChatMessage {
  id?: string | number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  model?: string;
  contextUsed?: number;
  contextLimit?: number;
  references?: Array<{
    content: string;
    score: number;
    metadata?: Record<string, unknown>;
  }>;
  streaming?: boolean;
  error?: boolean;
}

interface ChatStore {
  messages: ChatMessage[];
  isLoading: boolean;
  isUploading: boolean;

  sendMessage: (content: string) => Promise<void>;
  uploadDocument: (file: File) => Promise<void>;
  clearMessages: () => void;
}

export const useChatStore = create<ChatStore>((set, get) => ({
  messages: [],
  isLoading: false,
  isUploading: false,

  sendMessage: async (content: string) => {
    if (get().isLoading) return;

    const assistantId = Date.now();
    const userMsg: ChatMessage = { id: `user-${Date.now()}`, role: 'user', content };

    set(state => ({
      messages: [
        ...state.messages,
        userMsg,
        {
          id: assistantId,
          role: 'assistant',
          content: '',
          references: [],
          streaming: true,
          model: '',
          contextUsed: 0,
          contextLimit: 32000,
        },
      ],
      isLoading: true,
    }));

    try {
      const resp = await fetch(`${API_BASE}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: content }),
      });

      if (!resp.ok) throw new Error('请求失败: ' + resp.status);

      const reader = resp.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let references: ChatMessage['references'] = [];
      let answer = '';
      let currentEvent: string | null = null;
      let model = '';
      let contextUsed = 0;
      let contextLimit = 32000;

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
                references = JSON.parse(data);
              } catch {
                // ignore parse errors
              }
            } else if (currentEvent === 'meta') {
              try {
                const m = JSON.parse(data);
                model = m.model || model;
                contextUsed = m.contextUsed || 0;
                contextLimit = m.contextLimit || 32000;
              } catch {
                // ignore parse errors
              }
            } else if (currentEvent !== 'done') {
              // plain text chunk (MiniMax streaming)
              answer += data;
              contextUsed = Math.floor(answer.length / 4);
              set(state => ({
                messages: state.messages.map(msg =>
                  msg.id === assistantId
                    ? { ...msg, content: answer, model, contextUsed, contextLimit }
                    : msg
                ),
              }));
            }
            currentEvent = null;
          }
        }
      }

      set(state => ({
        messages: state.messages.map(msg =>
          msg.id === assistantId
            ? { ...msg, content: answer, references, streaming: false, model, contextUsed, contextLimit }
            : msg
        ),
        isLoading: false,
      }));
    } catch (err) {
      set(state => ({
        messages: state.messages.map(msg =>
          msg.id === assistantId
            ? { ...msg, content: '抱歉，请求失败了：' + (err as Error).message, streaming: false, error: true }
            : msg
        ),
        isLoading: false,
      }));
    }
  },

  uploadDocument: async (file: File) => {
    if (get().isUploading) return;
    set({ isUploading: true });

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await fetch(`${API_BASE}/documents`, { method: 'POST', body: formData });
      if (!res.ok) throw new Error('上传失败');
      const data = await res.json();

      set(state => ({
        messages: [
          ...state.messages,
          {
            id: `system-${Date.now()}`,
            role: 'system',
            content: `文档「${data.filename}」上传成功，已索引 ${data.status}`,
          },
        ],
      }));
    } catch (err) {
      set(state => ({
        messages: [
          ...state.messages,
          {
            id: `system-${Date.now()}`,
            role: 'system',
            content: '文档上传失败：' + (err as Error).message,
          },
        ],
      }));
    } finally {
      set({ isUploading: false });
    }
  },

  clearMessages: () => set({ messages: [] }),
}));
