import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'

import { apiClient } from '../../lib/apiClient'
import type { UserDto } from '../../types/userDto'
import type { AuditLogDto } from '../../types/audit'
import type { SpringPage } from '../../types/page'

const ENTITY_TYPES: { label: string; value: string }[] = [
  { label: 'Все', value: '' },
  { label: 'TRIP', value: 'TRIP' },
  { label: 'WORK_SESSION', value: 'WORK_SESSION' },
  { label: 'USER', value: 'USER' },
]

export function AuditLogsPage() {
  const [userId, setUserId] = useState<number | ''>('')
  const [entityType, setEntityType] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const size = 20

  const usersQuery = useQuery({
    queryKey: ['users', 'all-admin'],
    queryFn: async () => {
      const res = await apiClient.get<SpringPage<UserDto>>('/api/v1/users', { params: { size: 500 } })
      return res.data.content
    },
  })

  const filters = useMemo(
    () => ({
      userId: userId === '' ? undefined : userId,
      entityType: entityType || undefined,
      from: from ? new Date(from).toISOString() : undefined,
      to: to ? new Date(to).toISOString() : undefined,
    }),
    [userId, entityType, from, to],
  )

  const logsQuery = useQuery({
    queryKey: ['audit-logs', filters, page],
    queryFn: async () => {
      const res = await apiClient.get<SpringPage<AuditLogDto>>('/api/v1/audit-logs', {
        params: {
          ...filters,
          page,
          size,
          sort: 'createdAt,desc',
        },
      })
      return res.data
    },
    placeholderData: keepPreviousData,
  })

  const data = logsQuery.data

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-semibold text-white">Журнал аудита</h2>

      <form
        className="flex flex-wrap items-end gap-4 rounded-lg border border-slate-700 bg-slate-900/50 p-4"
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          logsQuery.refetch()
        }}
      >
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">Пользователь</span>
          <select
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={userId === '' ? '' : String(userId)}
            onChange={(e) => setUserId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">Все</option>
            {(usersQuery.data ?? []).map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">Тип сущности</span>
          <select
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
          >
            {ENTITY_TYPES.map((t) => (
              <option key={t.value || 'all'} value={t.value}>
                {t.label}
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
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-slate-400">По</span>
          <input
            type="datetime-local"
            className="rounded-md border border-slate-600 bg-slate-950 px-3 py-2 text-white"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
        <button
          type="submit"
          className="rounded-md bg-sky-600 px-4 py-2 text-sm text-white hover:bg-sky-500"
        >
          Применить
        </button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-slate-700">
        <table className="min-w-full divide-y divide-slate-700 text-sm text-slate-200">
          <thead className="bg-slate-900 text-left text-slate-400">
            <tr>
              <th className="px-3 py-2">Время</th>
              <th className="px-3 py-2">Пользователь</th>
              <th className="px-3 py-2">Действие</th>
              <th className="px-3 py-2">Тип</th>
              <th className="px-3 py-2">ID</th>
              <th className="px-3 py-2">Подробнее</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {(data?.content ?? []).map((row) => (
              <AuditRow key={row.id} row={row} />
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center gap-4 text-sm text-slate-400">
        <button
          type="button"
          disabled={page <= 0}
          className="rounded bg-slate-700 px-3 py-1 disabled:opacity-40"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Назад
        </button>
        <span>
          Стр. {page + 1} / {data ? Math.max(1, data.totalPages) : 1}
        </span>
        <button
          type="button"
          disabled={data ? page >= data.totalPages - 1 : true}
          className="rounded bg-slate-700 px-3 py-1 disabled:opacity-40"
          onClick={() => setPage((p) => p + 1)}
        >
          Вперёд
        </button>
      </div>
    </div>
  )
}

function AuditRow({ row }: { row: AuditLogDto }) {
  const [open, setOpen] = useState(false)
  let pretty = ''
  try {
    pretty = row.payloadJson ? JSON.stringify(JSON.parse(row.payloadJson), null, 2) : ''
  } catch {
    pretty = row.payloadJson ?? ''
  }

  return (
    <>
      <tr>
        <td className="px-3 py-2 whitespace-nowrap">{new Date(row.createdAt).toLocaleString()}</td>
        <td className="px-3 py-2">{row.userFullName ?? '—'}</td>
        <td className="px-3 py-2">{row.action}</td>
        <td className="px-3 py-2">{row.entityType}</td>
        <td className="px-3 py-2">{row.entityId ?? '—'}</td>
        <td className="px-3 py-2">
          <button
            type="button"
            className="text-sky-400 hover:underline"
            onClick={() => setOpen((o) => !o)}
          >
            {open ? 'Скрыть' : 'Подробнее'}
          </button>
        </td>
      </tr>
      {open && (
        <tr className="bg-slate-950/80">
          <td colSpan={6} className="px-3 py-3">
            <pre className="max-h-64 overflow-auto text-xs text-slate-300">{pretty || '—'}</pre>
          </td>
        </tr>
      )}
    </>
  )
}
