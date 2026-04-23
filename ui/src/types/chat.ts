export interface Reference {
  content: string;
  score: number;
  metadata?: Record<string, unknown>;
}

export interface ChatMessage {
  id?: string | number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  model?: string;
  contextUsed?: number;
  contextLimit?: number;
  references?: Reference[];
  streaming?: boolean;
  error?: boolean;
}
