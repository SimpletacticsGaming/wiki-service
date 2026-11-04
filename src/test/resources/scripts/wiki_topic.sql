-- Disable trigger temporarily to allow inserting with explicit IDs
ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type) VALUES (1, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (1, 'Thema 1', ARRAY [2], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (2, 'TOPIC');
INSERT INTO wiki_topic(id, topic, author, created_at, updated_at)
VALUES (2, 'Thema for poll', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (3, 'TOPIC');
INSERT INTO wiki_topic(id, topic, author, created_at, updated_at)
VALUES (3, 'Thema 2', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (4, 'TOPIC');
INSERT INTO wiki_topic(id, topic, author, created_at, updated_at)
VALUES (4, 'Thema 9', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (5, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (5, 'Thema 10', ARRAY [5], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (6, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (6, 'Thema 3', ARRAY [5], 'Author', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (16, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (16, 'Eintrag to delete 21', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (17, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (17, 'Eintrag to delete 3', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (7, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (7, 'Topic to delete', ARRAY [16, 17], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (8, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (8, 'Topic to delete 2', ARRAY [2, 7], 'unknown_author', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (9, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (9, 'Topic with child delete', ARRAY [12], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (10, 'TOPIC');
INSERT INTO wiki_topic(id, topic, author, created_at, updated_at)
VALUES (10, 'Topic to delete 2', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (15, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (15, 'Eintrag for topic delete', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (11, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (11, 'Thema 4', ARRAY [15], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (12, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (12, 'Eintrag 1', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (13, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (13, 'Frage 1', 'Beschreibung 1', null, false,
        '[{"votes": [{"userId": 1, "date": "2023-10-21T00:00:00Z", "option": "FALSE"}], "pollOption": {"text": "test1", "uuid": "egal1"}}, {"votes": [{"userId": 2, "date": "2023-10-22T00:00:00Z", "option": "TRUE"}], "pollOption": {"text": "test2", "uuid": "egal2"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (14, 'ENTRY');
INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (14, 'Eintrag to delete', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

-- Re-enable triggers
ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

-- Update sequence to be higher than any inserted ID
SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));
