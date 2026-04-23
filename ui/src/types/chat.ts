export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  id?: number;
  references?: Reference[];
  model?: string;
  contextUsed?: number;
  contextLimit?: number;
  streaming?: boolean;
}

export interface Reference {
  content: string;
  documentId: string;
  score: number;
}

export interface ChatMeta {
  model: string;
  contextUsed: number;
  contextLimit: number;
}

export interface DocumentResponse {
  id: string;
  filename: string;
  size: number;
  status: string;
}
