import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SignupPage } from "./SignupPage";
import { signup } from "./api";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, signup: vi.fn() };
});

describe("SignupPage", () => {
  const onSuccess = vi.fn();
  const onGoLogin = vi.fn();

  beforeEach(() => {
    onSuccess.mockReset();
    onGoLogin.mockReset();
    vi.mocked(signup).mockReset();
  });

  it("shows the API error and does not succeed", async () => {
    const user = userEvent.setup();
    vi.mocked(signup).mockRejectedValue(new Error("このユーザー名は使われています"));
    render(<SignupPage onSuccess={onSuccess} onGoLogin={onGoLogin} />);
    await user.type(screen.getByLabelText("ユーザー名"), "yamada");
    await user.type(screen.getByLabelText("表示名"), "山田");
    await user.type(screen.getByLabelText("メールアドレス"), "yama@example.com");
    await user.type(screen.getByLabelText("パスワード"), "password123");
    await user.type(screen.getByLabelText("パスワード確認"), "password123");
    await user.click(screen.getByRole("button", { name: "登録する" }));
    expect(await screen.findByText("このユーザー名は使われています")).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("goes to login", async () => {
    const user = userEvent.setup();
    render(<SignupPage onSuccess={onSuccess} onGoLogin={onGoLogin} />);
    await user.click(screen.getByRole("link", { name: "ログイン" }));
    expect(onGoLogin).toHaveBeenCalledOnce();
  });
});
