CREATE TYPE wiki_type AS ENUM ('TOPIC', 'ENTRY', 'POLL');

CREATE TABLE IF NOT EXISTS wiki
(
    id   integer PRIMARY KEY,
    type wiki_type
);

CREATE SEQUENCE IF NOT EXISTS wiki_id_seq AS integer MINVALUE 0 START WITH 0 INCREMENT BY 1 CACHE 1;
ALTER SEQUENCE wiki_id_seq OWNED BY wiki.id;

CREATE TABLE IF NOT EXISTS wiki_topic
(
    id       integer PRIMARY KEY,
    topic    text NOT NULL,
    child_id integer[] DEFAULT ARRAY []::integer[],
    author VARCHAR(255) NOT NULL DEFAULT 'unknown_user',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0)
);

CREATE TABLE IF NOT EXISTS wiki_entry
(
    id       integer PRIMARY KEY,
    headline text NOT NULL,
    body     text NOT NULL,
    author VARCHAR(255) NOT NULL DEFAULT 'unknown_user',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0)
);

CREATE TABLE IF NOT EXISTS wiki_poll
(
    id          integer PRIMARY KEY,
    question    text NOT NULL,
    description text,
    end_date    date,
    ended       boolean DEFAULT false,
    data        jsonb,
    author VARCHAR(255) NOT NULL DEFAULT 'unknown_user',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp(0)
);

ALTER TABLE ONLY wiki
    ALTER COLUMN id SET DEFAULT nextval('wiki_id_seq'::regclass);
ALTER TABLE ONLY wiki_entry
    ALTER COLUMN id SET DEFAULT nextval('wiki_id_seq'::regclass);
ALTER TABLE ONLY wiki_poll
    ALTER COLUMN id SET DEFAULT nextval('wiki_id_seq'::regclass);
ALTER TABLE ONLY wiki_topic
    ALTER COLUMN id SET DEFAULT nextval('wiki_id_seq'::regclass);

ALTER TABLE wiki_entry ALTER COLUMN id DROP DEFAULT;
ALTER TABLE wiki_poll ALTER COLUMN id DROP DEFAULT;
ALTER TABLE wiki_topic ALTER COLUMN id DROP DEFAULT;

ALTER TABLE wiki_entry ADD CONSTRAINT fk_wiki_entry_wiki FOREIGN KEY (id) REFERENCES wiki (id) ON DELETE CASCADE;
ALTER TABLE wiki_poll ADD CONSTRAINT fk_wiki_poll_wiki FOREIGN KEY (id) REFERENCES wiki (id) ON DELETE CASCADE;
ALTER TABLE wiki_topic ADD CONSTRAINT fk_wiki_topic_wiki FOREIGN KEY (id) REFERENCES wiki (id) ON DELETE CASCADE;

CREATE OR REPLACE FUNCTION addWiki()
    RETURNS trigger
    LANGUAGE plpgsql
AS
$$
DECLARE
wiki_type_value wiki_type;
BEGIN
CASE TG_TABLE_NAME
        WHEN 'wiki_entry' THEN
            wiki_type_value := 'ENTRY';
WHEN 'wiki_poll' THEN
            wiki_type_value := 'POLL';
WHEN 'wiki_topic' THEN
            wiki_type_value := 'TOPIC';
ELSE
            RAISE EXCEPTION 'Unsupported wiki table: %', TG_TABLE_NAME;
END CASE;

INSERT INTO wiki (type)
VALUES (wiki_type_value)
    RETURNING id INTO NEW.id;
RETURN NEW;
END;
$$;

CREATE TRIGGER addWiki
    BEFORE INSERT
    ON wiki_entry
    FOR EACH ROW
    EXECUTE FUNCTION addWiki();

CREATE TRIGGER addWiki
    BEFORE INSERT
    ON wiki_poll
    FOR EACH ROW
    EXECUTE FUNCTION addWiki();

CREATE TRIGGER addWiki
    BEFORE INSERT
    ON wiki_topic
    FOR EACH ROW
    EXECUTE FUNCTION addWiki();

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = current_timestamp(0);
RETURN NEW;
END;
$$;

CREATE TRIGGER update_wiki_topic_modtime
    BEFORE UPDATE ON wiki_topic
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_wiki_entry_modtime
    BEFORE UPDATE ON wiki_entry
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER update_wiki_poll_modtime
    BEFORE UPDATE ON wiki_poll
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE OR REPLACE FUNCTION delete_wiki_entry()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM wiki WHERE id = OLD.id;
    RETURN OLD;
END;
$$;

CREATE TRIGGER delete_wiki_topic_entry
    AFTER DELETE ON wiki_topic
    FOR EACH ROW EXECUTE FUNCTION delete_wiki_entry();

CREATE TRIGGER delete_wiki_entry_entry
    AFTER DELETE ON wiki_entry
    FOR EACH ROW EXECUTE FUNCTION delete_wiki_entry();

CREATE TRIGGER delete_wiki_poll_entry
    AFTER DELETE ON wiki_poll
    FOR EACH ROW EXECUTE FUNCTION delete_wiki_entry();

INSERT INTO wiki_topic (topic)
VALUES ('Startseite');
