export const AREAS = [
  "北海道",
  "東北",
  "関東",
  "中部",
  "近畿",
  "中国",
  "四国",
  "九州",
  "沖縄",
  "海外",
] as const;

export type Area = (typeof AREAS)[number];
