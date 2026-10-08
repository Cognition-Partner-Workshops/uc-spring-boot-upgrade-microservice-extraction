ALTER TABLE articles ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE article_favorites ADD COLUMN created_at TIMESTAMP;
CREATE INDEX idx_article_favorites_created_at ON article_favorites (created_at);
