import { useState, type FormEvent } from "react";
import { login, type User } from "./api";

type AuthOk = { accessToken?: string; refreshToken?: string; token?: string; user: User };

type Props = {
  onSuccess: (res: AuthOk) => void;
  onGoSignup: () => void;
};

const DEMOS = [
  { email: "yamada@example.com", label: "@yamada" },
  { email: "hanako@example.com", label: "@hanako" },
  { email: "ichiro@example.com", label: "@ichiro" },
];

export function LoginPage({ onSuccess, onGoSignup }: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function fill(demoEmail: string) {
    setEmail(demoEmail);
    setPassword("password123");
    setError("");
    void submit(demoEmail, "password123");
  }

  async function submit(nextEmail: string, nextPassword: string) {
    if (!nextEmail || !nextPassword) {
      setError("メールアドレスとパスワードを入力してください");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const res = await login(nextEmail, nextPassword);
      onSuccess(res);
    } catch (err) {
      const message = err instanceof Error ? err.message : "ログインに失敗しました";
      const retry = /接続|起動/.test(message);
      if (retry) {
        for (let i = 0; i < 4; i += 1) {
          await new Promise((resolve) => window.setTimeout(resolve, 700));
          try {
            const res = await login(nextEmail, nextPassword);
            onSuccess(res);
            return;
          } catch (again) {
            if (i === 3) {
              setPassword("");
              setError(again instanceof Error ? again.message : "ログインに失敗しました");
              return;
            }
          }
        }
      }
      setPassword("");
      setError(message);
    } finally {
      setBusy(false);
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    await submit(email, password);
  }

  return (
    <main className="wrap">
      <p className="brand">
        TripDiary <span className="sky-bird" aria-hidden="true">✈</span>
        <span className="brand-sub">旅の記録</span>
      </p>
      <p className="lead">
        行った場所と行きたい場所を、写真と感想で残すアプリです。投稿・コメントはログイン後だけ使えます。
      </p>
      <section className="card">
        <h1>ログイン</h1>
        <p className="demo">
          試すアカウント（パスワードは password123）
          <br />
          {DEMOS.map((demo) => (
            <button key={demo.email} type="button" className="btn ghost" onClick={() => fill(demo.email)}>
              {demo.label}
            </button>
          ))}
        </p>
        <form onSubmit={onSubmit}>
          <label htmlFor="email">メールアドレス</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <label htmlFor="password">パスワード</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <div className="err">{error}</div>
          <div className="row-actions">
            <button className="btn" type="submit" disabled={busy}>
              ログイン
            </button>
          </div>
        </form>
        <p className="hint">
          初めての人は{" "}
          <a className="btn link" href="/signup" onClick={(e) => { e.preventDefault(); onGoSignup(); }}>
            新規登録
          </a>
        </p>
      </section>
    </main>
  );
}
