import { useEffect, useRef } from "react";
import type { Map as LeafletMap, Marker } from "leaflet";
import iconUrl from "leaflet/dist/images/marker-icon.png";
import iconRetinaUrl from "leaflet/dist/images/marker-icon-2x.png";
import shadowUrl from "leaflet/dist/images/marker-shadow.png";

type Props = {
  latitude: number | null;
  longitude: number | null;
  onChange?: (latitude: number | null, longitude: number | null) => void;
  readOnly?: boolean;
};

export function MapPicker({ latitude, longitude, onChange, readOnly }: Props) {
  const host = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const markerRef = useRef<Marker | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    if (import.meta.env.VITEST || !host.current) return;
    let cancelled = false;
    let map: LeafletMap | null = null;
    void import("leaflet").then(async (L) => {
      await import("leaflet/dist/leaflet.css");
      if (cancelled || !host.current || mapRef.current) return;
      const icon = L.icon({
        iconUrl,
        iconRetinaUrl,
        shadowUrl,
        iconSize: [25, 41],
        iconAnchor: [12, 41],
      });
      const start: [number, number] =
        latitude != null && longitude != null ? [latitude, longitude] : [36.2, 138.2];
      map = L.map(host.current).setView(start, latitude != null ? 11 : 5);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap",
      }).addTo(map);
      if (latitude != null && longitude != null) {
        markerRef.current = L.marker(start, { icon }).addTo(map);
      }
      if (!readOnly) {
        map.on("click", (event) => {
          const lat = round(event.latlng.lat);
          const lng = round(event.latlng.lng);
          onChangeRef.current?.(lat, lng);
        });
      }
      mapRef.current = map;
    });
    return () => {
      cancelled = true;
      mapRef.current?.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, [readOnly]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || import.meta.env.VITEST) return;
    void import("leaflet").then((L) => {
      if (latitude == null || longitude == null) {
        markerRef.current?.remove();
        markerRef.current = null;
        return;
      }
      const at: [number, number] = [latitude, longitude];
      if (!markerRef.current) {
        const icon = L.icon({
          iconUrl,
          iconRetinaUrl,
          shadowUrl,
          iconSize: [25, 41],
          iconAnchor: [12, 41],
        });
        markerRef.current = L.marker(at, { icon }).addTo(map);
      } else {
        markerRef.current.setLatLng(at);
      }
      map.panTo(at);
    });
  }, [latitude, longitude]);

  if (import.meta.env.VITEST) {
    return (
      <div>
        <p>地図 {latitude == null ? "未設定" : `${latitude}, ${longitude}`}</p>
        {readOnly ? null : (
          <button type="button" onClick={() => onChange?.(35.68123, 139.76712)}>
            地図をクリック
          </button>
        )}
      </div>
    );
  }

  return (
    <div>
      <div ref={host} className="map-pick" />
      {!readOnly && latitude != null ? (
        <button type="button" className="btn link" onClick={() => onChange?.(null, null)}>
          位置を消す
        </button>
      ) : null}
    </div>
  );
}

function round(value: number) {
  return Math.round(value * 100000) / 100000;
}
