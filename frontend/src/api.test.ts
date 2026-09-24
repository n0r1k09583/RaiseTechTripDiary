import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  accessOf,
  createPost,
  getToken,
  login,
  logout,
  searchUsers,
  setSession,
} from "./api";

describe("api", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the server error body on 401", async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ status: 401, code: "UNAUTHORIZED", error: "ログインしてください" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(login("a@example.com", "x")).rejects.toThrow("ログインしてください");
  });

  it("falls back when the error body is not JSON", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response("<html>500</html>", { status: 500 }));
    await expect(login("a@example.com", "x")).rejects.toThrow(
      "サーバーに接続できません。バックエンドを起動してください",
    );
  });

  it("falls back on 400 without error field", async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response("{}", { status: 400, headers: { "Content-Type": "application/json" } }),
    );
    await expect(login("a@example.com", "x")).rejects.toThrow("リクエストに失敗しました");
  });

  it("maps a network failure to a Japanese message", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("Failed to fetch"));
    await expect(login("a@example.com", "x")).rejects.toThrow(
      "サーバーに接続できません。バックエンドを起動してください",
    );
  });

  it("does not send Content-Type for FormData", async () => {
    setSession("access-token", "refresh-token");
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ id: 1, body: "hi" }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await createPost({
      spotName: "東京駅",
      areaTag: "関東",
      visitStatus: "visited",
      body: "hi",
      image: new File(["x"], "a.jpg", { type: "image/jpeg" }),
    });
    const [, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(headers.get("Content-Type")).toBeNull();
    expect(headers.get("Authorization")).toBe("Bearer access-token");
    expect(init.body).toBeInstanceOf(FormData);
  });

  it("treats 204 as success with no body", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }));
    setSession("a", "r");
    await logout();
    expect(getToken()).toBeNull();
  });

  it("clears the session even when logout API fails", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("Failed to fetch"));
    setSession("a", "r");
    await logout();
    expect(getToken()).toBeNull();
  });

  it("prefers accessToken over legacy token", () => {
    expect(accessOf({ accessToken: "new", token: "old", user: { id: 1, email: "a", username: "a", displayName: "A" } })).toBe(
      "new",
    );
    expect(accessOf({ token: "old", user: { id: 1, email: "a", username: "a", displayName: "A" } })).toBe("old");
  });

  it("refreshes an expired access token and retries", async () => {
    setSession("expired", "refresh-token");
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ error: "トークンが無効です。再度ログインしてください" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            accessToken: "fresh",
            refreshToken: "next-refresh",
            user: { id: 1, email: "a", username: "a", displayName: "A" },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ users: [{ id: 2, username: "hanako", displayName: "佐藤 花子" }] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    const res = await searchUsers("hana");
    expect(res.users[0]?.username).toBe("hanako");
    expect(getToken()).toBe("fresh");
    expect(vi.mocked(fetch).mock.calls[1]?.[0]).toBe("/api/refresh");
    expect(vi.mocked(fetch).mock.calls[2]?.[0]).toBe("/api/users?q=hana");
  });
});
