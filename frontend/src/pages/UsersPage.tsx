import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'

import { apiClient } from '../lib/apiClient'
import type { SpringPage } from '../types/page'
import type { RoleCode } from '../types/role'
import type { UserDto } from '../types/userDto'
import {
  type UserCreateValues,
  type UserUpdateValues,
  roleCodes,
  userCreateSchema,
  userUpdateSchema,
} from './users/userFormSchema'

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const

const ROLE_LABELS: Record<RoleCode, string> = {
  DRIVER: 'Водитель',
  DISPATCHER: 'Диспетчер',
  ADMIN: 'Администратор',
}

const ENABLED_LABELS: Record<'true' | 'false', string> = {
  true: 'Активен',
  false: 'Заблокирован',
}

function emptyCreate(): UserCreateValues {
  return { email: '', fullName: '', roles: ['DRIVER'] }
}

export function UsersPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [roleFilter, setRoleFilter] = useState<RoleCode | ''>('')
  const [enabledFilter, setEnabledFilter] = useState<'' | 'true' | 'false'>('')

  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<UserDto | null>(null)
  const [rolesEditing, setRolesEditing] = useState<UserDto | null>(null)
  const [pendingRole, setPendingRole] = useState<RoleCode>('DRIVER')

  const roleParam = roleFilter === '' ? undefined : roleFilter
  const enabledParam = enabledFilter === '' ? undefined : enabledFilter === 'true'

  const queryKey = useMemo(
    () => ['users', page, size, roleParam ?? '', enabledFilter] as const,
    [page, size, roleParam, enabledFilter],
  )

  const usersQuery = useQuery({
    queryKey,
    queryFn: async () => {
      const { data } = await apiClient.get<SpringPage<UserDto>>('/api/v1/users', {
        params: {
          page,
          size,
          ...(roleParam !== undefined && { role: roleParam }),
          ...(enabledParam !== undefined && { enabled: enabledParam }),
        },
      })
      return data
    },
  })

  const createMutation = useMutation({
    mutationFn: async (body: Record<string, unknown>) => {
      await apiClient.post('/api/v1/users', body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setCreateOpen(false)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, body }: { id: number; body: Record<string, unknown> }) => {
      await apiClient.put(`/api/v1/users/${id}`, body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setEditing(null)
    },
  })

  const addRoleMutation = useMutation({
    mutationFn: async ({ id, role }: { id: number; role: RoleCode }) => {
      await apiClient.post(`/api/v1/users/${id}/roles`, { role })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })

  const createForm = useForm<UserCreateValues>({
    resolver: zodResolver(userCreateSchema),
    defaultValues: emptyCreate(),
  })

  const editForm = useForm<UserUpdateValues>({
    resolver: zodResolver(userUpdateSchema),
    defaultValues: {
      email: '',
      fullName: '',
      enabled: true,
      pictureUrl: undefined,
    },
  })

  function openCreate() {
    createForm.reset(emptyCreate())
    setCreateOpen(true)
  }

  function openEdit(row: UserDto) {
    setEditing(row)
    editForm.reset({
      email: row.email,
      fullName: row.fullName,
      enabled: row.enabled,
      pictureUrl: row.pictureUrl ?? undefined,
    })
  }

  function openRoles(row: UserDto) {
    setRolesEditing(row)
    setPendingRole('DRIVER')
  }

  function onCreateSubmit(values: UserCreateValues) {
    createMutation.mutate({
      email: values.email.trim(),
      fullName: values.fullName.trim(),
      roles: values.roles,
    })
  }

  function onEditSubmit(values: UserUpdateValues) {
    if (!editing) return
    updateMutation.mutate({
      id: editing.id,
      body: {
        email: values.email.trim(),
        fullName: values.fullName.trim(),
        enabled: values.enabled,
        pictureUrl: values.pictureUrl ?? null,
      },
    })
  }

  const pageData = usersQuery.data
  const isCreating = createMutation.isPending
  const isUpdating = updateMutation.isPending
  const isAddingRole = addRoleMutation.isPending
  const mutationError =
    createMutation.error ?? updateMutation.error ?? addRoleMutation.error

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-white">Пользователи</h2>
          <p className="mt-1 text-sm text-slate-400">
            Управление учётными записями и ролями.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            Роль
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value as RoleCode | '')
                setPage(0)
              }}
              className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              <option value="">Все</option>
              {roleCodes.map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABELS[r]}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs text-slate-400">
            Статус
            <select
              value={enabledFilter}
              onChange={(e) => {
                setEnabledFilter(e.target.value as '' | 'true' | 'false')
                setPage(0)
              }}
              className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white"
            >
              <option value="">Все</option>
              <option value="true">{ENABLED_LABELS.true}</option>
              <option value="false">{ENABLED_LABELS.false}</option>
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
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">ФИО</th>
              <th className="px-4 py-3 font-medium">Роли</th>
              <th className="px-4 py-3 font-medium">Статус</th>
              <th className="px-4 py-3 font-medium text-right">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800 bg-slate-950 text-slate-200">
            {usersQuery.isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                  Загрузка…
                </td>
              </tr>
            )}
            {usersQuery.isError && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-red-400">
                  Не удалось загрузить список.
                </td>
              </tr>
            )}
            {pageData?.content.length === 0 && !usersQuery.isLoading && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-slate-500">
                  Нет записей.
                </td>
              </tr>
            )}
            {pageData?.content.map((u) => (
              <tr key={u.id} className="hover:bg-slate-900/50">
                <td className="px-4 py-3 font-mono">{u.email}</td>
                <td className="px-4 py-3">{u.fullName}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {u.roles.map((r) => (
                      <span
                        key={r}
                        className="rounded bg-slate-800 px-2 py-0.5 text-xs text-slate-200"
                      >
                        {ROLE_LABELS[r]}
                      </span>
                    ))}
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span
                    className={
                      u.enabled
                        ? 'rounded bg-emerald-500/20 px-2 py-0.5 text-xs text-emerald-300'
                        : 'rounded bg-red-500/20 px-2 py-0.5 text-xs text-red-300'
                    }
                  >
                    {u.enabled ? 'Активен' : 'Заблокирован'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => openEdit(u)}
                    className="mr-2 text-sm text-sky-400 hover:text-sky-300"
                  >
                    Изменить
                  </button>
                  <button
                    type="button"
                    onClick={() => openRoles(u)}
                    className="text-sm text-amber-400 hover:text-amber-300"
                  >
                    Роли
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

      {/* === Модалка: создание === */}
      {createOpen && (
        <Modal title="Новый пользователь" onClose={() => setCreateOpen(false)}>
          <form
            className="mt-4 space-y-3"
            onSubmit={createForm.handleSubmit(onCreateSubmit)}
            noValidate
          >
            <Field label="Email" error={createForm.formState.errors.email?.message}>
              <input
                {...createForm.register('email')}
                type="email"
                autoComplete="off"
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                placeholder="user@example.com"
              />
            </Field>
            <Field label="ФИО" error={createForm.formState.errors.fullName?.message}>
              <input
                {...createForm.register('fullName')}
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                placeholder="Иванов Иван Иванович"
              />
            </Field>
            <Field
              label="Роли"
              error={createForm.formState.errors.roles?.message}
            >
              <div className="flex flex-col gap-1">
                {roleCodes.map((r) => (
                  <label
                    key={r}
                    className="flex items-center gap-2 text-sm text-slate-200"
                  >
                    <input
                      type="checkbox"
                      value={r}
                      {...createForm.register('roles')}
                      className="rounded border-slate-700 bg-slate-950"
                    />
                    {ROLE_LABELS[r]}
                  </label>
                ))}
              </div>
            </Field>
            <ModalActions
              onCancel={() => setCreateOpen(false)}
              isSaving={isCreating}
            />
          </form>
        </Modal>
      )}

      {/* === Модалка: редактирование === */}
      {editing && (
        <Modal title="Редактировать пользователя" onClose={() => setEditing(null)}>
          <form
            className="mt-4 space-y-3"
            onSubmit={editForm.handleSubmit(onEditSubmit)}
            noValidate
          >
            <Field label="Email" error={editForm.formState.errors.email?.message}>
              <input
                {...editForm.register('email')}
                type="email"
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
              />
            </Field>
            <Field label="ФИО" error={editForm.formState.errors.fullName?.message}>
              <input
                {...editForm.register('fullName')}
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
              />
            </Field>
            <Field
              label="URL аватара"
              error={editForm.formState.errors.pictureUrl?.message}
            >
              <input
                {...editForm.register('pictureUrl')}
                className="w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                placeholder="https://…"
              />
            </Field>
            <label className="flex items-center gap-2 text-sm text-slate-200">
              <input
                type="checkbox"
                {...editForm.register('enabled')}
                className="rounded border-slate-700 bg-slate-950"
              />
              Учётная запись активна
            </label>
            <ModalActions
              onCancel={() => setEditing(null)}
              isSaving={isUpdating}
            />
          </form>
        </Modal>
      )}

      {/* === Модалка: роли === */}
      {rolesEditing && (
        <Modal
          title={`Роли: ${rolesEditing.fullName}`}
          onClose={() => setRolesEditing(null)}
        >
          <div className="mt-4 space-y-3">
            <div>
              <p className="mb-1 text-xs text-slate-400">Текущие роли</p>
              <div className="flex flex-wrap gap-1">
                {rolesEditing.roles.length === 0 ? (
                  <span className="text-sm text-slate-500">Нет ролей</span>
                ) : (
                  rolesEditing.roles.map((r) => (
                    <span
                      key={r}
                      className="rounded bg-slate-800 px-2 py-0.5 text-xs text-slate-200"
                    >
                      {ROLE_LABELS[r]}
                    </span>
                  ))
                )}
              </div>
            </div>
            <div className="flex items-end gap-2">
              <label className="flex flex-1 flex-col gap-1 text-xs text-slate-400">
                Добавить роль
                <select
                  value={pendingRole}
                  onChange={(e) => setPendingRole(e.target.value as RoleCode)}
                  className="rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
                >
                  {roleCodes.map((r) => (
                    <option key={r} value={r}>
                      {ROLE_LABELS[r]}
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                disabled={
                  isAddingRole || rolesEditing.roles.includes(pendingRole)
                }
                onClick={() =>
                  addRoleMutation.mutate(
                    { id: rolesEditing.id, role: pendingRole },
                    {
                      onSuccess: () =>
                        setRolesEditing((cur) =>
                          cur
                            ? {
                                ...cur,
                                roles: cur.roles.includes(pendingRole)
                                  ? cur.roles
                                  : [...cur.roles, pendingRole],
                              }
                            : cur,
                        ),
                    },
                  )
                }
                className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500 disabled:opacity-50"
              >
                Добавить
              </button>
            </div>
            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setRolesEditing(null)}
                className="rounded-md border border-slate-600 px-4 py-2 text-sm text-slate-200 hover:bg-slate-800"
              >
                Закрыть
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

function Modal({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-semibold text-white">{title}</h3>
        {children}
      </div>
    </div>
  )
}

function ModalActions({
  onCancel,
  isSaving,
}: {
  onCancel: () => void
  isSaving: boolean
}) {
  return (
    <div className="flex justify-end gap-2 pt-2">
      <button
        type="button"
        onClick={onCancel}
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
