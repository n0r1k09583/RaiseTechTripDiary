import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";
import { login } from "./api";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, login: vi.fn() };
});

describe("LoginPage", () => {
  const onSuccess = vi.fn();
  const onGoSignup = vi.fn();

  beforeEach(() => {
    onSuccess.mockReset();
    onGoSignup.mockReset();
    vi.mocked(login).mockReset();
  });

  it("shows a validation error without calling the API when fields are empty", async () => {
    const user = userEvent.setup();
    render(<LoginPage onSuccess={onSuccess} onGoSignup={onGoSignup} />);
    await user.click(screen.getByRole("button", { name: "ログイン" }));
    expect(screen.getByText("メールアドレスとパスワードを入力してください")).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("clears the password when login fails", async () => {
    const user = userEvent.setup();
    vi.mocked(login).mockRejectedValue(new Error("メールアドレスまたはパスワードが違います"));
    render(<LoginPage onSuccess={onSuccess} onGoSignup={onGoSignup} />);
    await user.type(screen.getByLabelText("メールアドレス"), "yamada@example.com");
    await user.type(screen.getByLabelText("パスワード"), "wrong-pass");
    await user.click(screen.getByRole("button", { name: "ログイン" }));
    expect(await screen.findByText("メールアドレスまたはパスワードが違います")).toBeInTheDocument();
    expect(screen.getByLabelText("パスワード")).toHaveValue("");
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("goes to signup", async () => {
    const user = userEvent.setup();
    render(<LoginPage onSuccess={onSuccess} onGoSignup={onGoSignup} />);
    await user.click(screen.getByRole("link", { name: "新規登録" }));
    expect(onGoSignup).toHaveBeenCalledOnce();
  });
});
