CREATE TABLE posts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  spot_name TEXT NOT NULL,
  area_tag TEXT NOT NULL,
  visit_status TEXT NOT NULL DEFAULT 'visited',
  body TEXT NOT NULL,
  image_path TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_posts_created_at ON posts (created_at DESC, id DESC);
CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_area_tag ON posts (area_tag);
