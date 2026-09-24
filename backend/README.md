# バックエンド（認証と投稿）

いま実装しているのは **ユーザー登録・ログイン・JWT**、**投稿（一覧・作成・編集・削除）**、**コメント**。いいね操作・フォロー・検索はまだ。開発開始後の判断は `docs/dev-changes.md`。API 仕様の自動生成は `docs/openapi.md`。

## 起動

```powershell
cd backend
.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run
```

http://127.0.0.1:8080/api/health

API 仕様書（コードから自動生成。コントローラを直して再起動すると更新される）:

- OpenAPI JSON: http://127.0.0.1:8080/v3/api-docs
- Swagger UI: http://127.0.0.1:8080/swagger-ui.html

JWT の秘密鍵は `.env` の `JWT_SECRET`。Git に書かない。

## ログ

起動すると `logs/` にファイルが出る（Git に入れない）。設計は `docs/logging.md`。

- `application.log` / `error.log` … JSON（`traceId`・`userId`・`duration_ms` など）
- `access.*.log` … Tomcat のアクセスログ（テキスト）
- パスワード・JWT は出さない。Datadog は入れない

## テスト

本番の `data/tripdiary.db` には書かない。テストはメモリの H2（SQLite 互換）。考え方は `docs/testing.md`（ブラックボックス／ホワイトボックス、境界値）。

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm test
```

E2E はしない。

## API

| 方法   | パス              | 認証   | 内容                                                                    |
| ------ | ----------------- | ------ | ----------------------------------------------------------------------- |
| POST   | `/api/signup`     | 不要   | 登録して JWT を返す                                                     |
| POST   | `/api/login`      | 不要   | ログインして JWT を返す                                                 |
| POST   | `/api/refresh`    | 不要   | リフレッシュトークンを回す                                              |
| POST   | `/api/logout`     | 不要   | リフレッシュを破棄。204                                                 |
| GET    | `/api/me`         | Bearer | 今のユーザー。トークンが無い／壊れていると 401                          |
| GET    | `/api/health`     | 不要   | 起動確認                                                                |
| GET    | `/api/posts`                    | Bearer | タイムライン。新しい順。無限スクロール用の `before*`、差分用の `after*` |
| GET    | `/api/posts/{id}`               | Bearer | 1件                                                                     |
| POST   | `/api/posts`                    | Bearer | 作成（multipart。本文必須、画像任意）                                   |
| PATCH  | `/api/posts/{id}`               | Bearer | 自分の投稿だけ編集                                                      |
| DELETE | `/api/posts/{id}`               | Bearer | 自分の投稿だけ削除。204                                                 |
| GET    | `/api/posts/{id}/comments`      | Bearer | コメント一覧。古い順                                                    |
| POST   | `/api/posts/{id}/comments`      | Bearer | コメント作成。JSON `{ body }`。201                                      |
| DELETE | `/api/comments/{id}`            | Bearer | 自分のコメントだけ削除。204                                             |

ログイン・登録の成功レスポンス:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "token": "eyJ...",
  "user": {
    "id": 1,
    "email": "...",
    "username": "yamada",
    "displayName": "山田"
  }
}
```

画面は `Authorization: Bearer <accessToken>` を付ける。パスワードは `password_digest` としてハッシュ保存し、レスポンスに出さない。

デモユーザー（空の DB のときだけ入れる）: `yamada@example.com` / `hanako@example.com` / `ichiro@example.com`、パスワード `password123`。

## 認証と認可

- **認証**: メール＋パスワードが正しいか。正しければ JWT を発行する
- **認可**: トークンがある人だけ投稿できる。他人の投稿の編集・削除は 403

最新の取り方: 自分の操作は直後に画面へ出す。他人の変更は WebSocket で一件通知せず、画面が約30秒おき（またはタブ復帰時）に取り直す。

## コメント・いいねと N+1

N+1 問題: 親を 1 回取ったあと、各レコードごとに子を取る非効率なアクセス。

投稿一覧 50 件のあと、1 件ずつ「いいね数」「コメント数」を取るとクエリが 100 回増える。

対処:

1. **サブクエリ / JOIN で一括** … 投稿と件数を 1 本の SQL で取る
2. **バッチ** … `WHERE post_id IN (...)` で件数をまとめて取り、メモリでくっつける

いまの一覧は同じ SELECT にコメント件数といいね件数（`liked_by_me` 含む）のサブクエリを乗せている。投稿ごとに問い合わせない。
