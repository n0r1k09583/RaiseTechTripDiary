import { describe, expect, it } from "vitest";
import { answerGuide } from "./planGuide";
import { post } from "./test/fixtures";

const memory = { area: null, days: null } as const;

describe("answerGuide", () => {
  it("asks for an area when the message does not name one", () => {
    const answer = answerGuide("こんにちは", [], "山田", memory);
    expect(answer.stops).toEqual([]);
    expect(answer.text).toContain("世界地図");
  });

  it("builds a shared plan from public records in that area", () => {
    const answer = answerGuide(
      "沖縄の日帰り",
      [
        post({ id: 1, spotName: "恩納村の海", areaTag: "沖縄", displayName: "山田", likeCount: 2, body: "透き通っていました" }),
        post({ id: 2, spotName: "首里城", areaTag: "沖縄", displayName: "花子", likeCount: 1, body: "石畳がきれい" }),
        post({ id: 3, spotName: "函館山", areaTag: "北海道", displayName: "花子" }),
      ],
      "山田",
      memory,
    );
    expect(answer.area).toBe("沖縄");
    expect(answer.days).toBe(1);
    expect(answer.stops.map((stop) => stop.spotName)).toEqual(["恩納村の海", "首里城"]);
    expect(answer.text).toContain("山田さんと花子さん");
    expect(answer.stops[0]?.when).toBe("1日目 午前");
  });

  it("uses the area with the most records when asked for a recommendation", () => {
    const answer = answerGuide(
      "おすすめ",
      [
        post({ id: 1, areaTag: "近畿", spotName: "金閣寺", displayName: "一郎" }),
        post({ id: 2, areaTag: "北海道", spotName: "函館山", displayName: "花子", likeCount: 3 }),
        post({ id: 3, areaTag: "北海道", spotName: "五稜郭", displayName: "山田", likeCount: 1 }),
      ],
      "山田",
      memory,
    );
    expect(answer.area).toBe("北海道");
    expect(answer.days).toBe(3);
    expect(answer.stops[0]?.spotName).toBe("函館山");
  });

  it("keeps the previous area when only the length changes", () => {
    const answer = answerGuide("2泊3日", [post({ areaTag: "近畿", spotName: "金閣寺" })], "山田", {
      area: "近畿",
      days: 1,
    });
    expect(answer.area).toBe("近畿");
    expect(answer.days).toBe(3);
    expect(answer.text).toContain("2泊3日");
    expect(answer.text).toContain("スマート珈琲店");
  });

  it("reads a map click as a wish and adds cafes and restaurants", () => {
    const answer = answerGuide(
      "ここに行きたい 関東",
      [post({ areaTag: "関東", spotName: "浅草のカフェ", displayName: "花子", body: "コーヒーがおいしい" })],
      "山田",
      memory,
    );
    expect(answer.area).toBe("関東");
    expect(answer.text).toContain("地図のその場所は関東");
    expect(answer.text).toContain("猿田彦珈琲");
    expect(answer.text).toContain("たいめいけん");
    expect(answer.text).toContain("花子さんの「浅草のカフェ」");
  });
});
