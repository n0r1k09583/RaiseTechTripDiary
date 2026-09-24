# 確認ガイド（授業で聞かれたときの答え）

このアプリは中級編で学んだ技術を、旅行記録（TripDiary）にまとめたものです。  
**確認はローカルで足ります。AWS を作りっぱなしにしないので、お金はかかりません。**

## 1. 認証

- 画面: http://127.0.0.1:5173/login
- 試す: `@yamada` ボタン（パスワード `password123`）
- 仕組み: JWT（アクセス＋リフレッシュ）。パスワードは BCrypt。ログに出さない
- API 確認: 未ログインで `GET http://127.0.0.1:8080/api/posts` → 401
- Swagger: http://127.0.0.1:8080/swagger-ui.html の Authorize に `accessToken`

## 2. テスト

正しさのテスト。本番の SQLite には書かない（H2）。

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm test
```

メソッド名は日本語にしてよい（例: `自分の投稿は編集削除でき他人は403`）。

## 3. CI/CD

ファイル: `.github/workflows/ci.yml`

- push / PR で GitHub Actions が動く
- バックエンド: `./mvnw -B test`（Checkstyle 含む）
- フロント: `npm ci` → `tsc` → `npm test`
- k6 と Playwright は **毎回の CI に載せない**（遅い・壊れやすい）

GitHub の Actions タブで緑になれば CI は通っている。

## 4. ログ

起動すると `backend/logs/` にファイルができます。

- `application.log` / `error.log`: JSON（traceId・userId・duration_ms）
- アクセスログ: `access.log`
- Datadog は入れない
- パスワード・トークン・本文は出さない

確認: ログインしたあと `backend/logs/application.log` を開く。

解説: [logging.md](./logging.md)

## 5. パフォーマンステスト（任意）

毎回の `mvnw test` には載せない。任意のときだけ:

```powershell
k6 run perf/k6-timeline.js
```

見るもの: P50 / P95 / P99。N+1 を避けるため件数は同じ SELECT のサブクエリ。

解説: [performance.md](./performance.md)

## 6. E2Eテスト（任意）

実ブラウザ。CI には載せない。

```powershell
cd e2e
npx playwright install
npx playwright test
```

人が画面を触る確認でもよい（今回の Cursor ブラウザ確認と同じ）。

## 7. AWS 構成（定義だけ・課金しない）

授業の図に近い形を Terraform に書いてある。

| 層 | サービス |
|----|----------|
| 画面 | S3 + CloudFront |
| API | ECS Fargate（ポート 8080） |
| 前段 | ALB |
| DB | RDS PostgreSQL 17 |
| 画像 | S3 |

お金をかけない工夫:

1. `enable_infra` の既定は **false**。普通に apply しても AWS に何も立たない
2. **NAT Gateway は入れない**（一番高い）。Fargate はパブリック IP
3. Fargate の `desired_count` 既定は **0**
4. 提出 URL が要るときだけ短時間 `enable_infra=true` → 見終わったら **すぐ destroy**

提出の動作確認はローカル `5173` と `8080` で足りる。

解説: [../infra/terraform/README.md](../infra/terraform/README.md)
