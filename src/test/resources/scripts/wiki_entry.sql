-- Disable trigger temporarily to allow inserting with explicit IDs
ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type) VALUES (11, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (11, 'Thema 1', ARRAY [14], 'Author', '2024-01-01T12:00:00+00', '2024-01-02T12:00:00+00');
INSERT INTO wiki(id, type) VALUES (14, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (14, 'Eintrag 1', '<p>Test</p>', 'Author', '2024-01-01T12:00:00+00', '2024-01-02T12:00:00+00');
INSERT INTO wiki(id, type) VALUES (7, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (7, 'Eintrag to delete 3', '<p>Test</p>', 'Author', '2024-01-01T12:00:00+00', '2024-01-02T12:00:00+00');
INSERT INTO wiki(id, type) VALUES (8, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (8, 'Topic with child delete', ARRAY [7], 'Author', '2024-01-01T12:00:00+00', '2024-01-02T12:00:00+00');
INSERT INTO wiki(id, type) VALUES (15, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (15, 'Eintrag 2', '<p>Test</p>', 'Author', '2024-01-01T12:00:00+00', '2024-01-02T12:00:00+00');

-- Re-enable triggers
ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

-- Update sequence to be higher than any inserted ID
SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));
