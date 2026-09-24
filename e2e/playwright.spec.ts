import { test, expect } from "@playwright/test";

test("login then see travel feed", async ({ page }) => {
  await page.goto("http://127.0.0.1:5173/login");
  await expect(page.getByText("TripDiary")).toBeVisible();
  await page.getByRole("button", { name: "@yamada" }).click();
  await expect(page.getByRole("heading", { name: "みんなの旅" })).toBeVisible();
  await expect(page.getByRole("button", { name: "行きたい" })).toBeVisible();
});
