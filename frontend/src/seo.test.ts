import { afterEach, describe, expect, it } from "vitest";
import { applyPrivateSeo, applyPublicSeo, publicScreenFromPath } from "./seo";

describe("seo", () => {
  afterEach(() => {
    document.title = "";
    window.history.replaceState(null, "", "/");
  });

  it("indexes login and signup, not the logged-in timeline", () => {
    applyPublicSeo("login");
    expect(document.title).toBe("ログイン — TripDiary");
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute("content")).toBe(
      "index, follow",
    );
    expect(document.head.querySelector('meta[name="description"]')?.getAttribute("content")).toContain(
      "写真と感想で残すアプリ",
    );

    applyPublicSeo("signup");
    expect(document.title).toBe("新規登録 — TripDiary");
    expect(document.head.querySelector('link[rel="canonical"]')?.getAttribute("href")).toContain(
      "/signup",
    );

    applyPrivateSeo();
    expect(document.title).toBe("旅の記録 — TripDiary");
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute("content")).toBe(
      "noindex, nofollow",
    );
  });

  it("maps /signup to the public signup screen", () => {
    expect(publicScreenFromPath("/signup")).toBe("signup");
    expect(publicScreenFromPath("/login")).toBe("login");
    expect(publicScreenFromPath("/")).toBe("login");
  });
});
