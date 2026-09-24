# Webアプリケーションにおけるログ・監視の設計指針

公開したあともアプリは動き続ける。障害の痕跡は **ログに残る**。課題は **`backend/logs/` のファイルを開いて確認する** ことまで。Datadog などの監視 SaaS は検索しやすい、という紹介にとどめ、**このリポジトリには入れない**（課金・常時稼働を増やさない）。

関連: [テスト](./testing.md)、[パフォーマンステスト](./performance.md)（`duration_ms` を並べたものが P95）、[機能定義書](./FEATURES/機能定義書.md)（要件の「ログ」）、起動は `backend` で `.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run`

---

## ログレベルの使い分け

| レベル | 用途 | 例 |
|--------|------|-----|
| **ERROR** | 直ちに対応が必要な異常 | DB 接続失敗、外部 API 失敗、未処理例外 |
| **WARN** | 許容範囲内の異常・潜在的な問題 | リトライ、レート制限の接近、非推奨 API、ログイン失敗、権限なし |
| **INFO** | 業務上意味のあるイベント | ユーザー登録、ログイン成功、投稿作成、デプロイ成功 |
| **DEBUG** | 開発・調査用の詳細 | リクエスト／レスポンス本文、SQL、処理時間の内訳 |

本番の既定は `INFO`。調べるときだけ `application.yml` の `logging.level.com.raisetech.tripdiary` を `DEBUG` にする。

このアプリでは **DEBUG でもリクエスト本文やパスワードは出さない**。SQL を見たいときは MyBatis のロガーを一時的に上げる。

---

## 構造化ログ（JSON 形式）

プレーンテキストより JSON の方が、Datadog などで検索・アラートしやすい。課題の確認対象はファイルなので、`backend/logs/application.log` と `error.log` は **1行1 JSON** にする。コンソールは起動中に読みやすいよう従来のテキストのまま。

例:

```json
{
  "timestamp": "2026-09-05T08:00:00.000Z",
  "level": "ERROR",
  "service": "timeline",
  "traceId": "abc123...",
  "userId": "456",
  "class": "com.raisetech.tripdiary.service.PostService",
  "message": "画像の保存に失敗しました",
  "exception": "IOException: ...",
  "endpoint": "/api/posts",
  "method": "POST",
  "httpStatus": "500",
  "duration_ms": "3012"
}
```

`traceId` はリクエストごとに採番する。同じリクエストの INFO / WARN / ERROR を追える。

---

## ログに含めるべきフィールド

| フィールド | 役割 |
|------------|------|
| `traceId` / `requestId` | リクエスト単位の追跡。分散トレーシングの入口 |
| `userId` | 誰の操作か。PII に注意し、メールや氏名は載せない |
| `service` / `class` | どこで起きたか |
| `duration_ms` | 性能分析。API 完了時 |
| `httpStatus`, `endpoint`, `method` | API 層。どの URL が何ミリ秒で何番だったか |

実装: `RequestMdcFilter` が `traceId`・`endpoint`・`method`・`httpStatus`・`duration_ms` を MDC に載せる。ログイン後は `AuthInterceptor` が `userId` を載せる。

---

## ログに含めてはいけないもの

- パスワード、トークン（JWT・リフレッシュ）、クレジットカード番号
- 必要のない個人情報（メール本文、氏名、住所）
- リクエスト／レスポンスの本文（パスワードが混ざるため、既定では出さない）

ログイン失敗は理由（ユーザー不在／パスワード違い）をログでも分けない。画面と同じく列挙対策。失敗時は `email=` まで（パスワードは出さない）。

---

## このリポジトリでの置き場

| 種類 | 置き場 | 見ること |
|------|--------|----------|
| アクセスログ | `logs/access.*.log`（Tomcat、テキスト） | 誰がどの URL に何ミリ秒で、ステータス何番か |
| アプリケーションログ | `logs/application.log`（JSON） | 登録・ログイン、投稿作成、権限なし、リクエスト完了 |
| エラーログ | `logs/error.log`（ERROR 以上の JSON） | 未処理例外、画像保存失敗 |

ローテーション: アプリケーション 7日・10MB、エラー 14日。テストは `logback-test.xml` でコンソールのみ。`logs/` に書かない。

設定: `backend/src/main/resources/logback-spring.xml`、`application.yml` の `logging` と `server.tomcat.accesslog`。

確認手順:

1. `backend` で起動する
2. ログインをわざと失敗させる
3. `backend/logs/application.log` を開き、1行が JSON であること、`ログイン失敗` がありパスワードが無いことを見る
4. 同じ行に `traceId`・`endpoint`・`method`・`httpStatus` があることを見る
5. `backend/logs/access.*.log` に `POST /api/login` とステータスがあることを見る

---

## Datadog など（導入しない）

本番では JSON ログを Datadog / CloudWatch に送ることが多い。中級編の課題は **ファイルを自分で開く** こと。ツールのアカウント作成は不要。
