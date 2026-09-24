# TripDiary — 旅の記録

中級編最終課題向けの旅行記録アプリです。**ローカルで動かします。AWS に常時公開しないので月額課金は出ません。**

## 機能

- 投稿: 場所名・感想・写真
- いいね: 行きたい
- コメント: 行き方の質問・おすすめ
- フォロー: 旅好きユーザー
- 追加: エリアタグ絞り込み、訪問済み / 行きたいリスト

## 動かし方（お金がかからない確認）

別のターミナルで:

```powershell
cd backend
.\mvnw.cmd -Dmaven.test.skip=true spring-boot:run
```

```powershell
cd frontend
npm install
npm run dev
```

画面: http://127.0.0.1:5173/login  
試すアカウント: `@yamada` `@hanako` `@ichiro`（パスワード `password123`）

API 仕様: http://127.0.0.1:8080/swagger-ui.html

## 認証・テスト・CI・ログ・性能・E2E・AWS の確認場所

詳しくは [docs/verify.md](docs/verify.md) を見てください。

| 項目 | どこで確認するか |
|------|------------------|
| 認証 | ログイン画面。JWT。未ログインは `/api/posts` が 401 |
| テスト | `backend` で `.\mvnw.cmd test`（H2）。`frontend` で `npm test` |
| CI/CD | `.github/workflows/ci.yml`（push / PR で自動） |
| ログ | `backend/logs/` の JSON（パスワードは出さない） |
| 性能 | `perf/k6-timeline.js`（任意。毎回の test には載せない） |
| E2E | `e2e/playwright.spec.ts`（任意。CI には載せない） |
| AWS | `infra/terraform/`。既定 `enable_infra=false`。**apply して残さない** |

## やってはいけないこと

- `terraform apply` したまま放置（NAT / ALB / RDS は高い）
- アクセスキーを Git に書く
- 本番ドメインで常時公開する
