UPDATE articles
SET created_at = CAST(strftime('%s', created_at) AS INTEGER) * 1000
WHERE typeof(created_at) = 'text';

UPDATE articles
SET updated_at = CAST(strftime('%s', updated_at) AS INTEGER) * 1000
WHERE typeof(updated_at) = 'text';

UPDATE comments
SET created_at = CAST(strftime('%s', created_at) AS INTEGER) * 1000
WHERE typeof(created_at) = 'text';

UPDATE comments
SET updated_at = CAST(strftime('%s', updated_at) AS INTEGER) * 1000
WHERE typeof(updated_at) = 'text';
