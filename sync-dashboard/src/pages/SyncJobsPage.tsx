import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import type { SyncJob } from '../types'

const statusColors: Record<SyncJob['status'], string> = {
    RUNNING: 'bg-blue-100 text-blue-800',
    SUCCESS: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
    PARTIAL: 'bg-yellow-100 text-yellow-800',
}

export default function SyncJobsPage() {
    const [jobs, setJobs] = useState<SyncJob[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const loadJobs = async () => {
        try {
            setError(null)
            setJobs(await api.listJobs())
        } catch (e) {
            setError('Failed to load sync jobs. Is the bridge service running?')
        }
    }

    useEffect(() => {
        loadJobs()
    }, [])

    const triggerSync = async () => {
        setLoading(true)
        try {
            await api.triggerSync()
            await loadJobs()
        } catch (e) {
            setError('Sync trigger failed. Check bridge service connectivity.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div>
            <div className="flex items-center justify-between mb-4">
                <h1 className="text-xl font-semibold">Sync Job History</h1>
                <button
                    onClick={triggerSync}
                    disabled={loading}
                    className="bg-cyan-600 hover:bg-cyan-700 disabled:opacity-50 text-white px-4 py-2 rounded-md text-sm"
                >
                    {loading ? 'Triggering...' : 'Trigger Sync Now'}
                </button>
            </div>

            {error && <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">{error}</div>}

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-gray-50 text-left text-gray-600">
                        <tr>
                            <th className="px-4 py-3">ID</th>
                            <th className="px-4 py-3">Started</th>
                            <th className="px-4 py-3">Completed</th>
                            <th className="px-4 py-3">Status</th>
                            <th className="px-4 py-3 text-right">Processed</th>
                            <th className="px-4 py-3 text-right">Conflicted</th>
                            <th className="px-4 py-3 text-right">Failed</th>
                            <th className="px-4 py-3">Detail</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {jobs.length === 0 && (
                            <tr>
                                <td colSpan={8} className="px-4 py-8 text-center text-gray-400">
                                    No sync jobs yet. Click "Trigger Sync Now".
                                </td>
                            </tr>
                        )}
                        {jobs.map(job => (
                            <tr key={job.id} className="hover:bg-gray-50">
                                <td className="px-4 py-3 font-mono">{job.id}</td>
                                <td className="px-4 py-3">{new Date(job.startedAt).toLocaleString()}</td>
                                <td className="px-4 py-3">
                                    {job.completedAt ? new Date(job.completedAt).toLocaleString() : '—'}
                                </td>
                                <td className="px-4 py-3">
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColors[job.status]}`}>
                                        {job.status}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-right">{job.recordsProcessed}</td>
                                <td className="px-4 py-3 text-right">{job.recordsConflicted}</td>
                                <td className="px-4 py-3 text-right">{job.recordsFailed}</td>
                                <td className="px-4 py-3">
                                    <Link to={`/jobs/${job.id}`} className="text-cyan-600 hover:underline">View</Link>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}