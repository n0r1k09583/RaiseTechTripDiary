import { describe, expect, it } from "vitest";
import { areaFromPoint } from "./placeFromMap";

describe("areaFromPoint", () => {
  it("maps a click in Tokyo to Kanto", () => {
    expect(areaFromPoint(35.68123, 139.76712)).toEqual({ area: "関東", label: "関東" });
  });

  it("maps a click in Okinawa and Paris", () => {
    expect(areaFromPoint(26.5, 127.8).area).toBe("沖縄");
    expect(areaFromPoint(48.85, 2.35)).toEqual({ area: "海外", label: "ヨーロッパ" });
  });
});
