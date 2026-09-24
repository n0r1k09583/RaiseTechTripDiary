# 技術スタック（にゃんこタスクと同一バージョン）

RaiseTechタイムラインの **画面側の技術は、タスクマネジメント（にゃんこタスク／Trello風タスクアプリ）とほぼ同じ** にする。バージョンも揃える。おうち受付（Next.js / Express）には戻さない。

根拠となる実装: `C:\Users\user\Trello風タスクアプリ\package.json` および `package-lock.json`。

## 指定バージョン（ロックファイルの実体）

| パッケージ | package.json の指定 | 使う実体バージョン |
|------------|---------------------|-------------------|
| react | `^19.0.0` | **19.2.8** |
| react-dom | `^19.0.0` | **19.2.8** |
| vite | `^6.0.3` | **6.4.3** |
| typescript | `~5.7.2` | **5.7.3** |
| @vitejs/plugin-react | `^4.3.4` | **4.7.0** |
| @types/react | `^19.0.0` | **19.2.17** |
| @types/react-dom | `^19.0.0` | **19.2.3** |

実装時の `package.json` は、にゃんこタスクと同じレンジで入れる。`npm install` 後の実体も上表に合わせる（意図なく Vite 7 や React 18 へ上げない）。

にゃんこタスク専用の `@dnd-kit/*` は、このSNSには不要なので入れない。

## 構成

| 層 | にゃんこタスク | RaiseTechタイムライン |
|----|----------------|------------------------|
| 画面 | React 19 + Vite 6 + TypeScript 5.7 | **同じ（上表のバージョン）** |
| 言語 | TypeScript | TypeScript |
| モジュール | `"type": "module"` | 同じ |
| ドラッグ＆ドロップ | @dnd-kit | 使わない |
| データの持ち方 | 一人用のため localStorage | 複数ユーザーが同じ投稿を見るため、**共有できる保存**が必要。画面の技術は変えない |

一人用の localStorage だけでは、BさんのいいねやコメントがAさんの画面に出ない。フロントのバージョンはにゃんこタスクと揃え、共有データ用の最小限の保存（実装時）だけ足す。

## 使わないもの

- おうち受付の Next.js / Express / SQLite 構成を、このSNSの画面スタックとしては使わない
- おうち受付の Express をこのSNSの API には使わない。**本実装の API は Spring Boot + MyBatis + SQLite + Flyway**（画面の React / Vite は上表のまま）
- 本番の常時公開、有料AWS

## 機能定義書の置き場

機能定義書（Functional Requirements）は `docs/FEATURES/`。`FUTURES` ではない。
