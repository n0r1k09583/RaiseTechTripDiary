import { useEffect, useState, type FormEvent, type KeyboardEvent } from "react";
import type { User } from "./api";
import { isImeConfirmKey, queryFromSearchForm } from "./searchQuery";

type Props = {
  user: User;
  onLogout: () => void | Promise<void>;
  onHome?: () => void;
  onProfile?: () => void;
  onSearch?: (q: string) => void;
  searchQuery?: string;
};

export function AppHeader({ user, onLogout, onHome, onProfile, onSearch, searchQuery = "" }: Props) {
  const [q, setQ] = useState(searchQuery);

  useEffect(() => {
    setQ(searchQuery);
  }, [searchQuery]);

  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const needle = queryFromSearchForm(e.currentTarget);
    if (!needle) {
      return;
    }
    onSearch?.(needle);
  }

  function onSearchKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (isImeConfirmKey(e)) {
      e.preventDefault();
    }
  }

  return (
    <header className="topbar">
      <button type="button" className="brand brand-btn" onClick={onHome}>
        TripDiary <span className="sky-bird" aria-hidden="true">✈</span>
        <span className="brand-sub">旅の記録</span>
      </button>
      {onSearch ? (
        <form className="search-form" onSubmit={submit} role="search">
          <label className="sr-only" htmlFor="user-search">
            ユーザー名で検索
          </label>
          <input
            id="user-search"
            type="search"
            name="q"
            autoComplete="off"
            enterKeyHint="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={onSearchKeyDown}
            placeholder="@yamada / 山田 / ヤマダ"
          />
          <button className="btn ghost" type="submit">
            検索
          </button>
        </form>
      ) : null}
      <div className="topbar-right">
        {onProfile ? (
          <button type="button" className="chip chip-btn" onClick={onProfile}>
            {user.displayName}
          </button>
        ) : (
          <span className="chip">{user.displayName}</span>
        )}
        <button type="button" className="btn ghost" id="logout-btn" onClick={onLogout}>
          ログアウト
        </button>
      </div>
    </header>
  );
}
