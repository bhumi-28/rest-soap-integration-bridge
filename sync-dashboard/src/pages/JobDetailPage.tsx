import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import type { SyncJob } from '../types'

export default function JobDetailPage() {
    const { id } = useParams()
    const [job, setJob] = useState<SyncJob | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!id) return
        api.getJob(Number(id))
            .then(setJob)
            .catch(() => setError('Failed to load job'))
    }, [id])

    if (error) return <div className="p-3 bg-red-50 text-red-700 rounded-md text-sm">{error}</div>
    if (!job) return <div className="text-gray-500 text-sm">Loading...</div>

    const duration = job.completedAt
        ? ((new Date(job.completedAt).getTime() - new Date(job.startedAt).getTime()) / 1000).toFixed(2)
        : null

    return (
        <div>
            <Link to="/" className="text-cyan-600 hover:underline text-sm">← Back to jobs</Link>
            <h1 className="text-xl font-semibold mt-2 mb-4">Sync Job #{job.id}</h1>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <StatCard label="Status" value={job.status} />
                <StatCard label="Processed" value={String(job.recordsProcessed)} />
                <StatCard label="Conflicted" value={String(job.recordsConflicted)} />
                <StatCard label="Failed" value={String(job.recordsFailed)} />
            </div>

            <div className="bg-white rounded-lg shadow p-4 text-sm space-y-2">
                <div><span className="text-gray-500">Started:</span> {new Date(job.startedAt).toLocaleString()}</div>
                <div><span className="text-gray-500">Completed:</span> {job.completedAt ? new Date(job.completedAt).toLocaleString() : '—'}</div>
                <div><span className="text-gray-500">Duration:</span> {duration ? `${duration}s` : '—'}</div>
            </div>
        </div>
    )
}

function StatCard({ label, value }: { label: string; value: string }) {
    return (
        <div className="bg-white rounded-lg shadow p-4">
            <div className="text-xs text-gray-500 uppercase">{label}</div>
            <div className="text-2xl font-semibold mt-1">{value}</div>
        </div>
    )
}