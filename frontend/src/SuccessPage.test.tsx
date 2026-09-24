import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SuccessPage } from "./SuccessPage";
import { user } from "./test/fixtures";

describe("SuccessPage", () => {
  it("shows the logged-in user", () => {
    render(<SuccessPage user={user} onLogout={vi.fn()} />);
    expect(screen.getByRole("heading", { name: "ログイン成功" })).toBeInTheDocument();
    expect(screen.getByText(/山田（@yamada）/)).toBeInTheDocument();
  });

  it("calls logout", async () => {
    const onLogout = vi.fn();
    const events = userEvent.setup();
    render(<SuccessPage user={user} onLogout={onLogout} />);
    await events.click(screen.getByRole("button", { name: "ログアウト" }));
    expect(onLogout).toHaveBeenCalledOnce();
  });
});
