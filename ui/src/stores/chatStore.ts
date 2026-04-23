import { create } from 'zustand';
import type { ChatMessage, Reference } from '@/types/chat';
import { streamChat } from '@/services/chatService';

interface ChatState {
  messages: ChatMessage[];
  isLoading: boolean;
  isUploading: boolean;
  uploadProgress: number;
  currentAssistantId: number | null;
  currentModel: string;
  currentContextUsed: number;
  currentContextLimit: number;
  error: string | null;
}

interface ChatActions {
  sendMessage: (content: string) => Promise<void>;
  uploadDocument: (file: File) => Promise<void>;
  clearMessages: () => void;
  clearError: () => void;
}

type ChatStore = ChatState & ChatActions;

export const useChatStore = create<ChatStore>((set, get) => ({
  // State
  messages: [],
  isLoading: false,
  isUploading: false,
  uploadProgress: 0,
  currentAssistantId: null,
  currentModel: '',
  currentContextUsed: 0,
  currentContextLimit: 32000,
  error: null,

  // Actions
  sendMessage: async (content: string) => {
    if (!content.trim() || get().isLoading) return;

    const userMsg: ChatMessage = { role: 'user', content: content.trim() };
    const assistantId = Date.now();

    set((state) => ({
      messages: [
        ...state.messages,
        userMsg,
        { role: 'assistant', id: assistantId, content: '', references: [], streaming: true },
      ],
      isLoading: true,
      error: null,
      currentAssistantId: assistantId,
      currentModel: '',
      currentContextUsed: 0,
    }));

    let answer = '';
    let references: Reference[] = [];

    try {
      const controller = new AbortController();

      for await (const event of streamChat(content.trim(), controller.signal)) {
        if (event.type === 'chunk' && event.content !== undefined) {
          answer += event.content;
          set((state) => ({
            messages: state.messages.map((msg) =>
              msg.id === assistantId
                ? { ...msg, content: answer, contextUsed: state.currentContextUsed }
                : msg
            ),
          }));
        } else if (event.type === 'references' && event.references !== undefined) {
          references = event.references as Reference[];
          set((state) => ({
            messages: state.messages.map((msg) =>
              msg.id === assistantId ? { ...msg, references } : msg
            ),
          }));
        } else if (event.type === 'meta' && event.meta !== undefined) {
          set((state) => ({
            currentModel: event.meta!.model,
            currentContextUsed: event.meta!.contextUsed,
            currentContextLimit: event.meta!.contextLimit,
            messages: state.messages.map((msg) =>
              msg.id === assistantId
                ? {
                    ...msg,
                    model: event.meta!.model,
                    contextUsed: event.meta!.contextUsed,
                    contextLimit: event.meta!.contextLimit,
                  }
                : msg
            ),
          }));
        } else if (event.type === 'error') {
          set({ error: event.error || '未知错误' });
          break;
        }
      }

      // Finalize message
      set((state) => ({
        messages: state.messages.map((msg) =>
          msg.id === assistantId
            ? {
                ...msg,
                content: answer,
                references,
                streaming: false,
                model: state.currentModel,
                contextUsed: state.currentContextUsed,
                contextLimit: state.currentContextLimit,
              }
            : msg
        ),
      }));
    } catch (err) {
      set({
        error: err instanceof Error ? err.message : '请求失败',
        messages: get().messages.map((msg) =>
          msg.id === assistantId
            ? {
                ...msg,
                content: `抱歉，请求失败了：${err instanceof Error ? err.message : '未知错误'}`,
                streaming: false,
              }
            : msg
        ),
      });
    } finally {
      set({ isLoading: false, currentAssistantId: null });
    }
  },

  uploadDocument: async (file: File) => {
    if (get().isUploading) return;

    set({ isUploading: true, uploadProgress: 0, error: null });

    // Optimistically add system message
    const systemMsg: ChatMessage = { role: 'system', content: `文档「${file.name}」上传中...` };
    set((state) => ({ messages: [...state.messages, systemMsg] }));

    try {
      const { uploadDocument } = await import('@/services/documentService');
      const result = await uploadDocument(file, (percent) => {
        set({ uploadProgress: percent });
      });

      set((state) => ({
        messages: state.messages.map((msg) =>
          msg.role === 'system' && msg.content.includes(file.name)
            ? { role: 'system', content: `文档「${result.filename}」上传成功，已索引 ${result.status}` }
            : msg
        ),
        isUploading: false,
        uploadProgress: 0,
      }));
    } catch (err) {
      set({
        error: err instanceof Error ? err.message : '上传失败',
        isUploading: false,
        uploadProgress: 0,
        messages: get().messages.map((msg) =>
          msg.role === 'system' && msg.content.includes(file.name)
            ? { role: 'system', content: `文档上传失败：${err instanceof Error ? err.message : '未知错误'}` }
            : msg
        ),
      });
    }
  },

  clearMessages: () => {
    set({
      messages: [],
      error: null,
      currentModel: '',
      currentContextUsed: 0,
    });
  },

  clearError: () => set({ error: null }),
}));
