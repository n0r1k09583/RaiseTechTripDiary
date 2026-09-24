import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SearchPage } from "./SearchPage";
import { user } from "./test/fixtures";

const searchUsers = vi.fn();

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>();
  return { ...actual, searchUsers: (...args: unknown[]) => searchUsers(...args) };
});

describe("SearchPage", () => {
  beforeEach(() => {
    searchUsers.mockReset();
  });

  it("shows a loading state before empty results", () => {
    searchUsers.mockReturnValue(new Promise(() => undefined));
    render(
      <SearchPage
        user={user}
        q="hana"
        onLogout={vi.fn()}
        onHome={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    expect(screen.getByText("読み込み中…")).toBeInTheDocument();
    expect(screen.queryByText(/該当するユーザーはいません/)).not.toBeInTheDocument();
  });

  it("renders matched users after the API returns", async () => {
    searchUsers.mockResolvedValue({
      users: [{ id: 2, username: "hanako", displayName: "佐藤 花子", followedByMe: false, mine: false }],
    });
    render(
      <SearchPage
        user={user}
        q="@hana"
        onLogout={vi.fn()}
        onHome={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    expect(await screen.findByText("佐藤 花子")).toBeInTheDocument();
    expect(screen.getByText("@hanako")).toBeInTheDocument();
    expect(screen.getByText(/検索結果 1件/)).toBeInTheDocument();
  });

  it("does not keep the previous person when the query changes", async () => {
    searchUsers.mockResolvedValue({
      users: [{ id: 2, username: "hanako", displayName: "佐藤 花子", followedByMe: false, mine: false }],
    });
    const { rerender } = render(
      <SearchPage
        user={user}
        q="hana"
        onLogout={vi.fn()}
        onHome={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    expect(await screen.findByText("佐藤 花子")).toBeInTheDocument();

    searchUsers.mockReturnValue(new Promise(() => undefined));
    rerender(
      <SearchPage
        user={user}
        q="山田"
        onLogout={vi.fn()}
        onHome={vi.fn()}
        onProfile={vi.fn()}
        onSearch={vi.fn()}
      />,
    );
    expect(screen.getByText("読み込み中…")).toBeInTheDocument();
    expect(screen.queryByText("佐藤 花子")).not.toBeInTheDocument();
  });
});
