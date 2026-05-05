import { decode } from '@googlemaps/polyline-codec'
import { GoogleMap, Marker, Polyline, useJsApiLoader } from '@react-google-maps/api'

import type { RoutePointDto } from '../../types/trip'

type Props = {
  encodedPolyline: string
  points: RoutePointDto[]
}

const mapContainerStyle = { width: '100%', height: '360px' }

export function RouteMap({ encodedPolyline, points }: Props) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined
  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: apiKey ?? '',
    id: 'route-map-script',
  })

  const path = decode(encodedPolyline).map(([lat, lng]) => ({ lat, lng }))
  const markerCoords = points.map((p) => ({
    lat: Number(p.latitude),
    lng: Number(p.longitude),
    label: p.sequenceNo,
  }))

  let center = path[0] ?? markerCoords[0]
  if (!center && markerCoords.length > 0) {
    center = { lat: markerCoords[0].lat, lng: markerCoords[0].lng }
  }

  if (!apiKey || !isLoaded || !center) {
    return (
      <div className="rounded-lg border border-slate-700 bg-slate-900 p-4 text-sm text-slate-400">
        {!apiKey
          ? 'Задайте VITE_GOOGLE_MAPS_API_KEY для карты.'
          : 'Загрузка карты…'}
      </div>
    )
  }

  const bounds = new google.maps.LatLngBounds()
  if (path.length > 0) {
    path.forEach((p) => bounds.extend(p))
  }
  markerCoords.forEach((m) => bounds.extend({ lat: m.lat, lng: m.lng }))

  return (
    <GoogleMap
      mapContainerStyle={mapContainerStyle}
      center={center}
      zoom={12}
      onLoad={(map) => map.fitBounds(bounds)}
    >
      <Polyline path={path} options={{ strokeColor: '#38bdf8', strokeWeight: 4 }} />
      {markerCoords.map((m, i) => (
        <Marker key={i} position={{ lat: m.lat, lng: m.lng }} label={String(m.label)} />
      ))}
    </GoogleMap>
  )
}
