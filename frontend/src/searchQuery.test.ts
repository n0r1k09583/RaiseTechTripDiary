import { describe, expect, it } from "vitest";
import { isImeConfirmKey, normalizeUserQuery, queryFromSearchForm } from "./searchQuery";

describe("normalizeUserQuery", () => {
  it("trims and strips leading at marks", () => {
    expect(normalizeUserQuery("  @yamada  ")).toBe("yamada");
    expect(normalizeUserQuery("@@@hana")).toBe("hana");
    expect(normalizeUserQuery("   ")).toBe("");
  });

  it("reads the submitted form value", () => {
    const form = document.createElement("form");
    const input = document.createElement("input");
    input.name = "q";
    input.value = " @hanako ";
    form.append(input);
    expect(queryFromSearchForm(form)).toBe("hanako");
  });

  it("ignores Enter while IME is confirming", () => {
    expect(isImeConfirmKey({ key: "Enter", nativeEvent: { isComposing: true } })).toBe(true);
    expect(isImeConfirmKey({ key: "Enter", nativeEvent: {}, keyCode: 229 })).toBe(true);
    expect(isImeConfirmKey({ key: "Enter", nativeEvent: { isComposing: false } })).toBe(false);
  });
});
