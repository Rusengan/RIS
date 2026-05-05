import { NavLink } from 'react-router-dom'

import { useAuthStore } from '../../store/authStore'
import { hasAnyRole, type RoleCode } from '../../types/role'

type NavItem = {
  to: string
  label: string
  /** Empty = visible to any authenticated user */
  roles: RoleCode[]
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', roles: [] },
  { to: '/work', label: 'Смена', roles: ['DRIVER'] },
  { to: '/trips/mine', label: 'Мои рейсы', roles: ['DRIVER'] },
  { to: '/trips', label: 'Рейсы', roles: ['DISPATCHER', 'ADMIN'] },
  { to: '/timesheet', label: 'Табель', roles: ['DISPATCHER', 'ADMIN'] },
  {
    to: '/work-sessions',
    label: 'Work Sessions',
    roles: ['DRIVER', 'DISPATCHER', 'ADMIN'],
  },
  { to: '/vehicles', label: 'Vehicles', roles: ['DISPATCHER', 'ADMIN'] },
  { to: '/users', label: 'Users', roles: ['ADMIN'] },
  { to: '/audit-logs', label: 'Аудит', roles: ['ADMIN'] },
]

function linkClassName({ isActive }: { isActive: boolean }): string {
  return [
    'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive
      ? 'bg-slate-700 text-white'
      : 'text-slate-300 hover:bg-slate-800 hover:text-white',
  ].join(' ')
}

export function Sidebar() {
  const roles = useAuthStore((s) => s.user?.roles ?? [])

  const visible = NAV_ITEMS.filter((item) => hasAnyRole(roles, item.roles))

  return (
    <aside className="flex w-56 shrink-0 flex-col border-r border-slate-800 bg-slate-900">
      <div className="border-b border-slate-800 px-4 py-4">
        <span className="text-sm font-semibold tracking-tight text-white">
          Driver Service
        </span>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 p-3">
        {visible.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.to === '/dashboard'} className={linkClassName}>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
