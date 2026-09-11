import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import SyncJobsPage from './pages/SyncJobsPage'
import JobDetailPage from './pages/JobDetailPage'
import ConflictsPage from './pages/ConflictsPage'
import RecordsPage from './pages/RecordsPage'

export default function App() {
    return (
        <BrowserRouter>
            <div className="min-h-screen bg-gray-100">
                <nav className="bg-slate-800 text-white shadow">
                    <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
                        <div className="text-lg font-semibold">Sync Dashboard</div>
                        <div className="flex gap-4 text-sm">
                            <NavLink to="/" className={({ isActive }) =>
                                isActive ? 'text-cyan-400' : 'hover:text-cyan-300'}>
                                Sync Jobs
                            </NavLink>
                            <NavLink to="/conflicts" className={({ isActive }) =>
                                isActive ? 'text-cyan-400' : 'hover:text-cyan-300'}>
                                Conflicts
                            </NavLink>
                            <NavLink to="/records" className={({ isActive }) =>
                                isActive ? 'text-cyan-400' : 'hover:text-cyan-300'}>
                                Records
                            </NavLink>
                        </div>
                    </div>
                </nav>
                <main className="max-w-6xl mx-auto px-4 py-6">
                    <Routes>
                        <Route path="/" element={<SyncJobsPage />} />
                        <Route path="/jobs/:id" element={<JobDetailPage />} />
                        <Route path="/conflicts" element={<ConflictsPage />} />
                        <Route path="/records" element={<RecordsPage />} />
                    </Routes>
                </main>
            </div>
        </BrowserRouter>
    )
}