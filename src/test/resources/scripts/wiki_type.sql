-- Disable trigger temporarily to allow inserting with explicit IDs
ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type) VALUES (1, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (1, 'Thema 1', ARRAY [2], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (2, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (2, 'Eintrag 1', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (3, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (3, 'Frage 1', 'Beschreibung 1', null, false,
        '[{"votes": [], "pollOption": {"text": "test1", "uuid": "egal1"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

-- Re-enable triggers
ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

-- Update sequence to be higher than any inserted ID
SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));
