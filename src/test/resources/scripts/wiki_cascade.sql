-- A small tree with one of every node type, for delete-cascade tests.
--   100 Root
--   └── 101 Parent to delete
--       ├── 102 Child topic
--       │   └── 105 Grandchild entry
--       ├── 103 Entry
--       └── 104 Poll
ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type)
VALUES (100, 'TOPIC'), (101, 'TOPIC'), (102, 'TOPIC'), (103, 'ENTRY'), (104, 'POLL'), (105, 'ENTRY');

INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (100, 'Root', ARRAY [101], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00'),
       (101, 'Parent to delete', ARRAY [102, 103, 104], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00'),
       (102, 'Child topic', ARRAY [105], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

INSERT INTO wiki_entry(id, headline, body, author, created_at, updated_at)
VALUES (103, 'Entry', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00'),
       (105, 'Grandchild entry', '<p>Test</p>', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

INSERT INTO wiki_poll(id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (104, 'Poll', '', null, false,
        '[{"votes": [], "pollOption": {"text": "testCase", "uuid": "uuid-1"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));
