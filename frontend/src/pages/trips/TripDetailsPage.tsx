import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { RouteMap } from '../../components/Map/RouteMap'
import { apiClient } from '../../lib/apiClient'
import { useAuthStore } from '../../store/authStore'
import type { TripDetailsDto } from '../../types/trip'

export function TripDetailsPage() {
  const { id } = useParams()
  const tripId = Number(id)
  const qc = useQueryClient()
  const roles = useAuthStore((s) => s.user?.roles ?? [])
  const isDriver = roles.includes('DRIVER')
  const isStaff = roles.includes('DISPATCHER') || roles.includes('ADMIN')

  const detailQuery = useQuery({
    queryKey: ['trip', tripId],
    enabled: Number.isFinite(tripId),
    queryFn: async () => {
      const res = await apiClient.get<TripDetailsDto>(`/api/v1/trips/${tripId}`)
      return res.data
    },
  })

  const acceptMut = useMutation({
    mutationFn: () => apiClient.post(`/api/v1/trips/${tripId}/accept`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trip', tripId] })
      qc.invalidateQueries({ queryKey: ['trips'] })
    },
  })

  const completeMut = useMutation({
    mutationFn: () => apiClient.post(`/api/v1/trips/${tripId}/complete`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trip', tripId] })
      qc.invalidateQueries({ queryKey: ['trips'] })
    },
  })

  const cancelMut = useMutation({
    mutationFn: () =>
      apiClient.post(`/api/v1/trips/${tripId}/cancel`, { reason: 'Отменено диспетчером' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trip', tripId] })
      qc.invalidateQueries({ queryKey: ['trips'] })
    },
  })

  const checkInMut = useMutation({
    mutationFn: (routePointId: number) =>
      apiClient.post(`/api/v1/route-points/${routePointId}/check-in`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trip', tripId] })
      qc.invalidateQueries({ queryKey: ['trips'] })
    },
  })

  const calculateMut = useMutation({
    mutationFn: () => apiClient.post(`/api/v1/trips/${tripId}/route/calculate`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trip', tripId] })
      qc.invalidateQueries({ queryKey: ['trips'] })
    },
  })

  const data = detailQuery.data
  const t = data?.trip

  const allVisited =
    data?.routePoints?.every((p) => p.arrivedAt != null) ?? false

  if (detailQuery.isLoading || !t) {
    return <p className="text-slate-400">Загрузка…</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-4">
        <Link to={isDriver ? '/trips/mine' : '/trips'} className="text-sm text-sky-400 hover:underline">
          ← назад
        </Link>
        <h2 className="text-lg font-semibold text-white">
          Рейс #{t.id} · {t.status}
        </h2>
      </div>

      <div className="grid gap-4 rounded-xl border border-slate-700 bg-slate-900/50 p-4 text-sm text-slate-200 md:grid-cols-2">
        <div>
          <p className="text-slate-500">Водитель</p>
          <p>{t.driverFullName}</p>
        </div>
        <div>
          <p className="text-slate-500">ТС</p>
          <p>{t.vehiclePlate}</p>
        </div>
        <div>
          <p className="text-slate-500">План</p>
          <p>{new Date(t.plannedStartAt).toLocaleString()}</p>
        </div>
        <div>
          <p className="text-slate-500">Факт</p>
          <p>
            {t.actualStartAt ? new Date(t.actualStartAt).toLocaleString() : '—'} →{' '}
            {t.actualEndAt ? new Date(t.actualEndAt).toLocaleString() : '—'}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {isDriver && t.status === 'PLANNED' && (
          <button
            type="button"
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm text-white"
            onClick={() => acceptMut.mutate()}
          >
            Принять
          </button>
        )}
        {isDriver && t.status === 'IN_PROGRESS' && allVisited && (
          <button
            type="button"
            className="rounded-md bg-sky-600 px-4 py-2 text-sm text-white"
            onClick={() => completeMut.mutate()}
          >
            Завершить рейс
          </button>
        )}
        {isStaff && (t.status === 'PLANNED' || t.status === 'IN_PROGRESS') && (
          <button
            type="button"
            className="rounded-md bg-rose-700 px-4 py-2 text-sm text-white"
            onClick={() => cancelMut.mutate()}
          >
            Отменить
          </button>
        )}
      </div>

      {!data.route && (
        <button
          type="button"
          className="rounded-md bg-slate-600 px-4 py-2 text-sm text-white hover:bg-slate-500"
          onClick={() => calculateMut.mutate()}
          disabled={calculateMut.isPending}
        >
          Рассчитать маршрут
        </button>
      )}

      {data.route?.encodedPolyline && (
        <div className="overflow-hidden rounded-xl border border-slate-700">
          <RouteMap encodedPolyline={data.route.encodedPolyline} points={data.routePoints} />
        </div>
      )}

      <div>
        <h3 className="mb-2 text-sm font-semibold text-white">Точки</h3>
        <ul className="space-y-2">
          {data.routePoints.map((p) => (
            <li
              key={p.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-200"
            >
              <span>
                #{p.sequenceNo} {p.pointType} · {p.address}{' '}
                {p.arrivedAt ? (
                  <span className="text-emerald-400">(пройдена)</span>
                ) : (
                  <span className="text-amber-300">(не отмечена)</span>
                )}
              </span>
              {isDriver && t.status === 'IN_PROGRESS' && !p.arrivedAt && (
                <button
                  type="button"
                  className="rounded bg-slate-700 px-2 py-1 text-xs text-white"
                  onClick={() => checkInMut.mutate(p.id)}
                >
                  Отметить точку
                </button>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
