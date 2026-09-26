---
name: tripdiary-school
description: >-
  TripDiary の学校提出ワークフロー。Skill保存、フォルダ上書き、git commit、GitHub push。
  旅行アプリ、提出URL、RaiseTechTripDiary、コマンドプッシュの話で使う。
---

# TripDiary — 学校提出（Skill・ファイル・push）

ユーザーが「スキルして」「コマンドプッシュして」「ファイルに保管して」「フォルダーに上書き保存」と言ったら、次を行う。

## 絶対ルール

- アプリ名は **TripDiary**（旅の記録）。にゃんこ・おうち・課題提出タイムラインの見た目は使わない
- GitHub は岡田法子アカウント `n0r1k09583` の **public**（先生が URL を開ける）
- force push しない。git config は変えない
- `.env` / `*.db` / `backend/target/` / `node_modules/` / `uploads/` は Git に入れない
- AWS は定義のみ。**terraform apply して残さない**
- 画面に「学習用」と出さない

## リポジトリ

- 作業ルート: `C:\Users\user\旅行アプリ`
- リポジトリ: https://github.com/n0r1k09583/RaiseTechTripDiary
- 学校フォームの提出URL: 上記リポジトリ URL（`/blob/` は不可）

## ファイルに保管する場所（上書き）

プロジェクト内を正とし、ユーザー側 Skill も同じ内容で上書きする。

| 内容 | プロジェクト | コピー先 |
|------|----------------|----------|
| アプリ本体Skill | `.cursor/skills/tripdiary/SKILL.md` | `~/.cursor/skills/tripdiary/SKILL.md` |
| 本Skill | `.cursor/skills/tripdiary-school/SKILL.md` | `~/.cursor/skills/tripdiary-school/SKILL.md` |
| 提出用HTML | `docs/要件定義書.html` | `~/Desktop/旅行アプリ/` |
| 要件・機能・画面・ER・技術 | `docs/` | 同上 |
| 機能定義書 | `docs/FEATURES/機能定義書.md` | `~/Desktop/旅行アプリ/FEATURES/` |
| 結合プロトタイプ | `prototype/` | `~/Desktop/旅行アプリ/prototype/` |
| 確認ガイド | `docs/verify.md` | `~/Desktop/旅行アプリ/` |

ソースの控えは `~/Desktop/旅行アプリ/` に、`node_modules` / `backend/target` / `.git` / `*.db` / `uploads` / `logs` を除いて上書きする。

## コマンド（push）

```powershell
git add -A
git status
git commit -m "学校提出用: TripDiary のソースと中級編の技術一式を保管"
git push -u origin HEAD
```

リモートが無いときは:

```powershell
gh repo create RaiseTechTripDiary --public --source=. --remote=origin --push
```

## ミスしない場所

- 提出URLは次だけ。`/blob/` やファイルのURLは不可  
  https://github.com/n0r1k09583/RaiseTechTripDiary
- リポジトリは岡田法子 `n0r1k09583` の **public**
- ログイン案内: 「行った場所と行きたい場所を、写真と感想で残すアプリです。」画面に「学習用」と出さない
- 試すアカウント `@yamada` `@hanako` `@ichiro` / `password123`
- AWS は `infra/terraform/`。enable_infra=false。NAT なし。desired_count 0。**apply して残さない**
- `.env` / `*.db` / `backend/target/` / `node_modules/` / `uploads/` / `backend/logs/` は Git に入れない
- 他の受講生の Next.js や本番の EC2 には合わせない。こちらの API は Spring Boot のまま

## 再開用（ここまでの確定）

- 学校提出済み（2026-09-24）。先生の指摘（2026-09-26）: README をふくらませる。他の受講生の書き方を参考にしてよい。動作が分かる画像があるとよい
- 提出URLは https://github.com/n0r1k09583/RaiseTechTripDiary
- README は目次、確認アカウント、動作の画像、機能、技術、動かし方、CI、AWS は定義だけ
- 写真は `PhotoField`。感想がなくても写真だけで投稿可
- タブ: みんなの記録 / フォロー中 / 訪問済み / 行きたい / 写真 / お気に入り。みんなの記録は公開だけ。お気に入りは本人だけ。地図クリックで緯度・経度
- テスト: backend 151、frontend 47。CI は `.github/workflows/ci.yml`。k6 / E2E は毎回のテストに載せない
- フォームのコメントは `docs/提出の案内.txt`
