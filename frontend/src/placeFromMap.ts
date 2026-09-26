import type { Area } from "./areas";

type Box = {
  area: Area;
  south: number;
  north: number;
  west: number;
  east: number;
};

const BOXES: Box[] = [
  { area: "沖縄", south: 24, north: 28.5, west: 122.5, east: 131.5 },
  { area: "北海道", south: 41.3, north: 45.7, west: 139.2, east: 146 },
  { area: "四国", south: 32.7, north: 34.45, west: 132, east: 134.9 },
  { area: "九州", south: 30.9, north: 34, west: 129.3, east: 132.1 },
  { area: "中国", south: 34.1, north: 35.7, west: 130.8, east: 134.6 },
  { area: "近畿", south: 33.4, north: 35.75, west: 134.8, east: 136.6 },
  { area: "中部", south: 34.5, north: 37.6, west: 136.2, east: 139 },
  { area: "関東", south: 34.8, north: 37.2, west: 138.7, east: 141.1 },
  { area: "東北", south: 36.8, north: 41.5, west: 139.1, east: 142.2 },
];

export type MapPlace = {
  area: Area;
  label: string;
};

export function areaFromPoint(latitude: number, longitude: number): MapPlace {
  const hit = BOXES.find(
    (box) =>
      latitude >= box.south &&
      latitude <= box.north &&
      longitude >= box.west &&
      longitude <= box.east,
  );
  if (hit) return { area: hit.area, label: hit.area };
  return { area: "海外", label: abroadLabel(latitude, longitude) };
}

function abroadLabel(latitude: number, longitude: number) {
  if (latitude >= 18 && latitude <= 23 && longitude >= -161 && longitude <= -154) return "ハワイ";
  if (latitude >= 35 && latitude <= 72 && longitude >= -12 && longitude <= 40) return "ヨーロッパ";
  if (latitude >= 24 && latitude <= 55 && longitude >= -130 && longitude <= -60) return "北アメリカ";
  if (latitude >= -10 && latitude <= 24 && longitude >= 95 && longitude <= 141) return "東南アジア";
  if (latitude >= -45 && latitude <= -10 && longitude >= 110 && longitude <= 180) return "オセアニア";
  return "その土地";
}
