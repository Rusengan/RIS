import type { RoleCode } from '../types/role'
import type { UserDto } from '../types/userDto'

const KNOWN_ROLES: readonly RoleCode[] = ['DRIVER', 'DISPATCHER', 'ADMIN']

function decodeJwtPayload(accessToken: string): Record<string, unknown> | null {
  try {
    const parts = accessToken.split('.')
    if (parts.length < 2) {
      return null
    }
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return null
  }
}

/** Reads `roles` from JWT payload (no signature verification). */
export function parseRoleCodesFromJwt(accessToken: string): RoleCode[] {
  const payload = decodeJwtPayload(accessToken)
  if (!payload || !Array.isArray(payload.roles)) {
    return []
  }
  return payload.roles.filter(
    (r): r is RoleCode =>
      typeof r === 'string' && (KNOWN_ROLES as readonly string[]).includes(r),
  )
}

/** Builds `UserDto` from access token claims (no signature verification). */
export function parseUserDtoFromAccessToken(accessToken: string): UserDto {
  const roles = parseRoleCodesFromJwt(accessToken)
  const payload = decodeJwtPayload(accessToken)
  const rawSub = payload?.sub
  const id =
    typeof rawSub === 'string'
      ? Number(rawSub)
      : typeof rawSub === 'number'
        ? rawSub
        : NaN
  const email = typeof payload?.email === 'string' ? payload.email : ''
  return {
    id: Number.isFinite(id) ? id : 0,
    email,
    fullName: email,
    pictureUrl: null,
    enabled: true,
    roles,
  }
}
