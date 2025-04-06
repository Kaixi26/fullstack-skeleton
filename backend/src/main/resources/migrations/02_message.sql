CREATE TABLE IF NOT EXISTS message
(
    id         INTEGER NOT NULL PRIMARY KEY,
    name       VARCHAR(255),
    message    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
