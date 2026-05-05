import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { z } from 'zod'

import { apiClient } from '../../lib/apiClient'
import { useAuthStore } from '../../store/authStore'
import type { TripDto } from '../../types/trip'
import type { UserDto } from '../../types/userDto'
import type { VehicleDto } from '../../types/vehicle'

const pointSchema = z.object({
  sequenceNo: z.number().int().min(0),
  address: z.string().min(1),
  latitude: z.number(),
  longitude: z.number(),
  type: z.enum(['ORIGIN', 'WAYPOINT', 'DESTINATION']),
})

const wizardSchema = z.object({
  driverId: z.number(),
  vehicleId: z.number(),
  plannedStartAt: z.string().min(1),
  points: z.array(pointSchema).min(2),
})

type WizardForm = z.infer<typeof wizardSchema>

export function DispatcherTripsPage() {
  const qc = useQueryClient()
  const user = useAuthStore((s) => s.user)
  const [step, setStep] = useState<1 | 2>(1)

  const driversQuery = useQuery({
    queryKey: ['users', 'DRIVER'],
    queryFn: async () => {
      const res = await apiClient.get<{ content: UserDto[] }>('/api/v1/users', {
        params: { role: 'DRIVER', size: 500 },
      })
      return res.data.content
    },
  })

  const vehiclesQuery = useQuery({
    queryKey: ['vehicles'],
    queryFn: async () => {
      const res = await apiClient.get<{ content: VehicleDto[] }>('/api/v1/vehicles', {
        params: { size: 500 },
      })
      return res.data.content
    },
  })

  const tripsQuery = useQuery({
    queryKey: ['trips', 'list'],
    queryFn: async () => {
      const res = await apiClient.get<{ content: TripDto[] }>('/api/v1/trips', {
        params: { size: 50, sort: 'plannedStartAt,desc' },
      })
      return res.data.content
    },
  })

  const form = useForm<WizardForm>({
    resolver: zodResolver(wizardSchema),
    defaultValues: {
      points: [
        { sequenceNo: 1, address: '', latitude: 55.75, longitude: 37.62, type: 'ORIGIN' },
        { sequenceNo: 2, address: '', latitude: 55.76, longitude: 37.63, type: 'DESTINATION' },
      ],
    },
  })

  const { fields, append, remove } = useFieldArray({ control: form.control, name: 'points' })

  const createTrip = useMutation({
    mutationFn: async (payload: WizardForm) => {
      const dispatcherId = user?.id
      if (!dispatcherId) throw new Error('No user')
      const planned = new Date(payload.plannedStartAt).toISOString()
      const body = {
        driverId: payload.driverId,
        vehicleId: payload.vehicleId,
        dispatcherId,
        plannedStartAt: planned,
        points: payload.points.map((p) => ({
          sequenceNo: p.sequenceNo,
          address: p.address,
          latitude: p.latitude,
          longitude: p.longitude,
          type: p.type,
        })),
      }
      const res = await apiClient.post<TripDto>('/api/v1/trips', body)
      const trip = res.data
      await apiClient.post(`/api/v1/trips/${trip.id}/route/calculate`)
      return trip
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['trips'] })
      setStep(1)
      form.reset({
        points: [
          { sequenceNo: 1, address: '', latitude: 55.75, longitude: 37.62, type: 'ORIGIN' },
          { sequenceNo: 2, address: '', latitude: 55.76, longitude: 37.63, type: 'DESTINATION' },
        ],
      })
    },
  })

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-white">Рейсы</h2>
        <button
          type="button"
          className="rounded-md bg-emerald-600 px-4 py-2 text-sm text-white hover:bg-emerald-500"
          onClick={() => setStep(step === 1 ? 2 : 1)}
        >
          {step === 1 ? 'Создать рейс' : 'Назад к таблице'}
        </button>
      </div>

      {step === 1 && (
        <div className="overflow-x-auto rounded-lg border border-slate-700">
          <table className="min-w-full divide-y divide-slate-700 text-sm">
            <thead className="bg-slate-900 text-left text-slate-400">
              <tr>
                <th className="px-3 py-2">ID</th>
                <th className="px-3 py-2">Водитель</th>
                <th className="px-3 py-2">ТС</th>
                <th className="px-3 py-2">Статус</th>
                <th className="px-3 py-2">План</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-200">
              {(tripsQuery.data ?? []).map((t) => (
                <tr key={t.id}>
                  <td className="px-3 py-2">{t.id}</td>
                  <td className="px-3 py-2">{t.driverFullName}</td>
                  <td className="px-3 py-2">{t.vehiclePlate}</td>
                  <td className="px-3 py-2">{t.status}</td>
                  <td className="px-3 py-2">{new Date(t.plannedStartAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {step === 2 && (
        <form
          className="space-y-6 rounded-xl border border-slate-700 bg-slate-900/50 p-6"
          onSubmit={form.handleSubmit((v) => createTrip.mutate(v))}
        >
          <p className="text-xs text-slate-500">
            Укажите адрес и координаты точек (или подставьте координаты после поиска адреса внешним способом).
          </p>
          <div className="grid gap-4 md:grid-cols-3">
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-slate-400">Водитель</span>
              <select
                className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
                {...form.register('driverId', { valueAsNumber: true })}
                required
              >
                <option value="">—</option>
                {(driversQuery.data ?? []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.fullName}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-slate-400">ТС</span>
              <select
                className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
                {...form.register('vehicleId', { valueAsNumber: true })}
                required
              >
                <option value="">—</option>
                {(vehiclesQuery.data ?? []).map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.plateNumber}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-slate-400">План старта</span>
              <input
                type="datetime-local"
                className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
                {...form.register('plannedStartAt')}
                required
              />
            </label>
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">Точки маршрута</h3>
              <button
                type="button"
                className="text-sm text-sky-400 hover:underline"
                onClick={() =>
                  append({
                    sequenceNo: fields.length + 1,
                    address: '',
                    latitude: 0,
                    longitude: 0,
                    type: 'WAYPOINT',
                  })
                }
              >
                + точка
              </button>
            </div>

            {fields.map((field, index) => (
              <div
                key={field.id}
                className="grid gap-3 rounded-lg border border-slate-700 p-4 md:grid-cols-12 md:items-end"
              >
                <label className="md:col-span-3">
                  <span className="text-xs text-slate-500">Тип</span>
                  <select
                    className="mt-1 w-full rounded-md border border-slate-600 bg-slate-950 px-2 py-2 text-sm text-white"
                    {...form.register(`points.${index}.type`)}
                  >
                    <option value="ORIGIN">ORIGIN</option>
                    <option value="WAYPOINT">WAYPOINT</option>
                    <option value="DESTINATION">DESTINATION</option>
                  </select>
                </label>
                <label className="md:col-span-5">
                  <span className="text-xs text-slate-500">Адрес</span>
                  <input
                    className="mt-1 w-full rounded-md border border-slate-600 bg-slate-950 px-2 py-2 text-sm text-white"
                    {...form.register(`points.${index}.address`)}
                  />
                </label>
                <label className="md:col-span-2">
                  <span className="text-xs text-slate-500">Широта</span>
                  <input
                    type="number"
                    step="any"
                    className="mt-1 w-full rounded-md border border-slate-600 bg-slate-950 px-2 py-2 text-sm text-white"
                    {...form.register(`points.${index}.latitude`, { valueAsNumber: true })}
                  />
                </label>
                <label className="md:col-span-2">
                  <span className="text-xs text-slate-500">Долгота</span>
                  <input
                    type="number"
                    step="any"
                    className="mt-1 w-full rounded-md border border-slate-600 bg-slate-950 px-2 py-2 text-sm text-white"
                    {...form.register(`points.${index}.longitude`, { valueAsNumber: true })}
                  />
                </label>
                {fields.length > 2 && (
                  <button
                    type="button"
                    className="text-rose-400 md:col-span-12"
                    onClick={() => remove(index)}
                  >
                    Удалить
                  </button>
                )}
              </div>
            ))}
          </div>

          <button
            type="submit"
            disabled={createTrip.isPending}
            className="rounded-md bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-500"
          >
            Сохранить и рассчитать маршрут
          </button>
        </form>
      )}
    </div>
  )
}
