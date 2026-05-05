import type { RoleCode } from './role'

/** Mirrors backend `UserDto` (roles as enum codes). */
export type UserDto = {
  id: number
  email: string
  fullName: string
  pictureUrl: string | null
  enabled: boolean
  roles: RoleCode[]
}
