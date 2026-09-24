# N+1問題（コメント・いいね）

提出用の説明。**いまの実装は、タイムラインの件数取得で N+1 にしていない。** この資料どおり提出してよい。

## 何が N+1 か

親を **1回** 取ったあと、その件数ぶん子を **1件ずつ** 取る。クエリが「1 + N」になる。コメントといいねの両方を1件ずつ取ると「1 + 2N」になる。

このアプリでは親が **投稿一覧**、子が **コメント数** と **いいね数** である。どちらも「投稿に紐づくサブデータ」なので、同じ落とし穴になる。

## このアプリの悪い例（やってはいけない）

タイムラインはカードにコメント件数といいね件数を出す。投稿を 20 件出したあと、各投稿で次を呼ぶと N+1 になる。

```text
SELECT ... FROM posts ORDER BY created_at DESC LIMIT 20;   -- 1回

-- 投稿ごとに繰り返す（20回 + 20回）
SELECT COUNT(*) FROM comments WHERE post_id = ?;
SELECT COUNT(*) FROM likes    WHERE post_id = ?;
```

画面でも同じである。

```ts
const { posts } = await listPosts();
for (const post of posts) {
  await fetch(`/api/posts/${post.id}/comments`); // 件数のためだけに投稿ごと
  await fetch(`/api/posts/${post.id}/likes`);
}
```

無限スクロールで 50 件なら、件数のためだけで約 100 回の追加アクセスになる。自分の PC では件数が少なくてスムーズでも、提出先で投稿が増えると遅くなる。

## このアプリの正しい例（いまの実装）

投稿一覧の **同じ SELECT** に件数を乗せる。追加のラウンドトリップは無い。

```sql
SELECT p.id, p.user_id, p.body, p.image_path, p.created_at, p.updated_at,
       u.username, u.display_name,
       (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comment_count,
       (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS like_count,
       (SELECT COUNT(*) FROM likes liked
        WHERE liked.post_id = p.id AND liked.user_id = ?) AS liked_by_me
FROM posts p
JOIN users u ON u.id = p.user_id
ORDER BY p.created_at DESC, p.id DESC
LIMIT 20;
```

場所: `backend/src/main/resources/mapper/PostMapper.xml` の `selectPost`。

| 項目 | いま | N+1 か |
|------|------|--------|
| タイムラインのコメント数 | 上のサブクエリ | **ならない** |
| タイムラインのいいね数 | 同じ SELECT の `like_count` / `liked_by_me` | **ならない** |
| 投稿詳細のコメント本文 | 開いた 1 件だけ `GET /api/posts/{id}/comments` | **ならない** |
| フォロー中のフォロー先 | `follows` の IN サブクエリ 1 回 | **ならない** |

いいね表を足すときも、投稿ごとに問い合わせない。同じ SELECT へ乗せる。実装済み。

```sql
(SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS like_count,
(SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id AND l.user_id = ?) AS liked_by_me
```

別案は、取った投稿 ID で `WHERE post_id IN (...)` して `GROUP BY post_id` するバッチである。

## 起きやすい場所（機能定義との対応）

- F-03 コメント: タイムラインカードの **コメント n件**
- F-04 いいね: タイムラインカードの **♡ 件数** と自分が押したか
- F-06 フォロー: 「フォロー中」は `user_id IN (SELECT followee_id FROM follows ...)` を1回

投稿詳細でコメント本文を読むのは、親が 1 件なので N+1 ではない。
