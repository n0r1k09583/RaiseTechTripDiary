import { useEffect, useState } from "react";
import {
  followUser,
  listFollowees,
  listFollowers,
  unfollowUser,
  type User,
  type UserSummary,
} from "./api";
import { AppHeader } from "./AppHeader";

type Kind = "followees" | "followers";

type Props = {
  user: User;
  username: string;
  kind: Kind;
  onLogout: () => void | Promise<void>;
  onHome: () => void;
  onProfile: (username: string) => void;
  onSearch: (q: string) => void;
  onFollows: (username: string, kind: Kind) => void;
};

export function FollowListPage({
  user,
  username,
  kind,
  onLogout,
  onHome,
  onProfile,
  onSearch,
  onFollows,
}: Props) {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    const load = kind === "followers" ? listFollowers : listFollowees;
    load(username)
      .then((res) => {
        if (!cancelled) setUsers(res.users);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "読み込みに失敗しました");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [username, kind]);

  async function onToggle(target: UserSummary) {
    if (target.mine) return;
    try {
      const next = target.followedByMe ? await unfollowUser(target.username) : await followUser(target.username);
      setUsers((prev) =>
        prev.map((row) =>
          row.username === target.username ? { ...row, followedByMe: next.followedByMe } : row,
        ),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "フォローに失敗しました");
    }
  }

  return (
    <main className="page">
      <AppHeader
        user={user}
        onLogout={onLogout}
        onHome={onHome}
        onProfile={() => onProfile(user.username)}
        onSearch={onSearch}
      />
      <p className="back-row">
        <button type="button" className="btn link" onClick={() => onProfile(username)}>
          ← @{username}のプロフィール
        </button>
      </p>
      <section className="card">
        <div className="tabs" role="tablist">
          <button
            type="button"
            className={`tab${kind === "followees" ? " active" : ""}`}
            onClick={() => onFollows(username, "followees")}
          >
            フォロー中
          </button>
          <button
            type="button"
            className={`tab${kind === "followers" ? " active" : ""}`}
            onClick={() => onFollows(username, "followers")}
          >
            フォロワー
          </button>
        </div>
        {error ? <p className="err">{error}</p> : null}
        {loading ? <p className="empty">読み込み中…</p> : null}
        {!loading && users.length === 0 ? <p className="empty">まだいません</p> : null}
        {users.map((row) => (
          <div className="user-row" key={row.id}>
            <button type="button" className="btn link name-btn" onClick={() => onProfile(row.username)}>
              <span className="name">{row.displayName}</span>
              <span className="handle">@{row.username}</span>
            </button>
            {row.mine ? null : (
              <button type="button" className="btn ghost" onClick={() => void onToggle(row)}>
                {row.followedByMe ? "フォロー中" : "フォロー"}
              </button>
            )}
          </div>
        ))}
      </section>
    </main>
  );
}
