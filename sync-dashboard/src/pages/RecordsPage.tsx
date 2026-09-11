import { useEffect, useState } from 'react'
import { api } from '../api'
import type { CustomerRecordMapping } from '../types'

const statusStyles: Record<CustomerRecordMapping['syncStatus'], string> = {
    IN_SYNC: 'bg-green-100 text-green-800',
    CONFLICT: 'bg-yellow-100 text-yellow-800',
    ERROR: 'bg-red-100 text-red-800',
}

export default function RecordsPage() {
    const [records, setRecords] = useState<CustomerRecordMapping[]>([])
    const [query, setQuery] = useState('')
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        api.listMappings()
            .then(setRecords)
            .catch(() => setError('Failed to load records.'))
    }, [])

    const filtered = records.filter(r =>
        `${r.canonicalName} ${r.canonicalEmail}`.toLowerCase().includes(query.toLowerCase())
    )

    return (
        <div>
            <h1 className="text-xl font-semibold mb-4">Customer Records</h1>
            {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">{error}</div>}

            <input
                type="text"
                placeholder="Search by name or email..."
                value={query}
                onChange={e => setQuery(e.target.value)}
                className="mb-4 w-full max-w-sm px-3 py-2 border border-gray-300 rounded-md text-sm"
            />

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-gray-50 text-left text-gray-600">
                        <tr>
                            <th className="px-4 py-3">ID</th>
                            <th className="px-4 py-3">Name</th>
                            <th className="px-4 py-3">Email</th>
                            <th className="px-4 py-3">Phone</th>
                            <th className="px-4 py-3">Sys A</th>
                            <th className="px-4 py-3">Sys B</th>
                            <th className="px-4 py-3">Status</th>
                            <th className="px-4 py-3">Last Synced</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {filtered.length === 0 && (
                            <tr>
                                <td colSpan={8} className="px-4 py-8 text-center text-gray-400">
                                    No records match your search.
                                </td>
                            </tr>
                        )}
                        {filtered.map(r => (
                            <tr key={r.id} className="hover:bg-gray-50">
                                <td className="px-4 py-3 font-mono">{r.id}</td>
                                <td className="px-4 py-3">{r.canonicalName}</td>
                                <td className="px-4 py-3">{r.canonicalEmail}</td>
                                <td className="px-4 py-3">{r.canonicalPhone}</td>
                                <td className="px-4 py-3 font-mono">{r.systemAId ?? '—'}</td>
                                <td className="px-4 py-3 font-mono">{r.systemBId ?? '—'}</td>
                                <td className="px-4 py-3">
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusStyles[r.syncStatus]}`}>
                                        {r.syncStatus}
                                    </span>
                                </td>
                                <td className="px-4 py-3">
                                    {r.lastSyncedAt ? new Date(r.lastSyncedAt).toLocaleString() : '—'}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}