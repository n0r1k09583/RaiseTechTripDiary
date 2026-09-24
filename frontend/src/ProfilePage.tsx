import { useEffect, useState } from "react";
import {
  followUser,
  getProfile,
  listUserPosts,
  unfollowUser,
  type Post,
  type Profile,
  type User,
} from "./api";
import { AppHeader } from "./AppHeader";
import { PostCard } from "./PostCard";

type Props = {
  user: User;
  username: string;
  onLogout: () => void | Promise<void>;
  onHome: () => void;
  onProfile: (username: string) => void;
  onSearch: (q: string) => void;
  onOpen: (id: number) => void;
  onEdit: (id: number) => void;
  onFollows: (username: string, kind: "followees" | "followers") => void;
};

export function ProfilePage({
  user,
  username,
  onLogout,
  onHome,
  onProfile,
  onSearch,
  onOpen,
  onEdit,
  onFollows,
}: Props) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    Promise.all([getProfile(username), listUserPosts(username)])
      .then(([next, listed]) => {
        if (cancelled) return;
        setProfile(next);
        setPosts(listed.posts);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "読み込みに失敗しました");
        setProfile(null);
        setPosts([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [username]);

  async function onToggle() {
    if (!profile || profile.mine) return;
    setBusy(true);
    setError("");
    try {
      setProfile(profile.followedByMe ? await unfollowUser(username) : await followUser(username));
    } catch (err) {
      setError(err instanceof Error ? err.message : "フォローに失敗しました");
    } finally {
      setBusy(false);
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
        <button type="button" className="btn link" onClick={onHome}>
          ← みんなの旅
        </button>
      </p>
      {loading ? <p className="empty">読み込み中…</p> : null}
      {!loading && !profile ? <p className="empty">{error || "ユーザーが見つかりません"}</p> : null}
      {profile ? (
        <section className="card">
          <h1>{profile.displayName}</h1>
          <p className="lead">@{profile.username}</p>
          <div className="counts">
            <button type="button" className="btn link" onClick={() => onFollows(username, "followees")}>
              フォロー {profile.followingCount}
            </button>
            <button type="button" className="btn link" onClick={() => onFollows(username, "followers")}>
              フォロワー {profile.followerCount}
            </button>
          </div>
          {profile.mine ? null : (
            <button type="button" className="btn" disabled={busy} onClick={() => void onToggle()}>
              {profile.followedByMe ? "フォロー中" : "フォロー"}
            </button>
          )}
          {error && profile ? <p className="err">{error}</p> : null}
        </section>
      ) : null}
      {profile
        ? posts.map((item) => (
            <PostCard
              key={item.id}
              post={item}
              onOpen={onOpen}
              onEdit={item.mine ? onEdit : undefined}
              onProfile={onProfile}
              onLiked={(next) =>
                setPosts((prev) => prev.map((row) => (row.id === next.id ? next : row)))
              }
              onError={setError}
            />
          ))
        : null}
      {profile && !loading && posts.length === 0 ? <p className="empty">まだ投稿はありません。</p> : null}
    </main>
  );
}
