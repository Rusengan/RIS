import { createBrowserRouter, Navigate } from 'react-router-dom'

import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { AppLayout } from './components/layout/AppLayout'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { AuditLogsPage } from './pages/audit/AuditLogsPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { TimesheetPage } from './pages/timesheet/TimesheetPage'
import { UsersPage } from './pages/UsersPage'
import { VehiclesPage } from './pages/VehiclesPage'
import { DriverWorkPage } from './pages/work/DriverWorkPage'
import { WorkSessionsPage } from './pages/WorkSessionsPage'
import { DispatcherTripsPage } from './pages/trips/DispatcherTripsPage'
import { DriverTripsPage } from './pages/trips/DriverTripsPage'
import { TripDetailsPage } from './pages/trips/TripDetailsPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/auth/callback', element: <AuthCallbackPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'audit-logs', element: <AuditLogsPage /> },
          { path: 'work', element: <DriverWorkPage /> },
          { path: 'timesheet', element: <TimesheetPage /> },
          { path: 'trips/mine', element: <DriverTripsPage /> },
          { path: 'trips/:id', element: <TripDetailsPage /> },
          { path: 'trips', element: <DispatcherTripsPage /> },
          { path: 'work-sessions', element: <WorkSessionsPage /> },
          { path: 'vehicles', element: <VehiclesPage /> },
          { path: 'users', element: <UsersPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
