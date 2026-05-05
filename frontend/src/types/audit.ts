export type AuditLogDto = {
  id: number
  userId: number | null
  userFullName: string | null
  action: string
  entityType: string
  entityId: number | null
  payloadJson: string | null
  createdAt: string
}
