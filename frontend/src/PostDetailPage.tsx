import { useEffect, useState, type FormEvent } from "react";
import {
  createComment,
  deleteComment,
  getPost,
  listComments,
  type Comment,
  type Post,
  type User,
} from "./api";
import { AppHeader } from "./AppHeader";
import { formatTime } from "./formatTime";
import { MapPicker } from "./MapPicker";
import { PostCard } from "./PostCard";

type Props = {
  user: User;
  postId: number;
  onLogout: () => void | Promise<void>;
  onBack: () => void;
  onEdit: (id: number) => void;
  onProfile: (username: string) => void;
  onSearch: (q: string) => void;
};

export function PostDetailPage({ user, postId, onLogout, onBack, onEdit, onProfile, onSearch }: Props) {
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [body, setBody] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<Comment | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    Promise.all([getPost(postId), listComments(postId)])
      .then(([nextPost, listed]) => {
        if (cancelled) return;
        setPost(nextPost);
        setComments(listed.comments);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "読み込みに失敗しました");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [postId]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const text = body.trim();
    setError("");
    if (!text || text.length > 140) {
      setError("コメントは1〜140文字です");
      return;
    }
    setBusy(true);
    try {
      const created = await createComment(postId, text);
      setComments((prev) => [...prev, created]);
      setPost((prev) => (prev ? { ...prev, commentCount: prev.commentCount + 1 } : prev));
      setBody("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "送信に失敗しました");
    } finally {
      setBusy(false);
    }
  }

  async function onDelete() {
    if (!confirm) return;
    const target = confirm;
    setConfirm(null);
    try {
      await deleteComment(target.id);
      setComments((prev) => prev.filter((item) => item.id !== target.id));
      setPost((prev) => (prev ? { ...prev, commentCount: Math.max(0, prev.commentCount - 1) } : prev));
    } catch (err) {
      setError(err instanceof Error ? err.message : "削除に失敗しました");
    }
  }

  return (
    <main className="page">
      <AppHeader
        user={user}
        onLogout={onLogout}
        onHome={onBack}
        onProfile={() => onProfile(user.username)}
        onSearch={onSearch}
      />
      <p className="back-row">
        <button type="button" className="btn link" onClick={onBack}>
          ← みんなの記録
        </button>
      </p>
      {loading ? <p className="empty">読み込み中…</p> : null}
      {!loading && !post ? <p className="empty">{error || "投稿が見つかりません"}</p> : null}
      {post ? (
        <article className="card post-detail">
          <PostCard
            post={post}
            onEdit={post.mine ? onEdit : undefined}
            onProfile={onProfile}
            onLiked={setPost}
            onFavorited={setPost}
            onError={setError}
            large
          />
          {post.latitude != null && post.longitude != null ? (
            <MapPicker latitude={post.latitude} longitude={post.longitude} readOnly />
          ) : null}
        </article>
      ) : null}

      {post ? (
        <section className="card">
          <h1>コメント</h1>
          {comments.length === 0 ? (
            <p className="empty">まだコメントはありません（コメント 0件）</p>
          ) : (
            comments.map((comment) => (
              <article className="comment" key={comment.id}>
                <div className="avatar">{(comment.displayName || comment.username).slice(0, 1)}</div>
                <div>
                  <div>
                    <button type="button" className="btn link name-btn" onClick={() => onProfile(comment.username)}>
                      <span className="name">{comment.displayName}</span>
                      <span className="handle">@{comment.username}</span>
                    </button>
                    <span className="meta"> · {formatTime(comment.createdAt)}</span>
                    {comment.mine ? (
                      <>
                        {" "}
                        <button type="button" className="btn link" onClick={() => setConfirm(comment)}>
                          削除
                        </button>
                      </>
                    ) : null}
                  </div>
                  <p className="body">{comment.body}</p>
                </div>
              </article>
            ))
          )}
          {post.visibility === "private" ? (
            <p className="empty">非公開の記録には、いいねとコメントは付きません。</p>
          ) : (
          <form onSubmit={onSubmit}>
            <label htmlFor="comment-body">コメントを書く</label>
            <textarea
              id="comment-body"
              value={body}
              maxLength={140}
              placeholder="行き方の質問や、おすすめをどうぞ"
              onChange={(e) => setBody(e.target.value)}
            />
            <p className={`counter${body.length > 140 ? " over" : ""}`}>{body.length} / 140</p>
            <div className="err">{error}</div>
            <div className="row-actions">
              <button className="btn" type="submit" disabled={busy}>
                送信
              </button>
            </div>
          </form>
          )}
        </section>
      ) : null}

      {confirm ? (
        <div className="modal-bg show">
          <div className="modal">
            <h2>このコメントを削除しますか？</h2>
            <p className="lead">削除すると元に戻せません。</p>
            <div className="row-actions">
              <button type="button" className="btn ghost" onClick={() => setConfirm(null)}>
                キャンセル
              </button>
              <button type="button" className="btn danger" onClick={() => void onDelete()}>
                削除する
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}
