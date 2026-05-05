export type TripStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type RoutePointType = 'ORIGIN' | 'WAYPOINT' | 'DESTINATION'

export type RoutePointDto = {
  id: number
  tripId: number
  sequenceNo: number
  address: string
  latitude: number | string
  longitude: number | string
  pointType: RoutePointType
  arrivedAt: string | null
}

export type RouteDto = {
  id: number
  tripId: number
  encodedPolyline: string
  totalDistanceM: number
  totalDurationS: number
  provider: string
  calculatedAt: string
}

export type TripDto = {
  id: number
  driverId: number
  driverFullName: string
  vehicleId: number
  vehiclePlate: string
  dispatcherId: number
  workSessionId: number | null
  status: TripStatus
  plannedStartAt: string
  actualStartAt: string | null
  actualEndAt: string | null
  totalDistanceM: number | null
  totalDurationS: number | null
  cancelReason: string | null
  createdAt: string
  updatedAt: string
}

export type TripDetailsDto = {
  trip: TripDto
  route: RouteDto | null
  routePoints: RoutePointDto[]
}
