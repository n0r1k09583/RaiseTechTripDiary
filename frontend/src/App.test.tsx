import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { getToken, me } from "./api";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, getToken: vi.fn(), me: vi.fn(), logout: vi.fn(), setSession: vi.fn() };
});

describe("App", () => {
  beforeEach(() => {
    vi.mocked(getToken).mockReset();
    vi.mocked(me).mockReset();
  });

  it("shows login when there is no token", async () => {
    vi.mocked(getToken).mockReturnValue(null);
    window.history.replaceState(null, "", "/");
    render(<App />);
    expect(await screen.findByRole("heading", { name: "ログイン" })).toBeInTheDocument();
    expect(me).not.toHaveBeenCalled();
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute("content")).toBe(
      "index, follow",
    );
  });

  it("opens signup from /signup without a token", async () => {
    vi.mocked(getToken).mockReturnValue(null);
    window.history.replaceState(null, "", "/signup");
    render(<App />);
    expect(await screen.findByRole("heading", { name: "新規登録" })).toBeInTheDocument();
    expect(document.title).toBe("新規登録 — TripDiary");
  });

  it("stays on login when me fails", async () => {
    vi.mocked(getToken).mockReturnValue("dead-token");
    vi.mocked(me).mockRejectedValue(new Error("トークンが無効です。再度ログインしてください"));
    render(<App />);
    expect(await screen.findByRole("heading", { name: "ログイン" })).toBeInTheDocument();
  });
});
