export interface SyncJob {
    id: number
    startedAt: string
    completedAt: string | null
    status: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'PARTIAL'
    recordsProcessed: number
    recordsConflicted: number
    recordsFailed: number
}

export interface SyncConflict {
    id: number
    syncJob?: { id: number }
    mapping?: { id: number }
    fieldName: string
    valueFromA: string
    valueFromB: string
    resolution: 'UNRESOLVED' | 'A_WINS' | 'B_WINS' | 'MANUAL'
    resolvedBy: string | null
    resolvedAt: string | null
}

export interface CustomerRecordMapping {
    id: number
    systemAId: number | null
    systemBId: number | null
    canonicalName: string
    canonicalEmail: string
    canonicalPhone: string
    lastSyncedAt: string | null
    syncStatus: 'IN_SYNC' | 'CONFLICT' | 'ERROR'
}