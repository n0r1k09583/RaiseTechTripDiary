import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TimelinePage } from "./TimelinePage";
import { createPost, deletePost, listPosts } from "./api";
import { post, user } from "./test/fixtures";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return {
    ...actual,
    listPosts: vi.fn(),
    createPost: vi.fn(),
    deletePost: vi.fn(),
  };
});

describe("TimelinePage", () => {
  beforeEach(() => {
    vi.mocked(listPosts).mockReset();
    vi.mocked(createPost).mockReset();
    vi.mocked(deletePost).mockReset();
    vi.mocked(listPosts).mockResolvedValue({ posts: [post()], hasMore: false });
  });

  it("shows a load error from the API", async () => {
    vi.mocked(listPosts).mockRejectedValue(new Error("サーバーに接続できません。バックエンドを起動してください"));
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    expect(
      await screen.findByText("サーバーに接続できません。バックエンドを起動してください"),
    ).toBeInTheDocument();
  });

  it("rejects gif on the client without calling createPost", async () => {
    const events = userEvent.setup();
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await screen.findByText("本文です");
    await events.type(screen.getByLabelText("場所名"), "函館山");
    await events.type(screen.getByLabelText("感想（写真だけでも可）"), "画像つき");
    const gif = new File(["gif"], "x.gif", { type: "image/gif" });
    fireEvent.change(screen.getByLabelText("写真"), { target: { files: [gif] } });
    await events.click(screen.getByRole("button", { name: "みんなと共有する" }));
    expect(await screen.findByText("JPEG / PNG / WebP のみです")).toBeInTheDocument();
    expect(createPost).not.toHaveBeenCalled();
  });

  it("rejects an empty body without calling createPost", async () => {
    const events = userEvent.setup();
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await screen.findByText("本文です");
    await events.click(screen.getByRole("button", { name: "みんなと共有する" }));
    expect(await screen.findByText("場所名は1〜40文字です")).toBeInTheDocument();
    expect(createPost).not.toHaveBeenCalled();
  });

  it("shows the following-tab empty state from the API", async () => {
    const events = userEvent.setup();
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await screen.findByText("本文です");
    vi.mocked(listPosts).mockResolvedValue({ posts: [], hasMore: false });
    await events.click(screen.getByRole("button", { name: "フォロー中" }));
    expect(
      await screen.findByText("フォロー中の記録はまだありません。プロフィールからフォローできます"),
    ).toBeInTheDocument();
  });

  it("logs out when the list API says the session expired", async () => {
    const onLogout = vi.fn();
    vi.mocked(listPosts).mockRejectedValue(new Error("ログインしてください"));
    render(<TimelinePage user={user} onLogout={onLogout} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await vi.waitFor(() => expect(onLogout).toHaveBeenCalledOnce());
  });

  it("keeps the post when delete API fails", async () => {
    const events = userEvent.setup();
    vi.mocked(deletePost).mockRejectedValue(new Error("自分の投稿だけ削除できます"));
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await screen.findByText("本文です");
    await events.click(screen.getByRole("button", { name: "削除" }));
    await events.click(screen.getByRole("button", { name: "削除する" }));
    expect(await screen.findByText("自分の投稿だけ削除できます")).toBeInTheDocument();
    expect(screen.getByText("本文です")).toBeInTheDocument();
  });

  it("posts a photo without a caption", async () => {
    const events = userEvent.setup();
    const created = post({ id: 99, body: "", imageUrl: "/uploads/a.jpg", spotName: "函館山" });
    vi.mocked(createPost).mockResolvedValue(created);
    render(<TimelinePage user={user} onLogout={vi.fn()} onEdit={vi.fn()} onOpen={vi.fn()} onProfile={vi.fn()} onHome={vi.fn()} onSearch={vi.fn()} />);
    await screen.findByText("本文です");
    await events.type(screen.getByLabelText("場所名"), "函館山");
    const jpeg = new File(["jpeg"], "spot.jpg", { type: "image/jpeg" });
    fireEvent.change(screen.getByLabelText("写真"), { target: { files: [jpeg] } });
    await events.click(screen.getByRole("button", { name: "みんなと共有する" }));
    await vi.waitFor(() => expect(createPost).toHaveBeenCalledOnce());
    expect(createPost).toHaveBeenCalledWith(
      expect.objectContaining({ spotName: "函館山", body: "", image: jpeg }),
    );
    expect(await screen.findByText("写真をみんなと共有しました")).toBeInTheDocument();
  });
});
