# 開発開始後の仕様追加・変更

認証の実装を始めたあとで決めたことと、プログラムに足したことを残す資料である。最初の要件定義・機能定義を置き換えるものではなく、**後から足した判断**を追えるようにする。

日付: 2026-08-18

関連: [機能定義書](./FEATURES/機能定義書.md) F-02、[画面仕様書](./screens.md) S-03、[要件定義書](./requirements.md) 受け入れ基準、[登録・ログイン計画](./auth-plan.md)（当初計画）

---

## なぜ別資料にするか

開発開始時点の計画（`auth-plan.md`）は次だった。

- ログイン成功後は **Hello World（ログイン成功）の仮画面**
- 投稿・タイムラインはまだ作らない
- 投稿の続きは「最初は最新50件。それ以上は後からページネーションを足してよい」
- 投稿削除は「任意。実装時に決める」

実装に入ったあと、次が必要だと分かった。仕様書だけ直してプログラムに無い、またはプログラムだけあって仕様に無い、という状態にしない。

---

## 決めたことと、プログラムへの反映

| # | 開発開始後に決めたこと | 仕様 | プログラム |
|---|------------------------|------|------------|
| 1 | ログイン成功後は仮画面ではなく **タイムライン** を出す | F-02、S-03。auth-plan の Hello World は廃止 | `frontend/src/App.tsx` が成功後に `TimelinePage` を出す |
| 2 | タイムラインを出すために、投稿の **作成・表示** が要る | F-02 投稿作成、S-03 投稿欄 | `POST /api/posts`、タイムライン上の投稿欄 |
| 3 | 投稿機能は作成だけで止めず、**編集・削除** まで入れる（自分の投稿だけ） | F-02 投稿編集・投稿削除。F-01 の「削除は任意」は撤回 | `PATCH` / `DELETE /api/posts/{id}`。カードの編集・削除、確認ダイアログ |
| 4 | 「もっと見る」ボタンは使わない。続きは **無限スクロール** | F-02 並び順・読み方、S-03 読み方、受け入れ基準 | `GET /api/posts?beforeCreatedAt=&beforeId=`。画面下の sentinel で追加取得 |
| 5 | 自分の投稿・編集・削除は **押した直後** に一覧へ出す | F-02 最新の取り方、S-03 | 作成成功で先頭へ挿入。編集から戻ると再取得。削除は一覧から外す |
| 6 | 他人の作成・編集・削除は **一件ごとに通知しない**。WebSocket の都度プッシュは使わない | F-02、要件定義のスコープ外 | WebSocket なし。トースト・件数バッジなし |
| 7 | 他人の最新は **一定間隔（約30秒）**、またはタブに戻ったときに静かに取り直す | F-02「約30秒」「タブに戻ったとき」 | `TimelinePage` の `REFRESH_MS = 30_000` と `visibilitychange` |
| 8 | 画像は任意で1枚（JPEG / PNG / WebP、5MB） | F-05 | multipart の `image`、`uploads/`、`/uploads/**` |

英語での依頼の意味（実装判断の原文）:

- Timeline after login needs post create, or the feed is empty.
- Include the whole post function: create, edit, delete.
- A “read more” button is a poor SNS experience. Use infinite scroll.
- Show the new post on the timeline as soon as you post it.
- Do not use WebSocket to notify every create / update / delete. With many users that floods notifications. Refresh at the right time (your own action) or after a quiet interval.

---

## プログラムに足したもの（この変更で）

### API（要ログイン。Bearer）

| 方法 | パス | 内容 |
|------|------|------|
| GET | `/api/posts` | 新しい順。`tab=all\|following`。`limit` 既定20・最大50。古い続きは `beforeCreatedAt` + `beforeId`。新しい差分は `afterCreatedAt` + `afterId`。応答 `{ posts, hasMore }` |
| GET | `/api/posts/{id}` | 1件。編集画面用 |
| POST | `/api/posts` | 作成。multipart。`body` 必須、`image` 任意。201 |
| PATCH | `/api/posts/{id}` | 自分の投稿だけ編集。403 / 404 |
| DELETE | `/api/posts/{id}` | 自分の投稿だけ削除。204 |

フォロー表はまだ無い。「フォロー中」タブは後続として空案内を出す。いま見るのは **すべて** の全投稿。

### 画面

| 画面 | ファイル | 動き |
|------|----------|------|
| タイムライン | `frontend/src/TimelinePage.tsx` | 投稿欄、すべて／フォロー中（後続案内）、無限スクロール、投稿成功と続き読み込みの「投稿されました」、30秒の静かな取り直し、自分の削除確認 |
| 投稿を編集 | `frontend/src/EditPage.tsx` | 自分の投稿だけ。保存後はタイムラインへ戻る |

### データ

Flyway `V3__create_posts.sql`（`posts`）、`V4__create_comments.sql`（`comments`）。画像ファイルは `uploads/`（Git に入れない）。DB にはファイル名だけ持つ。

コメント件数は `posts` の SELECT に相関サブクエリを乗せる。投稿ごとに comments を問い合わせない。

### コメント API（要ログイン。Bearer）

| 方法 | パス | 内容 |
|------|------|------|
| GET | `/api/posts/{id}/comments` | 古い順。応答 `{ comments }` |
| POST | `/api/posts/{id}/comments` | 作成。JSON `{ body }`。1〜140文字。201 |
| DELETE | `/api/comments/{id}` | 自分のコメントだけ削除。204 |

画面: `frontend/src/PostDetailPage.tsx`。タイムラインの本文・画像・「コメント n件」から開く。

---

## 2026-09-14 いいね・フォロー・検索

Flyway `V5` likes、`V6` follows。件数は投稿と同じ SELECT。フォロー中は IN サブクエリ1回。

| 方法 | パス | 内容 |
|------|------|------|
| POST | `/api/posts/{id}/likes` | いいねのトグル。応答は投稿（`likeCount` / `likedByMe`） |
| GET | `/api/users?q=` | ユーザー名の部分一致。空は空配列 |
| GET | `/api/users/{username}` | プロフィール（email なし） |
| GET | `/api/users/{username}/posts` | その人の投稿 |
| GET | `/api/users/{username}/followees` | フォロー中 |
| GET | `/api/users/{username}/followers` | フォロワー |
| POST / DELETE | `/api/users/{username}/follow` | フォロー／解除。自分は 400 |

画面: プロフィール、フォロー一覧、検索、ヘッダー検索、いいねボタン、「フォロー中」タブ。

CI: `.github/workflows/ci.yml`（backend `mvnw test`、frontend `tsc` + `npm test`。k6 / E2E なし）

---

## 今回のプログラムにまだ入れないもの

プロフィール画像、WebSocket、AWS の常時公開。  
S3 は差し替え口のみ。提出の動作確認はローカルで足りる。

**これから**は [next-lesson.md](./next-lesson.md)。提出 URL が要るときだけ Terraform を短時間 apply してすぐ destroy。

---

## 確認の目安

1. ログインするとタイムラインが出る（ログイン成功の仮画面は出ない）
2. 投稿すると、再読み込みボタンなしで先頭に出る。「投稿されました」と出る
3. 自分の投稿は編集・削除できる。他人の投稿の編集・削除は 403
4. 下へスクロールすると古い投稿が続く。「もっと見る」は無い。続きが載ると「投稿されました」と出る
5. 他アカウントの投稿は、約30秒後またはタブに戻ったときに一覧へ混ざる。通知は出ない
6. 投稿詳細でコメントを書け、件数がタイムラインに反映される。自分のコメントだけ削除できる
7. ハートでいいね／取り消し。件数がカードに出る
8. ユーザー名からプロフィールへ進み、フォローできる。「フォロー中」に相手の投稿が出る
9. ヘッダー検索でユーザー名の一部を入れてプロフィールへ進める
