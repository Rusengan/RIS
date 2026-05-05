import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

import { parseUserDtoFromAccessToken } from '../lib/jwt'
import { useAuthStore } from '../store/authStore'

export function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)

  useEffect(() => {
    const accessToken = searchParams.get('accessToken')
    const refreshToken = searchParams.get('refreshToken')

    if (!accessToken || !refreshToken) {
      navigate('/login', { replace: true })
      return
    }

    const user = parseUserDtoFromAccessToken(accessToken)
    setSession(accessToken, refreshToken, user)
    navigate('/dashboard', { replace: true })
  }, [navigate, searchParams, setSession])

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-300">
      <p className="text-sm">Вход…</p>
    </div>
  )
}
