import type { DocumentUploadResponse } from '@/types/chat';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export async function uploadDocument(file: File): Promise<DocumentUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const resp = await fetch(`${API_BASE}/documents`, {
    method: 'POST',
    body: formData,
  });

  if (!resp.ok) {
    throw new Error(`上传失败: ${resp.status}`);
  }

  return resp.json() as Promise<DocumentUploadResponse>;
}
