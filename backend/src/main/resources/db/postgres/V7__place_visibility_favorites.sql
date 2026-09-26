ALTER TABLE posts ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE posts ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE posts ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'public';

CREATE TABLE favorites (
  id BIGSERIAL PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at VARCHAR(32) NOT NULL DEFAULT TO_CHAR(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS'),
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE (post_id, user_id)
);

CREATE INDEX idx_favorites_post_id ON favorites (post_id);
CREATE INDEX idx_favorites_user_id ON favorites (user_id);
