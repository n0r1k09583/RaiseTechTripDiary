import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { GuideWalker } from "./GuideWalker";

describe("GuideWalker", () => {
  it("keeps the world map and consults like a travel agent", async () => {
    const events = userEvent.setup();
    render(<GuideWalker displayName="山田" />);

    expect(screen.getByRole("region", { name: "みんなの旅行プラン" })).toBeInTheDocument();
    expect(screen.getByText("世界地図を押すと、ここに行きたい")).toBeInTheDocument();
    expect(screen.queryByText("函館山")).not.toBeInTheDocument();

    await events.click(screen.getByRole("button", { name: "地図をクリック" }));
    expect(await screen.findByText(/浅草寺/)).toBeInTheDocument();

    await events.click(screen.getByRole("button", { name: "パリ" }));
    expect(await screen.findByText(/エッフェル塔/)).toBeInTheDocument();
  });
});
