-- Give seeded favorites timestamps (epoch millis, matching how the app stores them)
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-6 days') AS INTEGER) * 1000
  WHERE article_id = 'article-1' AND user_id = 'user-2';
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-1 days') AS INTEGER) * 1000
  WHERE article_id = 'article-1' AND user_id = 'user-3';
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-4 days') AS INTEGER) * 1000
  WHERE article_id = 'article-2' AND user_id = 'user-1';
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-2 days') AS INTEGER) * 1000
  WHERE article_id = 'article-3' AND user_id = 'user-2';
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-1 days') AS INTEGER) * 1000
  WHERE article_id = 'article-4' AND user_id = 'user-1';
UPDATE article_favorites SET created_at = CAST(strftime('%s', 'now', '-10 hours') AS INTEGER) * 1000
  WHERE article_id = 'article-5' AND user_id = 'user-3';
