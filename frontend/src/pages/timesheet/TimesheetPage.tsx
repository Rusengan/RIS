import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'

import { apiClient } from '../../lib/apiClient'
import type { UserDto } from '../../types/userDto'
import type { WorkSessionDto } from '../../types/workSession'

type TimesheetReportDto = {
  driverId: number
  driverFullName: string
  from: string
  to: string
  totalWorkMinutes: number
  totalBreakMinutes: number
  sessionsCount: number
  sessions: WorkSessionDto[]
}

export function TimesheetPage() {
  const [driverId, setDriverId] = useState<number | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [submitted, setSubmitted] = useState<{ driverId: number; from: string; to: string } | null>(
    null,
  )

  const driversQuery = useQuery({
    queryKey: ['users', 'DRIVER'],
    queryFn: async () => {
      const res = await apiClient.get<{ content: UserDto[] }>('/api/v1/users', {
        params: { role: 'DRIVER', size: 500 },
      })
      return res.data.content
    },
  })

  const reportQuery = useQuery({
    queryKey: ['timesheet', submitted],
    enabled: !!submitted,
    queryFn: async () => {
      if (!submitted) return null
      const res = await apiClient.get<TimesheetReportDto>('/api/v1/reports/timesheet', {
        params: {
          driverId: submitted.driverId,
          from: submitted.from,
          to: submitted.to,
        },
      })
      return res.data
    },
  })

  const rows = useMemo(() => {
    const sessions = reportQuery.data?.sessions ?? []
    return sessions.map((s) => {
      const breakMin =
        s.breaks?.reduce((acc, b) => acc + (b.durationMinutes ?? 0), 0) ?? 0
      return {
        id: s.id,
        date: new Date(s.startedAt).toLocaleDateString(),
        start: new Date(s.startedAt).toLocaleTimeString(),
        end: s.endedAt ? new Date(s.endedAt).toLocaleTimeString() : '—',
        durationMin: s.totalMinutes ?? '—',
        breaksMin: breakMin,
        rowTotal: s.totalMinutes ?? 0,
      }
    })
  }, [reportQuery.data])

  const periodTotal = useMemo(() => {
    const sessions = reportQuery.data?.sessions ?? []
    return sessions.reduce((acc, s) => acc + (s.totalMinutes ?? 0), 0)
  }, [reportQuery.data])

  function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (driverId === '' || !from || !to) return
    const fromIso = new Date(from).toISOString()
    const toIso = new Date(to).toISOString()
    setSubmitted({ driverId, from: fromIso, to: toIso })
  }

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-white">Табель</h2>

      <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-4 rounded-lg border border-slate-700 bg-slate-900/50 p-4">
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">Водитель</span>
          <select
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={driverId === '' ? '' : String(driverId)}
            onChange={(e) => setDriverId(e.target.value ? Number(e.target.value) : '')}
            required
          >
            <option value="">Выберите…</option>
            {(driversQuery.data ?? []).map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">С</span>
          <input
            type="datetime-local"
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            required
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">По</span>
          <input
            type="datetime-local"
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            required
          />
        </label>
        <button
          type="submit"
          className="rounded-md bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-500"
        >
          Сформировать
        </button>
      </form>

      {reportQuery.data && (
        <div className="overflow-x-auto rounded-lg border border-slate-700">
          <table className="min-w-full divide-y divide-slate-700 text-sm text-slate-200">
            <thead className="bg-slate-900">
              <tr>
                <th className="px-4 py-2 text-left">Дата</th>
                <th className="px-4 py-2 text-left">Начало</th>
                <th className="px-4 py-2 text-left">Конец</th>
                <th className="px-4 py-2 text-right">Длительность (мин)</th>
                <th className="px-4 py-2 text-right">Перерывы (мин)</th>
                <th className="px-4 py-2 text-right">Итого</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {rows.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2">{r.date}</td>
                  <td className="px-4 py-2">{r.start}</td>
                  <td className="px-4 py-2">{r.end}</td>
                  <td className="px-4 py-2 text-right">{r.durationMin}</td>
                  <td className="px-4 py-2 text-right">{r.breaksMin}</td>
                  <td className="px-4 py-2 text-right">{r.rowTotal}</td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-slate-900 font-semibold">
              <tr>
                <td colSpan={5} className="px-4 py-3 text-right">
                  Итого за период
                </td>
                <td className="px-4 py-3 text-right">{periodTotal}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  )
}
