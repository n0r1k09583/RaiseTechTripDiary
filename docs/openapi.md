# API 仕様書（OpenAPI / Swagger）

別チームが画面と API を同時に作るときの **インターフェース定義** と、コードから仕様書を自動生成する仕組み。

見る場所:

| 役割 | URL |
|------|-----|
| 機械が読む仕様（OpenAPI JSON） | http://127.0.0.1:8080/v3/api-docs |
| 人が試す画面（Swagger UI） | http://127.0.0.1:8080/swagger-ui.html |

正は **コントローラと DTO**。ここを直してバックエンドを再起動すると、仕様書も更新される。

---

## 機能実装の確認（Swagger 導入の前）

機能定義書の必須機能のうち、今回の前提にした3つ。

| 機能 | 本実装 | 備考 |
|------|--------|------|
| タイムライン表示（F-02） | **完了** | `GET /api/posts`。新しい順。無限スクロールは `beforeCreatedAt` + `beforeId`。「フォロー中」タブはフォロー表がまだ無いので空案内 |
| 画像投稿（F-05） | **完了（ローカル）** | `POST /api/posts` の multipart `image`。実体は `uploads/`、DB はファイル名。応答の `imageUrl` は `/uploads/ファイル名` |
| 画像投稿の S3 連携 | **まだ繋がない** | 授業では差し替え口だけ。`imageUrl` を公開 URL に変える想定。バケットは作らない（常時稼働・有料禁止） |
| ユーザー検索（F-07） | **まだ** | 最終回。コードが無いので OpenAPI にもパスが出ない |

実装済みの API だけが Swagger に出る。仕様書を先に書いて実装が追いつかない、という状態にはしない。

---

## なぜ API 仕様書が要るか

バックエンドとフロントエンドが **別チーム** だと、口頭やチャットだけではずれる。画面担当が実装の途中で知りたいのは、次のインターフェースである。

### 1. リクエストパラメータ

- パスと HTTP 方法（例: `POST /api/login`、`GET /api/posts`）
- 要ログインか（`Authorization: Bearer`）
- JSON のフィールド名と制約（メール、パスワード8文字以上、本文1〜280文字）
- multipart のとき、フィールド名が `body` と `image` であること（`file` ではない）
- クエリ（`tab`、`limit`、無限スクロールの `before*`）

ここが無いと、画面は `POST /api/timeline` を叩き、API は `GET /api/posts` を待つ、ということが起きる。

### 2. レスポンス形式

- 成功時の JSON（`accessToken` / `refreshToken` / `user`、一覧の `{ posts, hasMore }`）
- 日時の文字列の形
- 画像はファイルそのものではなく `imageUrl`
- パスワードは絶対に出ない

画面は「あると思って読んだキー」が無いと落ちる。キー名を片方が勝手に変えると、結合まで気づかない。

### 3. エラー時の挙動

HTTP ステータスだけでなく、**本文の形** と **画面が何をするか** まで揃える。

この API のエラーは常に次の形である。

```json
{ "status": 401, "code": "UNAUTHORIZED", "error": "ログインしてください" }
```

| 状況 | HTTP | 画面の動き（合意） |
|------|------|-------------------|
| 未ログイン・トークン無効 | 401 | ログイン画面へ。`error` を出す |
| メール／パスワード違い | 401 | パスワード欄を空にする |
| 本文が空、画像形式不正 | 400 | 投稿せず、理由を出す |
| 画像が 5MB 超 | 413 | 「画像は5MBまでです」 |
| 他人の投稿を消そうとした | 403 | 消さない |
| 無い ID | 404 | 一覧へ戻すなど |

ステータスだけ決めて本文が HTML の 500 だと、画面は JSON として読めず「リクエストに失敗しました」とだけ出す。仕様書にエラーの形を書く意味はここにある。

---

## AI がなかったころとの比較

| | AI がなかったころ | いま（このリポジトリ） |
|--|-------------------|------------------------|
| 先に書くもの | Word / Excel / 手書きの API 一覧。レビューしてから実装 | コントローラと DTO。起動すると OpenAPI が出る |
| 正しさの置き場 | 文書。実装は文書に従う建前 | **コードが正**。文書はコードから生成する |
| 変更の伝わり方 | 実装を直した人が仕様書も直す。直し忘れが多かった | メソッド・フィールドを直して **再起動** すると `/v3/api-docs` が更新される |
| 画面担当の作業 | PDF を見てモックを組む。結合で初めて本当の JSON を見る | Swagger UI で同じパスを試し、フロントはキー名を合わせる |
| ずれが分かる時点 | 結合試験。週次の「仕様書とコードの突合」 | プルリクエストでアノテーションと実装が同じ差分に乗る |
| AI の役割 | 無い | コントローラ追加の下書きは速くできる。ただし **パス・型・エラー本文の合意は人が決める** |

昔の流れ（典型）:

1. バックエンドが API 一覧を書く（パス、パラメータ、例の JSON）
2. レビューして「これで作る」と決める
3. 両チームが並行実装する
4. 実装中にフィールドが増える
5. 仕様書が古いまま結合し、画面が 400 を受けて止まる

自動生成でも設計は無くならない。先に「ログインはメールかユーザー名か」「画像は multipart か URL か」は人が決める。変わるのは、**決めたあとの仕様書メンテを人手でやらない** ことである。

springdoc は起動時に次を読む。

- `@RestController` のマッピング
- メソッド引数（`@RequestParam`、`@RequestBody`、multipart）
- 戻り値の DTO
- `@Operation` / `@ApiResponse` / `@Schema`（人が足した説明）

コードに無い `GET /api/users?q=`（ユーザー検索）は仕様書にも出ない。未実装を文書だけ先に書く方式とは逆である。

---

## このリポジトリでの入れ方

| 役割 | 場所 |
|------|------|
| ライブラリ | `backend/pom.xml` の `springdoc-openapi-starter-webmvc-ui` |
| タイトル・JWT の Authorize | `backend/src/main/java/com/raisetech/timeline/config/OpenApiConfig.java` |
| パス設定 | `backend/src/main/resources/application.yml` の `springdoc` |
| 説明（パラメータ・エラー） | 各コントローラの `@Operation` / `@ApiResponse`、DTO の `@Schema` |
| ログイン不要 | `AuthInterceptor` は `/api/**` だけ。`/v3/api-docs` と `/swagger-ui.html` は対象外 |

試す順:

1. `backend` で `.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run`
2. ブラウザで http://127.0.0.1:8080/swagger-ui.html
3. `POST /api/login` を試す（例: `yamada@example.com` / `password123`）
4. 応答の `accessToken` を Authorize に入れる
5. `GET /api/posts` を試す

---

## コードを直したあとの自動更新

例: 投稿一覧にクエリ `tag` を足す。

1. `PostController.list` に引数を足す
2. 再起動する
3. `/v3/api-docs` の `GET /api/posts` の parameters に `tag` が増える
4. Swagger UI の入力欄にも出る

別ファイルの Markdown を直す必要はない。説明文を変えたいときだけ `@Parameter` や `@Schema` を直す。
