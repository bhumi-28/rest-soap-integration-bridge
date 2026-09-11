import type { CustomerRecordMapping, SyncConflict, SyncJob } from './types'

const BASE = '/api/sync'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const res = await fetch(`${BASE}${path}`, options)
    if (!res.ok) throw new Error(`Request failed: ${res.status}`)
    return res.json() as Promise<T>
}

export const api = {
    triggerSync: () => request<SyncJob>('/trigger', { method: 'POST' }),

    listJobs: () => request<SyncJob[]>('/jobs'),

    getJob: (id: number) => request<SyncJob>(`/jobs/${id}`),

    listConflicts: (status = 'UNRESOLVED') =>
        request<SyncConflict[]>(`/conflicts?status=${status}`),

    resolveConflict: (id: number, body: { resolution: string; manualValue?: string }) =>
        request<SyncConflict>(`/conflicts/${id}/resolve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        }),

    listMappings: () => request<CustomerRecordMapping[]>('/mappings'),
}