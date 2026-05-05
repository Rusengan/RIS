export type WorkSessionStatus = 'OPEN' | 'CLOSED'

export type BreakType = 'SHORT' | 'LUNCH' | 'MANDATORY'

export type BreakLogDto = {
  id: number
  workSessionId: number
  breakType: BreakType
  startedAt: string
  endedAt: string | null
  durationMinutes: number | null
  createdAt: string
  updatedAt: string
}

export type WorkSessionDto = {
  id: number
  driverId: number
  startedAt: string
  endedAt: string | null
  totalMinutes: number | null
  status: WorkSessionStatus
  createdAt: string
  updatedAt: string
  breaks: BreakLogDto[]
}
