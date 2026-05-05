import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'

import { apiClient } from '../lib/apiClient'
import type { SpringPage } from '../types/page'
import type { VehicleDto, VehicleStatus } from '../types/vehicle'
import {
  type VehicleFormValues,
  vehicleFormSchema,
  vehicleStatuses,
} from './vehicles/vehicleFormSchema'

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

const STATUS_LABELS: Record<VehicleStatus, string> = {
  ACTIVE: 'В работе',
  MAINTENANCE: 'На обслуживании',
  RETIRED: 'Списан',
}

function emptyDefaults(): VehicleFormValues {
  return {
    plateNumber: '',
    brand: '',
    model: '',
    capacityKg: undefined,
    status: 'ACTIVE',
  }
}

export function VehiclesPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [statusFilter, setStatusFilter] = useState<VehicleStatus | ''>('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<VehicleDto | null>(null)

  const statusParam = statusFilter === '' ? undefined : statusFilter

  const queryKey = useMemo(
    () => ['vehicles', page, size, statusParam ?? ''] as const,
    [page, size, statusParam],
  )

  const vehiclesQuery = useQuery({
    queryKey,
    queryFn: async () => {
      const { data } = await apiClient.get<SpringPage<VehicleDto>>('/api/v1/vehicles', {
        params: {
          page,
          size,
          ...(statusParam !== undefined && { status: statusParam }),
        },
      })
      return data
    },
  })

  const createMutation = useMutation({
    mutationFn: async (body: Record<string, unknown>) => {
      await apiClient.post('/api/v1/vehicles', body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
      setModalOpen(false)
      setEditing(null)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, body }: { id: number; body: Record<string, unknown> }) => {
      await apiClient.put(`/api/v1/vehicles/${id}`, body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
      setModalOpen(false)
      setEditing(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await apiClient.delete(`/api/v1/vehicles/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
  })

  const form = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleFormSchema),
    defaultValues: emptyDefaults(),
  })

  function openCreate() {
    setEditing(null)
    form.reset(emptyDefaults())
    setModalOpen(true)
  }

  function openEdit(row: VehicleDto) {
    setEditing(row)
    form.reset({
      plateNumber: row.plateNumber,
      brand: row.brand,
      model: row.model,
      capacityKg: row.capacityKg ?? undefined,
      status: row.status,
    })
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditing(null)
    form.reset(emptyDefaults())
  }

  function buildCreateBody(values: VehicleFormValues): Record<string, unknown> {
    const body: Record<string, unknown> = {
      plateNumber: values.plateNumber.trim(),
      brand: values.brand.trim(),
      model: values.model.trim(),
      status: values.status,
    }
    if (values.capacityKg !== undefined) {
      body.capacityKg = values.capacityKg
    }
    return body
  }

  function buildUpdateBody(values: VehicleFormValues): Record<string, unknown> {
    return {
      plateNumber: values.plateNumber.trim(),
      brand: values.brand.trim(),
      model: values.model.trim(),
      capacityKg: values.capacityKg ?? null,
      status: values.status,
    }
  }

  function onSubmit(values: VehicleFormValues) {
    if (editing) {
      updateMutation.mutate({ id: editing.id, body: buildUpdateBody(values) })
    } else {
      createMutation.mutate(buildCreateBody(values))
    }
  }

  const pageData = vehiclesQuery.data
  const isSaving = createMutation.isPending || updateMutation.isPending
  const mutationError =
    createMutation.error ?? updateMutation.error ?? deleteMutation.error

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-white">Транспорт</h2>
          <p className="mt-1 text-sm text-slate-400">Список ТС и управление.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            Статус
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as VehicleStatus | '')
                setPage(0)
              }}
              className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              <option value="">Все</option>
              {vehicleStatuses.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            На странице
            <select
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value))
                setPage(0)
              }}
              className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            onClick={openCreate}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
          >
            Создать
          </button>
        </div>
      </div>

      {mutationError && (
        <p className="text-sm text-red-400" role="alert">
          Ошибка запроса. Проверьте данные и права доступа.
        </p>
      )}

      <div className="overflow-x-auto rounded-lg border border-slate-800">
        <table className="min-w-full divide-y divide-slate-800 text-left text-sm">
          <thead className="bg-slate-900/80 text-slate-300">
            <tr>
              <th className="px-4 py-3 font-medium">Госномер</th>
              <th className="px-4 py-3 font-medium">Марка</th>
              <th className="px-4 py-3 font-medium">Модель</th>
              <th className="px-4 py-3 font-medium">Грузоподъёмность, кг</th>
              <th className="px-4 py-3 font-medium">Статус</th>
              <th className="px-4 py-3 font-medium text-right">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800 bg-slate-950 text-slate-200">
            {vehiclesQuery.isLoading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                  Загрузка…
                </td>
              </tr>
            )}
            {vehiclesQuery.isError && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-red-400">
                  Не удалось загрузить список.
                </td>
              </tr>
            )}
            {pageData?.content.length === 0 && !vehiclesQuery.isLoading && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                  Нет записей.
                </td>
              </tr>
            )}
            {pageData?.content.map((v) => (
              <tr key={v.id} className="hover:bg-slate-900/50">
                <td className="px-4 py-3 font-mono">{v.plateNumber}</td>
                <td className="px-4 py-3">{v.brand}</td>
                <td className="px-4 py-3">{v.model}</td>
                <td className="px-4 py-3">{v.capacityKg ?? '—'}</td>
                <td className="px-4 py-3">{STATUS_LABELS[v.status]}</td>
                <td className="px-4 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => openEdit(v)}
                    className="mr-2 text-sm text-sky-400 hover:text-sky-300"
                  >
                    Изменить
                  </button>
                  <button
                    type="button"
                    disabled={deleteMutation.isPending}
                    onClick={() => {
                      if (window.confirm(`Удалить ТС ${v.plateNumber}?`)) {
                        deleteMutation.mutate(v.id)
                      }
                    }}
                    className="text-sm text-red-400 hover:text-red-300 disabled:opacity-50"
                  >
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {pageData !== undefined && pageData.totalPages > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-400">
          <span>
            Страница {pageData.number + 1} из {pageData.totalPages} (
            {pageData.totalElements} всего)
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="rounded-md border border-slate-700 px-3 py-1 text-white hover:bg-slate-800 disabled:opacity-40"
            >
              Назад
            </button>
            <button
              type="button"
              disabled={page >= pageData.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-md border border-slate-700 px-3 py-1 text-white hover:bg-slate-800 disabled:opacity-40"
            >
              Вперёд
            </button>
          </div>
        </div>
      )}

      {modalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-white">
              {editing ? 'Редактировать ТС' : 'Новое ТС'}
            </h3>
            <form
              className="mt-4 space-y-3"
              onSubmit={form.handleSubmit(onSubmit)}
              noValidate
            >
              <Field label="Госномер" error={form.formState.errors.plateNumber?.message}>
                <input
                  {...form.register('plateNumber')}
                  autoComplete="off"
                  className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-sm uppercase text-white placeholder:text-slate-600"
                  placeholder="A123BC77"
                />
              </Field>
              <Field label="Марка" error={form.formState.errors.brand?.message}>
                <input
                  {...form.register('brand')}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </Field>
              <Field label="Модель" error={form.formState.errors.model?.message}>
                <input
                  {...form.register('model')}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                />
              </Field>
              <Field label="Грузоподъёмность (кг)" error={form.formState.errors.capacityKg?.message}>
                <input
                  type="number"
                  min={0}
                  step={1}
                  {...form.register('capacityKg')}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                  placeholder="Необязательно"
                />
              </Field>
              <Field label="Статус" error={form.formState.errors.status?.message}>
                <select
                  {...form.register('status')}
                  className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                >
                  {vehicleStatuses.map((s) => (
                    <option key={s} value={s}>
                      {STATUS_LABELS[s]}
                    </option>
                  ))}
                </select>
              </Field>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={closeModal}
                  className="rounded-md border border-slate-600 px-4 py-2 text-sm text-slate-200 hover:bg-slate-800"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
                >
                  {isSaving ? 'Сохранение…' : 'Сохранить'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: ReactNode
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs text-slate-400">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
    </label>
  )
}
