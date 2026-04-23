const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = {
  baseUrl: API_BASE,

  async get<T>(path: string): Promise<T> {
    const resp = await fetch(`${API_BASE}${path}`);
    if (!resp.ok) throw new Error(`GET ${path} failed: ${resp.status}`);
    return resp.json() as Promise<T>;
  },

  async post<T>(path: string, body: unknown): Promise<T> {
    const resp = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!resp.ok) throw new Error(`POST ${path} failed: ${resp.status}`);
    return resp.json() as Promise<T>;
  },
};
