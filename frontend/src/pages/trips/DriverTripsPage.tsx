import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { apiClient } from '../../lib/apiClient'
import type { TripDto } from '../../types/trip'

export function DriverTripsPage() {
  const qc = useQueryClient()

  const tripsQuery = useQuery({
    queryKey: ['trips', 'mine'],
    queryFn: async () => {
      const res = await apiClient.get<{ content: TripDto[] }>('/api/v1/trips/mine', {
        params: { size: 50, sort: 'plannedStartAt,desc' },
      })
      return res.data.content
    },
  })

  const acceptMut = useMutation({
    mutationFn: (tripId: number) => apiClient.post(`/api/v1/trips/${tripId}/accept`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trips'] })
      qc.invalidateQueries({ queryKey: ['trips', 'mine'] })
    },
  })

  const trips = tripsQuery.data ?? []

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-white">Мои рейсы</h2>
      <div className="grid gap-4 md:grid-cols-2">
        {trips.map((t) => {
          return (
            <div
              key={t.id}
              className="rounded-xl border border-slate-700 bg-slate-900/50 p-4 shadow"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="rounded bg-slate-700 px-2 py-0.5 text-xs uppercase text-slate-200">
                  {t.status}
                </span>
                <span className="text-xs text-slate-500">
                  {new Date(t.plannedStartAt).toLocaleString()}
                </span>
              </div>
              <p className="mt-2 text-sm text-slate-400">{t.vehiclePlate}</p>
              <p className="mt-1 text-sm text-slate-300">Рейс #{t.id}</p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link
                  to={`/trips/${t.id}`}
                  className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500"
                >
                  Открыть
                </Link>
                {t.status === 'PLANNED' && (
                  <button
                    type="button"
                    className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm text-white hover:bg-emerald-500"
                    onClick={() => acceptMut.mutate(t.id)}
                  >
                    Принять
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
      {trips.length === 0 && <p className="text-slate-500">Рейсов пока нет.</p>}
    </div>
  )
}
