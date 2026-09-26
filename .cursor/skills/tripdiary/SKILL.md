---
name: tripdiary
description: >-
  TripDiary 旅の記録アプリ（中級編最終課題）。React + Spring Boot + MyBatis + JWT。
  場所名・感想・写真共有、行きたい、コメント、フォロー、エリア絞り込み、訪問済み/行きたい/写真タブ。
  AWS は定義のみで apply して残さない。お金をかけない。
  Use when editing 旅行アプリ, TripDiary, 旅の記録, 行きたい, エリアタグ, 写真共有.
---

# TripDiary — 旅の記録

作業ルート: `C:\Users\user\旅行アプリ`

にゃんこ・おうち・課題提出タイムラインの見た目は使わない。空のような水色・白い雲・飛ぶ感じ。

ログイン案内は次の文言（「学習用」は出さない）:

行った場所と行きたい場所を、写真と感想で残すアプリです。

## 機能

- 投稿: 場所名・エリア・訪問済み/行きたい・感想・写真1枚（JPEG / PNG / WebP、5MB）
- 写真共有: ドロップゾーン（`PhotoField`）。感想がなくても写真だけで投稿可。一覧は写真を大きく出す。送信は「みんなと共有する」
- いいね: 行きたい（`POST /api/posts/{id}/likes`）
- コメント: 行き方・おすすめ
- フォロー / ユーザー検索
- タブ: みんなの記録 / フォロー中 / 訪問済み / 行きたい / 写真 / お気に入り（`tab=photos` は `image_path` あり。みんなの記録は公開だけ）
- 公開 / 非公開。地図クリックで緯度・経度。お気に入りは本人だけ
- 旅行案内: 「旅行プラン」タブ。世界地図は残す。そら（`travelConsult.ts`）は世界の観光地と有名な場所を会話で案内する。カフェ店名は案内の中心にしない
- エリア絞り込み

## 技術

- 画面: `frontend/` React 19 / Vite / TypeScript。ポート 5173
- API: `backend/` Spring Boot + MyBatis XML + Flyway + JWT。ポート 8080。起動は `.\mvnw.cmd`
- ローカル DB は SQLite。テストは H2。jOOQ 禁止。Express に戻さない
- 画像は `uploads/`。シード写真は `backend/src/main/resources/seed-photos/`（起動時に場所名へ付ける）
- CI: `.github/workflows/ci.yml`。k6 / E2E は載せない
- ログ: `backend/logs/` JSON。Datadog なし
- AWS: `infra/terraform/`。`enable_infra=false`。NAT なし。desired_count 0。**terraform apply して残さない**

## 確認

- 提出済み。提出URL: https://github.com/n0r1k09583/RaiseTechTripDiary （public）
- 画面 http://127.0.0.1:5173/login
- `@yamada`（山田） `@hanako` `@ichiro` / `password123`
- 写真共有の見え方は山田の投稿を正とする
- 答えの一覧は `docs/verify.md`
- テスト（2026-09-26）: backend 151、frontend 47

## 起動

```powershell
# backend
.\mvnw.cmd "-Dmaven.test.skip=true" spring-boot:run
# frontend
npm run dev
```

## やってはいけないこと

- AWS 常時稼働・有料リソースを作る
- NAT Gateway を入れる
- 画面に「学習用」と出す
- `.env` / `*.db` / `backend/target/` を Git に入れる
