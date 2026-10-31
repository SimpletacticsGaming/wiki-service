-- Demo content for local development.
--
-- The addWiki BEFORE INSERT trigger overwrites NEW.id from wiki_id_seq, so
-- explicit ids only stick while it is disabled - the same trick the test
-- fixtures use. Topic 0 ('Startseite') is created by migration V001.
BEGIN;

ALTER TABLE wiki_topic DISABLE TRIGGER addWiki;
ALTER TABLE wiki_entry DISABLE TRIGGER addWiki;
ALTER TABLE wiki_poll DISABLE TRIGGER addWiki;

INSERT INTO wiki(id, type)
VALUES (1, 'TOPIC'), (2, 'TOPIC'), (3, 'TOPIC'),
       (4, 'ENTRY'), (5, 'ENTRY'),
       (6, 'POLL'), (7, 'POLL')
ON CONFLICT (id) DO NOTHING;

UPDATE wiki_topic SET child_id = ARRAY [1, 2, 3] WHERE id = 0;

INSERT INTO wiki_topic(id, topic, child_id)
VALUES (1, 'Thema 1', ARRAY [4, 6, 7]),
       (2, 'Thema 2', ARRAY [5]),
       (3, 'Thema 3', ARRAY []::integer[])
ON CONFLICT (id) DO NOTHING;

INSERT INTO wiki_entry(id, headline, body)
VALUES (4, 'Eintrag 1', '<p>Test</p> <p>Test vom Handy<sub>Tief</sub>2</p>'),
       (5, 'Eintrag 2', '<p>Pest</p> <p>Hallo</p>')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wiki_poll(id, question, description, end_date, ended, data)
VALUES (6, 'test', '', '2024-01-01', true, '[
  {
    "votes": [],
    "pollOption": {
      "text": "Option 1",
      "uuid": "ac2b5baf-eebc-46aa-bc7f-613318612110"
    }
  },
  {
    "votes": [],
    "pollOption": {
      "text": "Option zwei",
      "uuid": "3a3341bd-ee0a-4f7a-809a-4702b3e354fe"
    }
  }
]'),
       (7, 'test2', 'Hier eine Beschreibung einfügen.', NULL, false, '[
         {
           "votes": [],
           "pollOption": {
             "text": "Option Nummer 1",
             "uuid": "ac2b5baf-eebc-46aa-bc7f-613318612110"
           }
         },
         {
           "votes": [],
           "pollOption": {
             "text": "Option die zweite",
             "uuid": "3a3341bd-ee0a-4f7a-809a-4702b3e354fe"
           }
         }
       ]')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE wiki_topic ENABLE TRIGGER addWiki;
ALTER TABLE wiki_entry ENABLE TRIGGER addWiki;
ALTER TABLE wiki_poll ENABLE TRIGGER addWiki;

-- Keep the shared sequence ahead of every id inserted above.
SELECT setval('wiki_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM wiki));

COMMIT;
