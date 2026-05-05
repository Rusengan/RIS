import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'

import { apiClient } from '../lib/apiClient'
import { useAuthStore } from '../store/authStore'
import { hasAnyRole } from '../types/role'
import type { WorkSessionDto, WorkSessionStatus } from '../types/workSession'

type PageDto<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const STATUS_LABEL: Record<WorkSessionStatus, string> = {
  OPEN: 'Открыта',
  CLOSED: 'Закрыта',
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function formatDuration(totalMinutes: number | null, startedAt: string, endedAt: string | null): string {
  // If totalMinutes is null and the session is still OPEN, compute live elapsed.
  let minutes: number
  if (totalMinutes != null) {
    minutes = totalMinutes
  } else {
    const startMs = new Date(startedAt).getTime()
    const endMs = endedAt ? new Date(endedAt).getTime() : Date.now()
    minutes = Math.max(0, Math.floor((endMs - startMs) / 60000))
  }
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${h}ч ${m.toString().padStart(2, '0')}м`
}

export function WorkSessionsPage() {
  const roles = useAuthStore((s) => s.user?.roles ?? [])
  const isDriverOnly = hasAnyRole(roles, ['DRIVER']) && !hasAnyRole(roles, ['DISPATCHER', 'ADMIN'])

  const [statusFilter, setStatusFilter] = useState<WorkSessionStatus | 'ALL'>('ALL')
  const [fromDate, setFromDate] = useState<string>('') // yyyy-mm-dd
  const [toDate, setToDate] = useState<string>('')

  // Drivers see only their own sessions; dispatchers/admins see all.
  const endpoint = isDriverOnly ? '/api/v1/work-sessions/mine' : '/api/v1/work-sessions'

  const params = useMemo(() => {
    const p: Record<string, string> = {
      size: '50',
      sort: 'startedAt,desc',
    }
    if (statusFilter !== 'ALL') p.status = statusFilter
    if (fromDate) p.from = new Date(`${fromDate}T00:00:00`).toISOString()
    if (toDate) p.to = new Date(`${toDate}T23:59:59`).toISOString()
    return p
  }, [statusFilter, fromDate, toDate])

  const sessionsQuery = useQuery({
    queryKey: ['work-sessions', endpoint, params],
    queryFn: async () => {
      const res = await apiClient.get<PageDto<WorkSessionDto>>(endpoint, { params })
      return res.data
    },
  })

  const sessions = sessionsQuery.data?.content ?? []
  const total = sessionsQuery.data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-white">Смены</h2>
          <p className="text-sm text-slate-400">
            {isDriverOnly ? 'Ваши рабочие смены и перерывы.' : 'Все смены и учёт рабочего времени.'}
          </p>
        </div>
        <span className="text-xs text-slate-500">Всего записей: {total}</span>
      </div>

      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-700 bg-slate-900/40 p-4">
        <div className="flex flex-col">
          <label className="mb-1 text-xs uppercase tracking-wide text-slate-400">Статус</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as WorkSessionStatus | 'ALL')}
            className="rounded-md border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-200"
          >
            <option value="ALL">Все</option>
            <option value="OPEN">{STATUS_LABEL.OPEN}</option>
            <option value="CLOSED">{STATUS_LABEL.CLOSED}</option>
          </select>
        </div>
        <div className="flex flex-col">
          <label className="mb-1 text-xs uppercase tracking-wide text-slate-400">С даты</label>
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className="rounded-md border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-200"
          />
        </div>
        <div className="flex flex-col">
          <label className="mb-1 text-xs uppercase tracking-wide text-slate-400">По дату</label>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className="rounded-md border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-200"
          />
        </div>
        {(fromDate || toDate || statusFilter !== 'ALL') && (
          <button
            type="button"
            onClick={() => {
              setStatusFilter('ALL')
              setFromDate('')
              setToDate('')
            }}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-700"
          >
            Сбросить
          </button>
        )}
      </div>

      {sessionsQuery.isLoading && (
        <p className="text-sm text-slate-400">Загрузка…</p>
      )}
      {sessionsQuery.isError && (
        <div className="rounded-md border border-rose-700/40 bg-rose-950/40 p-3 text-sm text-rose-200">
          Не удалось загрузить смены. Попробуйте обновить страницу.
        </div>
      )}

      {!sessionsQuery.isLoading && sessions.length === 0 && (
        <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-6 text-center text-sm text-slate-400">
          Смен по выбранным фильтрам не найдено.
        </div>
      )}

      {sessions.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-slate-700 bg-slate-900/40">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-800/60 text-xs uppercase tracking-wide text-slate-400">
              <tr>
                <th className="px-4 py-3">#</th>
                {!isDriverOnly && <th className="px-4 py-3">Водитель</th>}
                <th className="px-4 py-3">Начало</th>
                <th className="px-4 py-3">Окончание</th>
                <th className="px-4 py-3">Длительность</th>
                <th className="px-4 py-3">Перерывы</th>
                <th className="px-4 py-3">Статус</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-200">
              {sessions.map((ws) => (
                <tr key={ws.id} className="hover:bg-slate-800/40">
                  <td className="px-4 py-3 font-mono text-slate-400">{ws.id}</td>
                  {!isDriverOnly && (
                    <td className="px-4 py-3 text-slate-300">#{ws.driverId}</td>
                  )}
                  <td className="px-4 py-3">{formatDate(ws.startedAt)}</td>
                  <td className="px-4 py-3">{formatDate(ws.endedAt)}</td>
                  <td className="px-4 py-3 font-mono">
                    {formatDuration(ws.totalMinutes, ws.startedAt, ws.endedAt)}
                  </td>
                  <td className="px-4 py-3">
                    {ws.breaks.length === 0 ? (
                      <span className="text-slate-500">—</span>
                    ) : (
                      <span className="rounded bg-slate-700 px-2 py-0.5 text-xs text-slate-200">
                        {ws.breaks.length}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        'rounded px-2 py-0.5 text-xs font-medium ' +
                        (ws.status === 'OPEN'
                          ? 'bg-emerald-900/60 text-emerald-300'
                          : 'bg-slate-700 text-slate-300')
                      }
                    >
                      {STATUS_LABEL[ws.status]}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
