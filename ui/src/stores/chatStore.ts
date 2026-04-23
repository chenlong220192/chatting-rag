import { create } from 'zustand';
import type { ChatMessage } from '@/types/chat';

interface ChatState {
  messages: ChatMessage[];
  loading: boolean;
  uploading: boolean;
  addMessage: (msg: ChatMessage) => void;
  updateMessage: (id: string | number, patch: Partial<ChatMessage>) => void;
  setLoading: (loading: boolean) => void;
  setUploading: (uploading: boolean) => void;
  reset: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  loading: false,
  uploading: false,

  addMessage: (msg) =>
    set((state) => ({ messages: [...state.messages, msg] })),

  updateMessage: (id, patch) =>
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === id ? { ...msg, ...patch } : msg
      ),
    })),

  setLoading: (loading) => set({ loading }),
  setUploading: (uploading) => set({ uploading }),

  reset: () => set({ messages: [], loading: false, uploading: false }),
}));
