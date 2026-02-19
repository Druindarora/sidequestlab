-- Migration to change SMALLINT columns to INTEGER to match JPA `int` fields
-- Uses USING cast to be safe when changing types

ALTER TABLE card_progress
    ALTER COLUMN box TYPE integer USING box::integer;

ALTER TABLE memoquiz_session
    ALTER COLUMN day_index TYPE integer USING day_index::integer;

ALTER TABLE memoquiz_session_item
    ALTER COLUMN box TYPE integer USING box::integer;

ALTER TABLE memoquiz_review_log
    ALTER COLUMN previous_box TYPE integer USING previous_box::integer,
    ALTER COLUMN next_box TYPE integer USING next_box::integer;

ALTER TABLE memoquiz_quiz_card
    ALTER COLUMN box TYPE integer USING box::integer;
