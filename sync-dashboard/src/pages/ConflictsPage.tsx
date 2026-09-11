import { useEffect, useState } from 'react'
import { api } from '../api'
import type { SyncConflict } from '../types'

export default function ConflictsPage() {
    const [conflicts, setConflicts] = useState<SyncConflict[]>([])
    const [error, setError] = useState<string | null>(null)
    const [manualValue, setManualValue] = useState<Record<number, string>>({})

    const load = async () => {
        try {
            setError(null)
            setConflicts(await api.listConflicts('UNRESOLVED'))
        } catch (e) {
            setError('Failed to load conflicts.')
        }
    }

    useEffect(() => { load() }, [])

    const resolve = async (id: number, resolution: string, value?: string) => {
        try {
            await api.resolveConflict(id, { resolution, manualValue: value })
            await load()
        } catch (e) {
            setError('Resolution failed.')
        }
    }

    return (
        <div>
            <h1 className="text-xl font-semibold mb-4">Conflicts Inbox</h1>
            {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">{error}</div>}
            {conflicts.length === 0 && (
                <div className="bg-white rounded-lg shadow p-8 text-center text-gray-400">
                    No unresolved conflicts. Everything is in sync.
                </div>
            )}

            <div className="space-y-4">
                {conflicts.map(conflict => (
                    <div key={conflict.id} className="bg-white rounded-lg shadow p-4">
                        <div className="flex items-center justify-between mb-3">
                            <div>
                                <span className="text-sm font-medium text-gray-800">
                                    Mapping #{conflict.mapping?.id}
                                </span>
                                <span className="ml-2 text-xs text-gray-500">
                                    Conflict #{conflict.id} (Job #{conflict.syncJob?.id})
                                </span>
                            </div>
                            <span className="px-2 py-0.5 bg-yellow-100 text-yellow-800 rounded-full text-xs font-medium">
                                {conflict.fieldName}
                            </span>
                        </div>

                        <div className="grid grid-cols-2 gap-4 mb-3">
                            <div className="bg-cyan-50 border border-cyan-100 rounded-md p-3">
                                <div className="text-xs text-cyan-700 uppercase font-medium mb-1">System A value</div>
                                <div className="text-sm font-semibold break-all">{conflict.valueFromA}</div>
                            </div>
                            <div className="bg-purple-50 border border-purple-100 rounded-md p-3">
                                <div className="text-xs text-purple-700 uppercase font-medium mb-1">System B value</div>
                                <div className="text-sm font-semibold break-all">{conflict.valueFromB}</div>
                            </div>
                        </div>

                        <div className="flex items-center gap-3 text-sm">
                            <button
                                onClick={() => resolve(conflict.id, 'A_WINS')}
                                className="px-3 py-1.5 bg-cyan-600 text-white rounded-md hover:bg-cyan-700"
                            >
                                Keep A's value
                            </button>
                            <button
                                onClick={() => resolve(conflict.id, 'B_WINS')}
                                className="px-3 py-1.5 bg-purple-600 text-white rounded-md hover:bg-purple-700"
                            >
                                Keep B's value
                            </button>

                            <div className="flex items-center gap-2 ml-2 flex-1">
                                <input
                                    type="text"
                                    placeholder="Manual value..."
                                    value={manualValue[conflict.id] ?? ''}
                                    onChange={e => setManualValue({ ...manualValue, [conflict.id]: e.target.value })}
                                    className="flex-1 px-3 py-1.5 border border-gray-300 rounded-md text-sm"
                                />
                                <button
                                    onClick={() => resolve(conflict.id, 'MANUAL', manualValue[conflict.id])}
                                    className="px-3 py-1.5 bg-gray-700 text-white rounded-md hover:bg-gray-800"
                                >
                                    Enter manual value
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}