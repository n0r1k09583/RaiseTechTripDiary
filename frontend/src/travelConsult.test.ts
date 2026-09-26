import { describe, expect, it } from "vitest";
import { consultTravel } from "./travelConsult";

const memory = { place: null } as const;

describe("consultTravel", () => {
  it("introduces famous sights when the place is not named yet", () => {
    const answer = consultTravel("こんにちは", "山田", memory);
    expect(answer.place).toBeNull();
    expect(answer.text).toContain("観光地");
  });

  it("answers with famous sights in that city", () => {
    const answer = consultTravel("パリの有名なところ", "山田", memory);
    expect(answer.place).toBe("パリ");
    expect(answer.text).toContain("エッフェル塔");
    expect(answer.text).toContain("ルーブル美術館");
    expect(answer.text).not.toContain("記録");
  });

  it("answers a named landmark and keeps the conversation on that city", () => {
    const first = consultTravel("金閣寺は見た方がいい？", "山田", memory);
    expect(first.place).toBe("京都");
    expect(first.text).toContain("金閣寺");
    const next = consultTravel("日帰りなら？", "山田", { place: first.place });
    expect(next.place).toBe("京都");
    expect(next.text).toContain("清水寺");
  });

  it("reads a map pin as famous sights in that region", () => {
    const answer = consultTravel("ここに行きたい 海外 ヨーロッパ", "山田", memory);
    expect(answer.place).toBe("パリ");
    expect(answer.text).toContain("地図のピン");
    expect(answer.text).toContain("エッフェル塔");
  });

  it("answers Brisbane instead of repeating Sydney", () => {
    const answer = consultTravel("ブリスベンは？", "花子", { place: "シドニー" });
    expect(answer.place).toBe("ブリスベン");
    expect(answer.text).toContain("サウスバンク");
    expect(answer.text).not.toContain("オペラハウス");
  });

  it("answers Osaka instead of Kyoto", () => {
    const answer = consultTravel("大阪の有名なところ", "山田", memory);
    expect(answer.place).toBe("大阪");
    expect(answer.text).toContain("大阪城");
    expect(answer.text).not.toContain("清水寺");
  });

  it("suggests another famous place when asked what else", () => {
    const answer = consultTravel("他には？", "山田", { place: "パリ" });
    expect(answer.place).not.toBe("パリ");
    expect(answer.text).toContain("有名");
  });
});
