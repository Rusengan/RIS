import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'

import { apiClient } from '../../lib/apiClient'
import type { BreakLogDto, BreakType, WorkSessionDto } from '../../types/workSession'

async function fetchCurrent(): Promise<WorkSessionDto | null> {
  const res = await apiClient.get<WorkSessionDto | null>('/api/v1/work-sessions/current', {
    validateStatus: (s) => s === 200 || s === 204,
  })
  if (res.status === 204 || res.data == null) return null
  return res.data
}

export function DriverWorkPage() {
  const qc = useQueryClient()
  const [tick, setTick] = useState(0)

  const { data: session, isLoading } = useQuery({
    queryKey: ['work-sessions', 'current'],
    queryFn: fetchCurrent,
    retry: false,
  })

  useEffect(() => {
    if (session?.status !== 'OPEN') return
    const id = window.setInterval(() => setTick((t) => t + 1), 1000)
    return () => window.clearInterval(id)
  }, [session?.status])

  const elapsedLabel = useMemo(() => {
    if (!session || session.status !== 'OPEN') return null
    const start = new Date(session.startedAt).getTime()
    const sec = Math.max(0, Math.floor((Date.now() - start) / 1000))
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    const s = sec % 60
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }, [session, tick])

  const startMutation = useMutation({
    mutationFn: async () => {
      await apiClient.post('/api/v1/work-sessions/start')
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['work-sessions', 'current'] }),
  })

  const closeMutation = useMutation({
    mutationFn: async (id: number) => {
      await apiClient.post(`/api/v1/work-sessions/${id}/close`)
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['work-sessions', 'current'] }),
  })

  const startBreakMutation = useMutation({
    mutationFn: async ({ sessionId, breakType }: { sessionId: number; breakType: BreakType }) => {
      await apiClient.post(`/api/v1/work-sessions/${sessionId}/breaks/start`, { breakType })
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['work-sessions', 'current'] }),
  })

  const endBreakMutation = useMutation({
    mutationFn: async (breakId: number) => {
      await apiClient.post(`/api/v1/breaks/${breakId}/end`)
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['work-sessions', 'current'] }),
  })

  const breaks = session?.breaks ?? []
  const activeBreak: BreakLogDto | undefined = breaks.find((b) => !b.endedAt)

  if (isLoading) {
    return <p className="text-slate-400">Загрузка…</p>
  }

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-white">Текущая смена</h2>

      <div className="rounded-xl border border-slate-700 bg-slate-900/60 p-6 shadow-lg">
        {!session || session.status === 'CLOSED' ? (
          <div className="flex flex-col gap-4">
            <p className="text-slate-300">Смена не открыта.</p>
            <button
              type="button"
              className="inline-flex max-w-xs justify-center rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
              onClick={() => startMutation.mutate()}
              disabled={startMutation.isPending}
            >
              Начать смену
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-500">Статус</p>
                <p className="text-xl font-semibold text-white">{session.status}</p>
                <p className="mt-1 text-sm text-slate-400">
                  Начало: {new Date(session.startedAt).toLocaleString()}
                </p>
              </div>
              <div className="text-right">
                <p className="text-xs uppercase tracking-wide text-slate-500">Длительность</p>
                <p className="font-mono text-3xl font-semibold text-emerald-400">{elapsedLabel}</p>
              </div>
            </div>
            <button
              type="button"
              className="rounded-md bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-500"
              onClick={() => closeMutation.mutate(session.id)}
              disabled={closeMutation.isPending}
            >
              Закрыть смену
            </button>
          </div>
        )}
      </div>

      {session?.status === 'OPEN' && (
        <div className="rounded-xl border border-slate-700 bg-slate-900/60 p-6">
          <h3 className="mb-4 text-sm font-semibold text-white">Перерывы</h3>
          {activeBreak ? (
            <button
              type="button"
              className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-500"
              onClick={() => endBreakMutation.mutate(activeBreak.id)}
              disabled={endBreakMutation.isPending}
            >
              Завершить перерыв
            </button>
          ) : (
            <div className="flex flex-wrap gap-2">
              {(['SHORT', 'LUNCH', 'MANDATORY'] as BreakType[]).map((bt) => (
                <button
                  key={bt}
                  type="button"
                  className="rounded-md bg-slate-700 px-3 py-2 text-sm text-white hover:bg-slate-600"
                  onClick={() =>
                    startBreakMutation.mutate({ sessionId: session.id, breakType: bt })
                  }
                  disabled={startBreakMutation.isPending}
                >
                  {bt}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
