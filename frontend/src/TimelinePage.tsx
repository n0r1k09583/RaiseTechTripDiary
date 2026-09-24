import { useEffect, useRef, useState, type FormEvent } from "react";
import { createPost, deletePost, listPosts, type Post, type User, type VisitStatus } from "./api";
import { AREAS } from "./areas";
import { AppHeader } from "./AppHeader";
import { PhotoField } from "./PhotoField";
import { PostCard } from "./PostCard";

const PAGE = 20;
const REFRESH_MS = 30_000;
const IMAGE_MAX = 5 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

type Tab = "all" | "following" | "visited" | "want" | "photos";

type Props = {
  user: User;
  onLogout: () => void | Promise<void>;
  onEdit: (id: number) => void;
  onOpen: (id: number) => void;
  onProfile: (username: string) => void;
  onHome: () => void;
  onSearch: (q: string) => void;
};

export function TimelinePage({ user, onLogout, onEdit, onOpen, onProfile, onHome, onSearch }: Props) {
  const [tab, setTab] = useState<Tab>("all");
  const [area, setArea] = useState("");
  const [posts, setPosts] = useState<Post[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [spotName, setSpotName] = useState("");
  const [areaTag, setAreaTag] = useState("関東");
  const [visitStatus, setVisitStatus] = useState<VisitStatus>("visited");
  const [body, setBody] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<Post | null>(null);
  const [fresh, setFresh] = useState<Post[]>([]);
  const [notice, setNotice] = useState("");
  const sentinelRef = useRef<HTMLDivElement>(null);
  const loadingMoreRef = useRef(false);
  const postsRef = useRef<Post[]>([]);
  const hasMoreRef = useRef(false);
  const tabRef = useRef(tab);
  const areaRef = useRef(area);
  const bodyRef = useRef<HTMLTextAreaElement>(null);
  const noticeTimerRef = useRef(0);

  postsRef.current = posts;
  hasMoreRef.current = hasMore;
  tabRef.current = tab;
  areaRef.current = area;
  loadingMoreRef.current = loadingMore;

  useEffect(() => {
    let cancelled = false;
    setFresh([]);
    setLoading(true);
    setError("");
    listPosts({ tab, area: area || undefined, limit: PAGE })
      .then((res) => {
        if (cancelled) return;
        setPosts(res.posts);
        setHasMore(res.hasMore);
      })
      .catch((err) => {
        if (cancelled) return;
        const message = err instanceof Error ? err.message : "読み込みに失敗しました";
        if (message.includes("ログイン")) {
          void onLogout();
          return;
        }
        setError(message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [tab, area, onLogout]);

  useEffect(() => {
    return () => window.clearTimeout(noticeTimerRef.current);
  }, []);

  function showPostedNotice(message: string) {
    window.clearTimeout(noticeTimerRef.current);
    setNotice(message);
    noticeTimerRef.current = window.setTimeout(() => setNotice(""), 6000);
  }

  useEffect(() => {
    async function refreshQuietly() {
      if (document.visibilityState !== "visible") return;
      const currentTab = tabRef.current;
      const head = postsRef.current[0];
      if (!head) return;
      try {
        const res = await listPosts({
          tab: currentTab,
          area: areaRef.current || undefined,
          limit: PAGE,
          afterCreatedAt: head.createdAt,
          afterId: head.id,
        });
        if (res.posts.length === 0) return;
        if (window.scrollY < 80) {
          applyFresh(res.posts);
        } else {
          setFresh((prev) => mergeById(res.posts, prev));
        }
      } catch {
        // 一定間隔の取り直しは失敗しても画面に出さない
      }
    }

    const timer = window.setInterval(() => {
      void refreshQuietly();
    }, REFRESH_MS);

    function onVisible() {
      if (document.visibilityState === "visible") void refreshQuietly();
    }
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, []);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (!entries[0]?.isIntersecting) return;
        void loadOlder();
      },
      { rootMargin: "240px" },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [tab, area, posts.length, hasMore, loading]);

  function applyFresh(incoming: Post[]) {
    setPosts((prev) => mergeById(incoming, prev));
    setFresh([]);
  }

  function showFresh() {
    applyFresh(fresh);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function loadOlder() {
    if (loading || loadingMoreRef.current || !hasMoreRef.current) return;
    const last = postsRef.current[postsRef.current.length - 1];
    if (!last) return;
    loadingMoreRef.current = true;
    setLoadingMore(true);
    try {
      const res = await listPosts({
        tab: tabRef.current,
        area: areaRef.current || undefined,
        limit: PAGE,
        beforeCreatedAt: last.createdAt,
        beforeId: last.id,
      });
      const existing = new Set(postsRef.current.map((p) => p.id));
      const added = res.posts.filter((p) => !existing.has(p.id));
      setPosts((prev) => {
        const ids = new Set(prev.map((p) => p.id));
        return [...prev, ...res.posts.filter((p) => !ids.has(p.id))];
      });
      setHasMore(res.hasMore);
      if (added.length > 0) {
        showPostedNotice(`記録が増えました（続きを${added.length}件読み込みました）`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "続きの読み込みに失敗しました");
    } finally {
      loadingMoreRef.current = false;
      setLoadingMore(false);
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const name = spotName.trim();
    const text = body.trim();
    setError("");
    if (!name || name.length > 40) {
      setError("場所名は1〜40文字です");
      return;
    }
    if (text.length > 280) {
      setError("本文は1〜280文字です");
      return;
    }
    if (!text && !image) {
      setError("感想か写真のどちらかが必要です");
      return;
    }
    if (image && !IMAGE_TYPES.includes(image.type)) {
      setError("JPEG / PNG / WebP のみです");
      return;
    }
    if (image && image.size > IMAGE_MAX) {
      setError("画像は5MBまでです");
      return;
    }
    setBusy(true);
    try {
      const created = await createPost({ spotName: name, areaTag, visitStatus, body: text, image });
      setPosts((prev) => [created, ...prev.filter((p) => p.id !== created.id)]);
      setFresh((prev) => prev.filter((p) => p.id !== created.id));
      setSpotName("");
      setBody("");
      setImage(null);
      setTab("all");
      showPostedNotice(image ? "写真をみんなと共有しました" : "記録しました");
    } catch (err) {
      setError(err instanceof Error ? err.message : "投稿に失敗しました");
    } finally {
      setBusy(false);
    }
  }

  async function onConfirmDelete() {
    if (!confirm) return;
    const target = confirm;
    setConfirm(null);
    try {
      await deletePost(target.id);
      setPosts((prev) => prev.filter((p) => p.id !== target.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "削除に失敗しました");
    }
  }

  const emptyMessage =
    tab === "following"
      ? "フォロー中の記録はまだありません。プロフィールからフォローできます"
      : tab === "visited"
        ? "訪問済みの記録はまだありません"
        : tab === "want"
          ? "行きたいリストはまだ空です。♡行きたい を押すとここに入ります"
          : tab === "photos"
            ? "写真つきの記録はまだありません。上から写真を投稿できます"
            : "まだ旅の記録はありません。";

  return (
    <main className="page">
      <AppHeader
        user={user}
        onLogout={onLogout}
        onHome={onHome}
        onProfile={() => onProfile(user.username)}
        onSearch={onSearch}
      />
      {notice ? (
        <p className="notice-bar" role="status">
          {notice}
        </p>
      ) : null}
      {fresh.length > 0 ? (
        <button type="button" className="fresh-bar" onClick={showFresh}>
          新着の記録が{fresh.length}件あります
        </button>
      ) : null}
      <section className="card feed-card">
        <div className="feed-head">
          <h1>みんなの旅</h1>
          <p className="lead">
            写真と感想をみんなと共有して、次の行き先を見つけます。
            <button type="button" className="btn link" onClick={() => bodyRef.current?.focus()}>
              記録する
            </button>
          </p>
          <form onSubmit={(e) => void onSubmit(e)}>
            <PhotoField id="image" file={image} onChange={setImage} />
            <div className="composer-grid">
              <div>
                <label htmlFor="spotName">場所名</label>
                <input
                  id="spotName"
                  value={spotName}
                  maxLength={40}
                  placeholder="例: 函館山"
                  onChange={(e) => setSpotName(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="areaTag">エリア</label>
                <select id="areaTag" value={areaTag} onChange={(e) => setAreaTag(e.target.value)}>
                  {AREAS.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <label htmlFor="visitStatus">記録の種類</label>
            <select
              id="visitStatus"
              value={visitStatus}
              onChange={(e) => setVisitStatus(e.target.value as VisitStatus)}
            >
              <option value="visited">訪問済み</option>
              <option value="want">行きたい</option>
            </select>
            <label htmlFor="body">感想（写真だけでも可）</label>
            <textarea
              id="body"
              ref={bodyRef}
              placeholder="景色、ご飯、次に行きたいこと…"
              value={body}
              maxLength={280}
              onChange={(e) => setBody(e.target.value)}
            />
            <p className={`counter${body.length > 280 ? " over" : ""}`}>{body.length} / 280</p>
            <div className="err">{error}</div>
            <div className="row-actions">
              <button className="btn" type="submit" disabled={busy}>
                みんなと共有する
              </button>
            </div>
          </form>
        </div>
        <div className="filters">
          <label htmlFor="areaFilter">エリアで絞る</label>
          <select id="areaFilter" value={area} onChange={(e) => setArea(e.target.value)}>
            <option value="">すべてのエリア</option>
            {AREAS.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </div>
        <div className="tabs" role="tablist">
          <button type="button" className={`tab${tab === "all" ? " active" : ""}`} onClick={() => setTab("all")}>
            すべて
          </button>
          <button
            type="button"
            className={`tab${tab === "following" ? " active" : ""}`}
            onClick={() => setTab("following")}
          >
            フォロー中
          </button>
          <button
            type="button"
            className={`tab${tab === "visited" ? " active" : ""}`}
            onClick={() => setTab("visited")}
          >
            訪問済み
          </button>
          <button type="button" className={`tab${tab === "want" ? " active" : ""}`} onClick={() => setTab("want")}>
            行きたい
          </button>
          <button
            type="button"
            className={`tab${tab === "photos" ? " active" : ""}`}
            onClick={() => setTab("photos")}
          >
            写真
          </button>
        </div>
        <div>
          {loading && posts.length === 0 ? <p className="empty">読み込み中…</p> : null}
          {!loading && posts.length === 0 ? <p className="empty">{emptyMessage}</p> : null}
          {posts.map((item) => (
            <PostCard
              key={item.id}
              post={item}
              onOpen={onOpen}
              onEdit={onEdit}
              onDelete={setConfirm}
              onProfile={onProfile}
              onLiked={(next) =>
                setPosts((prev) => prev.map((row) => (row.id === next.id ? next : row)))
              }
              onError={setError}
            />
          ))}
          <div ref={sentinelRef} className="scroll-sentinel" />
          {loadingMore ? <p className="empty">続きを読み込み中…</p> : null}
        </div>
      </section>
      {confirm ? (
        <div className="modal-bg show">
          <div className="modal">
            <h2>この記録を削除しますか？</h2>
            <p className="lead">削除すると元に戻せません。</p>
            <div className="row-actions">
              <button type="button" className="btn ghost" onClick={() => setConfirm(null)}>
                キャンセル
              </button>
              <button type="button" className="btn danger" onClick={() => void onConfirmDelete()}>
                削除する
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

function mergeById(incoming: Post[], prev: Post[]): Post[] {
  const ids = new Set(incoming.map((p) => p.id));
  return [...incoming, ...prev.filter((p) => !ids.has(p.id))];
}
