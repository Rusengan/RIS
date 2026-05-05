/** Matches backend `RoleCode` */
export type RoleCode = 'DRIVER' | 'DISPATCHER' | 'ADMIN'

export function hasAnyRole(userRoles: RoleCode[], allowed: RoleCode[]): boolean {
  if (allowed.length === 0) {
    return true
  }
  return userRoles.some((r) => allowed.includes(r))
}
