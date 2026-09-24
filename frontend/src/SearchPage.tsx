import { useEffect, useState, type FormEvent, type KeyboardEvent } from "react";
import { searchUsers, type User, type UserSummary } from "./api";
import { AppHeader } from "./AppHeader";
import { isImeConfirmKey, normalizeUserQuery, queryFromSearchForm } from "./searchQuery";

type Props = {
  user: User;
  q: string;
  onLogout: () => void | Promise<void>;
  onHome: () => void;
  onProfile: (username: string) => void;
  onSearch: (q: string) => void;
};

export function SearchPage({ user, q, onLogout, onHome, onProfile, onSearch }: Props) {
  const needle = normalizeUserQuery(q);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [error, setError] = useState("");
  const [fetchedFor, setFetchedFor] = useState("");
  const [loading, setLoading] = useState(() => Boolean(needle));
  const [draft, setDraft] = useState(q);
  const pending = Boolean(needle) && (loading || fetchedFor !== needle);

  useEffect(() => {
    setDraft(q);
  }, [q]);

  useEffect(() => {
    const current = normalizeUserQuery(q);
    if (!current) {
      setUsers([]);
      setError("");
      setFetchedFor("");
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError("");

    async function load() {
      try {
        let res: { users: UserSummary[] };
        try {
          res = await searchUsers(current);
        } catch {
          res = await searchUsers(current);
        }
        if (!cancelled) {
          setUsers(res.users);
          setFetchedFor(current);
        }
      } catch (err) {
        if (!cancelled) {
          setUsers([]);
          setFetchedFor(current);
          setError(err instanceof Error ? err.message : "検索に失敗しました");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [q]);

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const next = queryFromSearchForm(e.currentTarget);
    if (!next) {
      return;
    }
    onSearch(next);
  }

  function onSearchKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (isImeConfirmKey(e)) {
      e.preventDefault();
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
        searchQuery={needle}
      />
      <section className="card">
        <h1>ユーザー検索</h1>
        <form className="search-page-form" onSubmit={submit} role="search">
          <label htmlFor="search-q">ユーザー名</label>
          <input
            id="search-q"
            type="search"
            name="q"
            autoComplete="off"
            enterKeyHint="search"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onSearchKeyDown}
            placeholder="@yamada / 山田 / ヤマダ"
          />
          <button className="btn" type="submit">
            検索
          </button>
        </form>
        {!needle ? <p className="empty">ユーザー名を入力して検索してください。@付きや表示名でも探せます。</p> : null}
        {pending ? <p className="empty">読み込み中…</p> : null}
        {needle && !pending ? (
          <p className="lead" aria-live="polite">
            「{needle}」の検索結果 {users.length}件
          </p>
        ) : null}
        {error && fetchedFor === needle ? <p className="err">{error}</p> : null}
        {needle && !pending && users.length === 0 && !error ? (
          <p className="empty">該当するユーザーはいません。ユーザー名の一部か表示名で試してください。</p>
        ) : null}
        {!pending && fetchedFor === needle
          ? users.map((row) => (
              <div className="user-row" key={row.id}>
                <button type="button" className="btn link name-btn" onClick={() => onProfile(row.username)}>
                  <span className="name">{row.displayName}</span>
                  <span className="handle">@{row.username}</span>
                </button>
                <button type="button" className="btn ghost" onClick={() => onProfile(row.username)}>
                  プロフィール
                </button>
              </div>
            ))
          : null}
      </section>
    </main>
  );
}
