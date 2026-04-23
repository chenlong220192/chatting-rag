export interface ChatMessage {
  id?: string | number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  references?: Reference[];
  streaming?: boolean;
  model?: string;
  contextUsed?: number;
  contextLimit?: number;
  timestamp?: number;
}

export interface Reference {
  content: string;
  score: number;
  metadata?: Record<string, unknown>;
}

export interface ChatMeta {
  model: string;
  contextUsed: number;
  contextLimit: number;
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponseEvent {
  event?: 'references' | 'meta' | 'done' | 'answer';
  data?: string;
}

export interface DocumentUploadResponse {
  filename: string;
  status: string;
  chunkCount?: number;
}
