import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { UserDto } from '../types/userDto'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  user: UserDto | null
  setSession: (accessToken: string, refreshToken: string, user: UserDto) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, user }),
      clearSession: () =>
        set({ accessToken: null, refreshToken: null, user: null }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    },
  ),
)
