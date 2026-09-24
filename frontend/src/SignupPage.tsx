import { useState, type FormEvent } from "react";
import { signup, type User } from "./api";

type AuthOk = { accessToken?: string; refreshToken?: string; token?: string; user: User };

type Props = {
  onSuccess: (res: AuthOk) => void;
  onGoLogin: () => void;
};

export function SignupPage({ onSuccess, onGoLogin }: Props) {
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const res = await signup({ username, displayName, email, password, confirm });
      onSuccess(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登録に失敗しました");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="wrap">
      <p className="brand">
        TripDiary <span className="sky-bird" aria-hidden="true">✈</span>
        <span className="brand-sub">旅の記録</span>
      </p>
      <p className="lead">
        旅の記録にアカウントを作ります。行った場所と行きたい場所は登録後に残せます。
      </p>
      <section className="card">
        <h1>新規登録</h1>
        <form onSubmit={onSubmit}>
          <label htmlFor="username">ユーザー名</label>
          <input
            id="username"
            value={username}
            placeholder="yamada_1"
            autoComplete="username"
            onChange={(e) => setUsername(e.target.value)}
          />
          <p className="hint">3〜20文字。半角英小文字・数字・_</p>
          <label htmlFor="displayName">表示名</label>
          <input
            id="displayName"
            value={displayName}
            maxLength={20}
            autoComplete="nickname"
            onChange={(e) => setDisplayName(e.target.value)}
          />
          <p className="counter">
            {displayName.length} / 20
          </p>
          <label htmlFor="email">メールアドレス</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <label htmlFor="password">パスワード</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <label htmlFor="confirm">パスワード確認</label>
          <input
            id="confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
          <div className="err">{error}</div>
          <div className="row-actions">
            <button className="btn" type="submit" disabled={busy}>
              登録する
            </button>
          </div>
        </form>
        <p className="hint">
          すでにアカウントがある人は{" "}
          <a className="btn link" href="/login" onClick={(e) => { e.preventDefault(); onGoLogin(); }}>
            ログイン
          </a>
        </p>
      </section>
    </main>
  );
}
