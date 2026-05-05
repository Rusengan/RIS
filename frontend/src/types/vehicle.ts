export type VehicleStatus = 'ACTIVE' | 'MAINTENANCE' | 'RETIRED'

export type VehicleDto = {
  id: number
  plateNumber: string
  brand: string
  model: string
  capacityKg: number | null
  status: VehicleStatus
  createdAt: string
  updatedAt: string
}

