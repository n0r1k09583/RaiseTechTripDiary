import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AppHeader } from "./AppHeader";
import { user } from "./test/fixtures";

describe("AppHeader", () => {
  it("shows the display name and brand", () => {
    render(<AppHeader user={user} onLogout={vi.fn()} />);
    expect(screen.getByText("山田")).toBeInTheDocument();
    expect(screen.getByText("TripDiary")).toBeInTheDocument();
    expect(screen.getByText("旅の記録")).toBeInTheDocument();
  });

  it("calls logout", async () => {
    const onLogout = vi.fn();
    const events = userEvent.setup();
    render(<AppHeader user={user} onLogout={onLogout} />);
    await events.click(screen.getByRole("button", { name: "ログアウト" }));
    expect(onLogout).toHaveBeenCalledOnce();
  });

  it("calls onHome from the brand button", async () => {
    const onHome = vi.fn();
    const events = userEvent.setup();
    render(<AppHeader user={user} onLogout={vi.fn()} onHome={onHome} />);
    await events.click(screen.getByRole("button", { name: /TripDiary/ }));
    expect(onHome).toHaveBeenCalledOnce();
  });

  it("strips @ before searching", async () => {
    const onSearch = vi.fn();
    const events = userEvent.setup();
    render(<AppHeader user={user} onLogout={vi.fn()} onSearch={onSearch} />);
    await events.type(screen.getByLabelText("ユーザー名で検索"), "@hana");
    await events.click(screen.getByRole("button", { name: "検索" }));
    expect(onSearch).toHaveBeenCalledWith("hana");
  });

  it("does not search an empty query", async () => {
    const onSearch = vi.fn();
    const events = userEvent.setup();
    render(<AppHeader user={user} onLogout={vi.fn()} onSearch={onSearch} />);
    await events.click(screen.getByRole("button", { name: "検索" }));
    expect(onSearch).not.toHaveBeenCalled();
  });
});
