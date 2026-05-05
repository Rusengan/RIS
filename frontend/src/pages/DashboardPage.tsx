import { keepPreviousData, useQueries } from '@tanstack/react-query'
import { useMemo } from 'react'

import { apiClient } from '../lib/apiClient'
import { useAuthStore } from '../store/authStore'
import type { AuditLogDto } from '../types/audit'
import type { TripDto } from '../types/trip'
import type { BreakLogDto, WorkSessionDto } from '../types/workSession'
import type { SpringPage } from '../types/page'
import type { UserDto } from '../types/userDto'
import type { VehicleDto } from '../types/vehicle'
import { Link } from 'react-router-dom'

export function DashboardPage() {
  const roles = useAuthStore((s) => s.user?.roles ?? [])
  const isDriver = roles.includes('DRIVER')
  const isDispatcher = roles.includes('DISPATCHER') || roles.includes('ADMIN')
  const isAdmin = roles.includes('ADMIN')

  const dayRange = useMemo(() => {
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    const end = new Date()
    end.setHours(23, 59, 59, 999)
    return { from: start.toISOString(), to: end.toISOString() }
  }, [])

  const results = useQueries({
    queries: [
      {
        queryKey: ['work-sessions', 'current', 'dashboard'],
        enabled: isDriver,
        queryFn: async () => {
          const res = await apiClient.get<WorkSessionDto | null>('/api/v1/work-sessions/current', {
            validateStatus: (s) => s === 200 || s === 204,
          })
          if (res.status === 204) return null
          return res.data
        },
      },
      {
        queryKey: ['trips', 'mine', 'today', dayRange.from],
        enabled: isDriver,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<TripDto>>('/api/v1/trips/mine', {
            params: {
              from: dayRange.from,
              to: dayRange.to,
              size: 30,
              sort: 'plannedStartAt,desc',
            },
          })
          return res.data
        },
      },
      {
        queryKey: ['trips', 'in-progress-count'],
        enabled: isDispatcher,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<TripDto>>('/api/v1/trips', {
            params: { status: 'IN_PROGRESS', size: 1 },
          })
          return res.data.totalElements
        },
      },
      {
        queryKey: ['work-sessions', 'open-count'],
        enabled: isDispatcher,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<WorkSessionDto>>('/api/v1/work-sessions', {
            params: { status: 'OPEN', size: 1 },
          })
          return res.data.totalElements
        },
      },
      {
        queryKey: ['trips', 'dispatcher-recent'],
        enabled: isDispatcher,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<TripDto>>('/api/v1/trips', {
            params: { size: 10, sort: 'plannedStartAt,desc' },
          })
          return res.data.content
        },
      },
      {
        queryKey: ['users', 'count'],
        enabled: isAdmin,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<UserDto>>('/api/v1/users', { params: { size: 1 } })
          return res.data.totalElements
        },
      },
      {
        queryKey: ['vehicles', 'count'],
        enabled: isAdmin,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<VehicleDto>>('/api/v1/vehicles', { params: { size: 1 } })
          return res.data.totalElements
        },
      },
      {
        queryKey: ['trips', 'count'],
        enabled: isAdmin,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<TripDto>>('/api/v1/trips', { params: { size: 1 } })
          return res.data.totalElements
        },
      },
      {
        queryKey: ['audit-logs', 'recent'],
        enabled: isAdmin,
        queryFn: async () => {
          const res = await apiClient.get<SpringPage<AuditLogDto>>('/api/v1/audit-logs', {
            params: { size: 10, sort: 'createdAt,desc' },
          })
          return res.data.content
        },
        placeholderData: keepPreviousData,
      },
    ],
  })

  const currentSession = results[0].data as WorkSessionDto | null | undefined
  const todayTrips = results[1].data as SpringPage<TripDto> | undefined
  const inProgCount = results[2].data as number | undefined
  const openSessionsCount = results[3].data as number | undefined
  const recentTripsDispatcher = results[4].data as TripDto[] | undefined
  const usersCount = results[5].data as number | undefined
  const vehiclesCount = results[6].data as number | undefined
  const tripsCount = results[7].data as number | undefined
  const auditRecent = results[8].data as AuditLogDto[] | undefined

  const loading = results.some((r) => r.isLoading)

  if (loading) {
    return <p className="text-slate-400">Загрузка…</p>
  }

  return (
    <div className="space-y-8">
      <h2 className="text-lg font-semibold text-white">Dashboard</h2>

      {isDriver && (
        <div className="grid gap-6 md:grid-cols-2">
          <section className="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
            <h3 className="text-sm font-semibold text-slate-300">Текущая смена</h3>
            {currentSession && currentSession.status === 'OPEN' ? (
              <div className="mt-2 text-sm text-slate-200">
                <p>С {new Date(currentSession.startedAt).toLocaleString()}</p>
                <p className="mt-1 text-slate-400">
                  Перерывов:{' '}
                  {currentSession.breaks?.filter((b: BreakLogDto) => b.endedAt).length ?? 0} завершено
                </p>
              </div>
            ) : (
              <p className="mt-2 text-sm text-slate-500">Нет открытой смены</p>
            )}
            <Link className="mt-3 inline-block text-sky-400 hover:underline" to="/work">
              Открыть смену
            </Link>
          </section>
          <section className="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
            <h3 className="text-sm font-semibold text-slate-300">Рейсы сегодня</h3>
            <ul className="mt-2 space-y-2 text-sm text-slate-200">
              {(todayTrips?.content ?? []).map((t) => (
                <li key={t.id}>
                  <Link className="text-sky-400 hover:underline" to={`/trips/${t.id}`}>
                    #{t.id} · {t.status} · {t.vehiclePlate}
                  </Link>
                </li>
              ))}
              {(todayTrips?.content ?? []).length === 0 && (
                <li className="text-slate-500">Нет рейсов за сегодня</li>
              )}
            </ul>
          </section>
        </div>
      )}

      {isDispatcher && !isAdmin && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-4">
            <div className="rounded-lg border border-slate-700 bg-slate-900/50 px-4 py-3">
              <p className="text-xs text-slate-500">IN_PROGRESS</p>
              <p className="text-2xl font-semibold text-white">{inProgCount ?? '—'}</p>
            </div>
            <div className="rounded-lg border border-slate-700 bg-slate-900/50 px-4 py-3">
              <p className="text-xs text-slate-500">OPEN смен</p>
              <p className="text-2xl font-semibold text-white">{openSessionsCount ?? '—'}</p>
            </div>
          </div>
          <section className="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
            <h3 className="text-sm font-semibold text-slate-300">Последние рейсы</h3>
            <ul className="mt-2 space-y-2 text-sm">
              {(recentTripsDispatcher ?? []).map((t) => (
                <li key={t.id}>
                  <Link className="text-sky-400" to={`/trips/${t.id}`}>
                    #{t.id} {t.driverFullName} · {t.status}
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        </div>
      )}

      {isAdmin && (
        <div className="space-y-6">
          <div className="flex flex-wrap gap-4">
            <Stat label="Пользователи" value={usersCount} />
            <Stat label="ТС" value={vehiclesCount} />
            <Stat label="Рейсы (всего)" value={tripsCount} />
          </div>
          <section className="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
            <h3 className="text-sm font-semibold text-slate-300">Последние аудит-записи</h3>
            <ul className="mt-2 space-y-2 text-xs text-slate-300">
              {(auditRecent ?? []).map((a) => (
                <li key={a.id}>
                  {new Date(a.createdAt).toLocaleString()} · {a.action} · {a.entityType} #{a.entityId}
                </li>
              ))}
            </ul>
            <Link className="mt-3 inline-block text-sky-400 hover:underline" to="/audit-logs">
              Все записи
            </Link>
          </section>
        </div>
      )}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number | undefined }) {
  return (
    <div className="rounded-lg border border-slate-700 bg-slate-900/50 px-4 py-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-2xl font-semibold text-white">{value ?? '—'}</p>
    </div>
  )
}
