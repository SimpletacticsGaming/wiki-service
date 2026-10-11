-- Disable trigger temporarily to allow inserting with explicit IDs
ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type) VALUES (24, 'TOPIC');
INSERT INTO wiki_topic(id, topic, author, created_at, updated_at)
VALUES (24, 'Thema for poll', 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (5, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (5, 'Frage 1', 'Beschreibung 1', '2023-10-21', false,
        '[{"votes": [{"userId": 1, "date": "2023-10-21T00:00:00Z", "option": "FALSE"}], "pollOption": {"text": "test1", "uuid": "egal1"}}, {"votes": [{"userId": 2, "date": "2023-10-22T00:00:00Z", "option": "TRUE"}], "pollOption": {"text": "test2", "uuid": "egal2"}}]',
        'testAuthor', '2023-10-21T00:00:00+00', '2023-10-21T00:00:00+00');
INSERT INTO wiki(id, type) VALUES (2, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (2, 'This is a test', 'Test description', '2023-10-21', false,
        '[{"votes": [], "pollOption": {"text": "testCase", "uuid": "ddb11436-bdc8-4488-87f6-fsdfsd"}}, {"votes": [], "pollOption": {"text": "testCaseTwo", "uuid": "aaa1436-bdc8-4488-87f6-fsdfsd"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (6, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (6, 'This is a test poll to delete', '', null, true,
        '[{"votes": [{"userId": 1, "date": "2023-10-21T00:00:00Z", "option": "FALSE"}], "pollOption": {"text": "testCase", "uuid": "ddb11436-bdc8-4488-87f6-fsdfsd"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (1, 'TOPIC');
INSERT INTO wiki_topic(id, topic, child_id, author, created_at, updated_at)
VALUES (1, 'Thema for poll 2', ARRAY [2,6], 'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (3, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (3, 'This is a test', '', null, false,
        '[{"votes": [{"userId": 1, "date": "2023-10-21T00:00:00Z", "option": "FALSE"}], "pollOption": {"text": "testCase", "uuid": "ddb11436-bdc8-4488-87f6-fsdfsd"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (4, 'POLL');
INSERT INTO wiki_poll (id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (4, 'This is a test', '', '2099-09-24', false,
        '[{"votes": [{"userId": 1, "date": "2023-10-21T00:00:00Z", "option": "FALSE"}], "pollOption": {"text": "testCase", "uuid": "ddb11436-bdc8-4488-87f6-fsdfsd"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');
INSERT INTO wiki(id, type) VALUES (30, 'POLL');
INSERT INTO wiki_poll(id, question, description, end_date, ended, data, author, created_at, updated_at)
VALUES (30, 'This is a closed test', 'Test description', '2023-10-30', true,
        '[{"votes": [], "pollOption": {"text": "testCase", "uuid": "ddb11436-bdc8-4488-87f6-fsdfsd"}}, {"votes": [], "pollOption": {"text": "testCaseTwo", "uuid": "aaa1436-bdc8-4488-87f6-fsdfsd"}}]',
        'unknown_user', '2024-01-01T10:00:00+00', '2024-01-01T10:00:00+00');

-- Re-enable triggers
ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

-- Update sequence to be higher than any inserted ID
SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));
