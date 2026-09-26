ALTER TABLE posts ADD COLUMN latitude REAL;
ALTER TABLE posts ADD COLUMN longitude REAL;
ALTER TABLE posts ADD COLUMN visibility TEXT NOT NULL DEFAULT 'public';

CREATE TABLE favorites (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  post_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE (post_id, user_id)
);

CREATE INDEX idx_favorites_post_id ON favorites (post_id);
CREATE INDEX idx_favorites_user_id ON favorites (user_id);
