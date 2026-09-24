# 次回・最終回 — 再開用

フォルダを開いたら **ここから再開**する。  
日付: 2026-09-14

関連: [機能定義書](./FEATURES/機能定義書.md) F-05・F-06・F-07、[画面仕様書](./screens.md) S-03・S-05・S-07、[ER図](./er.md)、[N+1](./n-plus-one.md)

---

## 授業の並び

| 回 | 内容 | プログラム |
|----|------|------------|
| いままで | 認証・投稿・コメント・画像（ローカル） | あり |
| 直前 | プロフィール表示、フォロー／フォロワー、「フォロー中」タブ | あり |
| **いま** | Swagger（OpenAPI）で API 仕様書を自動生成 | あり（`/v3/api-docs`） |
| 番外 | テストの答え合わせとログ。JSON 構造化、日本語メソッド名、Mockito、H2、Checkstyle | あり。CI は `.github/workflows/ci.yml`（`mvnw test` と `npm test`。k6 / E2E は載せない） |
| **最終回** | ユーザー検索、タイムライン「フォロー中」、画像の全体調整（授業では S3 の話あり） | 検索・フォロー中は **あり**。AWS の Terraform 定義は `infra/terraform/`。apply して残さない |

---

## Swagger で仕様書を自動生成する仕組み

別チームが画面と API を同時に作るとき、口頭やチャットだけではずれる。必要なのは次のインターフェース定義である。

- パスと HTTP 方法（例: `POST /api/login`）
- リクエスト（JSON のフィールド、multipart の `body` / `image`）
- レスポンスの形（トークン、投稿一覧の `posts` / `hasMore`）
- エラー（401 は `{ error, code, status }`、「ログインしてください」など）

**AI がなかったころ**は、先に Word / Markdown / 手書きの仕様書を書き、実装して、実装が変わったら仕様書を人手で直した。直し忘れが多かった。

**いま**はコントローラと DTO が正で、springdoc が起動時にアノテーションとメソッドを読んで OpenAPI JSON を出す。コードを直して **再起動** すると仕様書も更新される。別ファイルを二重管理しない。

授業用の解説（必要性、エラーの形、AI 前後の比較、機能確認）は [openapi.md](./openapi.md)。

| 役割 | URL |
|------|-----|
| 機械が読む仕様 | http://127.0.0.1:8080/v3/api-docs |
| 人が見る画面 | http://127.0.0.1:8080/swagger-ui.html |

設定: `backend/src/main/java/com/raisetech/timeline/config/OpenApiConfig.java`、`application.yml` の `springdoc`。JWT が要る API は Swagger の Authorize に `accessToken` を入れる。`/v3/api-docs` は `/api/**` の外なのでログイン不要。

---

## いま終わっていること（共有してよい）

| 機能 | 状態 |
|------|------|
| ログイン／投稿／無限スクロール／コメント | あり。SQL は XML。件数は一覧のサブクエリ（N+1 にしない） |
| いいね | トグルあり。件数は同じ SELECT のサブクエリ（N+1 にしない） |
| 画像投稿 | ローカル `uploads/`。5MB。JPEG / PNG / WebP。S3 は `APP_STORAGE=s3` の差し替え口 |
| プロフィール・フォロー・検索 | あり。「フォロー中」タブは自分＋フォロー先 |

試すアカウント: `@yamada` `@hanako` `@ichiro`、パスワードは全員 `password123`。API は 8080。止まっていると「リクエストに失敗しました」。

---

## 率直に共有すること（プロフィール画像・AI）

プロフィール画像の変更は、**1〜2時間苦戦してよい題材**である。AI に頼っても、次は人の確認が要る。

- OS のファイル選択ダイアログは、エージェントや自動化から押せない
- 画面は通ったのに API が 400/413、または 8080 が止まっていて全部失敗、ということが続く
- Content-Type が空だと JPEG でも弾く
- 「直して」と繰り返しても、ファイルを選ぶ操作そのものは AI が代われない

**AI を使った開発でも、詰まる場面はある。** 特にファイル・権限・ブラウザの外側（OS ダイアログ）はそうである。授業では失敗のログと、API を curl で分けて見たことを話せばよい。

---

## プルリクエストで見る実装ポイント

### 1. MyBatis は XML

Java に複数行の `@Select` を書かない。SQL は `backend/src/main/resources/mapper/*.xml`。設定は `application.yml`:

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.raisetech.tripdiary.domain
  configuration:
    map-underscore-to-camel-case: true
```

検索・フォローも `UserMapper.xml` / `FollowMapper.xml` に足す。`<sql>` で SELECT を共有する。

### 2. ファイルサイズのバリデーション

二重にする（どちらか片方だと漏れる）。

| 層 | いまの場所 | 超えたとき |
|----|------------|------------|
| Spring | `spring.servlet.multipart.max-file-size: 5MB` | 413「画像は5MBまでです」 |
| アプリ | `ImageStorage` の `MAX_BYTES` | いまは 400。最終調整で 413 に揃えてよい |
| 画面 | `TimelinePage` の `IMAGE_MAX` | 送る前に止める |

プロフィール画像も同じ 5MB・同じ形式にする。S3 に出す場合も、**アップロード前**にサイズを見る（大きいファイルをバケットに載せない）。

### 3. ユーザー検索の SQL（F-07）

ユーザーを1件ずつ取らない。バインドする。部分一致はユーザー名。SQLite なら大文字小文字を区別しない。

```sql
SELECT id, email, username, display_name, created_at
FROM users
WHERE username LIKE '%' || #{q} || '%' COLLATE NOCASE
ORDER BY username ASC
LIMIT 20
```

- `#{q}` にする。文字列連結で SQL を組まない
- 空文字は SQL を叩かず、案内だけ返す
- 結果からプロフィールへ。自分の行は出してよいがフォローボタンは出さない

---

## Chrome DevTools の注意

授業・提出の確認で DevTools を使うとき、次は **できない／拒否される** ことがある。

- ネイティブのファイル選択（プロフィール画像・投稿画像）
- cookie / ダウンロード / ローカルファイルに触る CDP
- 自動化からの `Input.*` でファイルを載せる操作

画像は「人が選ぶ」か「API に multipart を直接送る」。Network タブで `/api/posts` やプロフィール更新のステータスと JSON を見る。DevTools だけでファイルダイアログまで再現しようとしない。

---

## 最終回の実装の方向

### タイムライン「フォロー中」（フォロー中ユーザーのみ）

`follows` が無いと空のまま。フォロー実装のあと、`PostMapper.xml` の `tabFilter` を次にする（フォロー先を1人ずつ取らない）。

```sql
<if test="tab == 'following'">
  AND (p.user_id = #{viewerId}
    OR p.user_id IN (SELECT followee_id FROM follows WHERE follower_id = #{viewerId}))
</if>
```

自分の投稿も出す。0件のときは「フォロー中の投稿はまだありません」。

### ユーザー検索

`GET /api/users?q=`（要ログイン）。画面はヘッダーから S-07。プロトタイプ `search.html` を見た目の参考にする。

### 画像投稿と S3（全体調整）

**いま動いているのはローカル `uploads/` である。AWS のバケットを常時作らない。** 月額課金・実サーバー常時稼働は禁止（このリポジトリのルール）。構成の **定義** は `infra/terraform/`（S3+CloudFront、Fargate、RDS PostgreSQL 17、画像 S3、ALB）。`terraform apply` して残さない。

授業で S3 を話す場合の整理:

- DB には URL またはキーだけ持つ。ファイル本体を SQLite に入れない（いまと同じ）
- キー・シークレットは `.env`。Git に書かない
- 無料枠の説明とコードの差し替え口（`ImageStorage`）まで。`terraform apply` で作りっぱなしにしない
- 提出の動作確認はローカルアップロードで足りる

全体調整: タイムライン・詳細・プロフィールで同じ画像 URL を使う。Vite の `/uploads` プロキシを忘れない。S3 にするときは公開 URL に差し替えるだけにする。

---

## 作業順（残り）

1. プロフィール画像の変更は人がファイルを選ぶ（任意。ER に列はまだ無い）
2. 提出用 URL が要るときだけ `terraform apply` → 動作確認 → **すぐ destroy**（NAT なし。常時稼働しない）
3. 任意: k6（`docs/performance.md`）、Playwright E2E。どちらも毎回の `mvnw test` / `npm test` / CI には載せない
4. 起動: `backend` で `.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run`。API が通れば画面の前に十分
