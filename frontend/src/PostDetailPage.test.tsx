import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PostDetailPage } from "./PostDetailPage";
import { createComment, deleteComment, getPost, listComments } from "./api";
import { comment, post, user } from "./test/fixtures";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return {
    ...actual,
    getPost: vi.fn(),
    listComments: vi.fn(),
    createComment: vi.fn(),
    deleteComment: vi.fn(),
  };
});

describe("PostDetailPage", () => {
  beforeEach(() => {
    vi.mocked(getPost).mockReset();
    vi.mocked(listComments).mockReset();
    vi.mocked(createComment).mockReset();
    vi.mocked(deleteComment).mockReset();
  });

  it("shows not found when the post API fails", async () => {
    vi.mocked(getPost).mockRejectedValue(new Error("投稿が見つかりません"));
    vi.mocked(listComments).mockRejectedValue(new Error("投稿が見つかりません"));
    render(
      <PostDetailPage
        user={user}
        postId={99}
        onLogout={vi.fn()}
        onBack={vi.fn()}
        onEdit={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    expect(await screen.findByText("投稿が見つかりません")).toBeInTheDocument();
  });

  it("rejects a blank comment without calling the API", async () => {
    const events = userEvent.setup();
    vi.mocked(getPost).mockResolvedValue(post());
    vi.mocked(listComments).mockResolvedValue({ comments: [] });
    render(
      <PostDetailPage
        user={user}
        postId={10}
        onLogout={vi.fn()}
        onBack={vi.fn()}
        onEdit={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    await screen.findByText("本文です");
    await events.click(screen.getByRole("button", { name: "送信" }));
    expect(await screen.findByText("コメントは1〜140文字です")).toBeInTheDocument();
    expect(createComment).not.toHaveBeenCalled();
  });

  it("keeps the comment when delete API fails", async () => {
    const events = userEvent.setup();
    vi.mocked(getPost).mockResolvedValue(post({ commentCount: 1 }));
    vi.mocked(listComments).mockResolvedValue({ comments: [comment()] });
    vi.mocked(deleteComment).mockRejectedValue(new Error("自分のコメントだけ削除できます"));
    render(
      <PostDetailPage
        user={user}
        postId={10}
        onLogout={vi.fn()}
        onBack={vi.fn()}
        onEdit={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    await screen.findByText("コメントです");
    await events.click(screen.getByRole("button", { name: "削除" }));
    await events.click(screen.getByRole("button", { name: "削除する" }));
    expect(await screen.findByText("自分のコメントだけ削除できます")).toBeInTheDocument();
    expect(screen.getByText("コメントです")).toBeInTheDocument();
  });
});
