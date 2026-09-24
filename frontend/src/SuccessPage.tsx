import type { User } from "./api";

type Props = {
  user: User;
  onLogout: () => void | Promise<void>;
};

export function SuccessPage({ user, onLogout }: Props) {
  return (
    <main className="wrap">
      <p className="brand">
        TripDiary
        <span className="brand-sub">旅の記録</span>
      </p>
      <section className="card">
        <h1>ログイン成功</h1>
        <p className="lead">
          {user.displayName}（@{user.username}）としてログインしています。
        </p>
        <div className="row-actions">
          <button className="btn ghost" id="logout-btn" type="button" onClick={onLogout}>
            ログアウト
          </button>
        </div>
      </section>
    </main>
  );
}
