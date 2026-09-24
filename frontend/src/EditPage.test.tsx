import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EditPage } from "./EditPage";
import { getPost, updatePost } from "./api";
import { post, user } from "./test/fixtures";

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, getPost: vi.fn(), updatePost: vi.fn() };
});

describe("EditPage", () => {
  beforeEach(() => {
    vi.mocked(getPost).mockReset();
    vi.mocked(updatePost).mockReset();
  });

  it("returns home when the post is not mine", async () => {
    const onDone = vi.fn();
    vi.mocked(getPost).mockResolvedValue(post({ mine: false }));
    render(<EditPage user={user} postId={10} onLogout={vi.fn()} onDone={onDone} />);
    await vi.waitFor(() => expect(onDone).toHaveBeenCalledOnce());
  });

  it("shows an error when the post cannot be opened", async () => {
    vi.mocked(getPost).mockRejectedValue(new Error("投稿が見つかりません"));
    render(<EditPage user={user} postId={10} onLogout={vi.fn()} onDone={vi.fn()} />);
    expect(await screen.findByText("投稿が見つかりません")).toBeInTheDocument();
  });

  it("rejects gif on the client without calling updatePost", async () => {
    const events = userEvent.setup();
    vi.mocked(getPost).mockResolvedValue(post({ body: "元の本文" }));
    render(<EditPage user={user} postId={10} onLogout={vi.fn()} onDone={vi.fn()} />);
    await screen.findByDisplayValue("元の本文");
    const gif = new File(["gif"], "x.gif", { type: "image/gif" });
    fireEvent.change(screen.getByLabelText("写真"), { target: { files: [gif] } });
    await events.click(screen.getByRole("button", { name: "保存する" }));
    expect(await screen.findByText("JPEG / PNG / WebP のみです")).toBeInTheDocument();
    expect(updatePost).not.toHaveBeenCalled();
  });

  it("rejects an empty body without calling updatePost", async () => {
    const events = userEvent.setup();
    vi.mocked(getPost).mockResolvedValue(post({ body: "元の本文" }));
    render(<EditPage user={user} postId={10} onLogout={vi.fn()} onDone={vi.fn()} />);
    await screen.findByDisplayValue("元の本文");
    await events.clear(screen.getByLabelText("感想（写真だけでも可）"));
    await events.click(screen.getByRole("button", { name: "保存する" }));
    expect(await screen.findByText("感想か写真のどちらかが必要です")).toBeInTheDocument();
    expect(updatePost).not.toHaveBeenCalled();
  });

  it("shows the API error when save fails", async () => {
    const events = userEvent.setup();
    vi.mocked(getPost).mockResolvedValue(post({ body: "元の本文" }));
    vi.mocked(updatePost).mockRejectedValue(new Error("自分の投稿だけ編集できます"));
    render(<EditPage user={user} postId={10} onLogout={vi.fn()} onDone={vi.fn()} />);
    await screen.findByDisplayValue("元の本文");
    await events.click(screen.getByRole("button", { name: "保存する" }));
    expect(await screen.findByText("自分の投稿だけ編集できます")).toBeInTheDocument();
  });
});
