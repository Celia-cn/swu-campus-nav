import { request } from '@/api/http'
import type { RagDocument, RagUpsertPayload } from '@/types/api'

export function fetchRagDocuments() {
  return request<RagDocument[]>('/api/admin/rag/documents')
}

export function createRagDocument(payload: RagUpsertPayload) {
  return request<RagDocument>('/api/admin/rag/documents', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateRagDocument(id: number, payload: RagUpsertPayload) {
  return request<RagDocument>(`/api/admin/rag/documents/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteRagDocument(id: number) {
  return request<void>(`/api/admin/rag/documents/${id}`, { method: 'DELETE' })
}
