CREATE TABLE memoquiz_session (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    day_index SMALLINT NOT NULL CHECK (day_index >= 1 AND day_index <= 64)
);

CREATE TABLE memoquiz_session_item (
    session_id BIGINT NOT NULL REFERENCES memoquiz_session(id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    box SMALLINT NOT NULL,
    PRIMARY KEY (session_id, card_id)
);

CREATE TABLE memoquiz_review_log (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES memoquiz_session(id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES card(id) ON DELETE CASCADE,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    answer_text VARCHAR(10000) NOT NULL,
    correct BOOLEAN NOT NULL,
    previous_box SMALLINT NOT NULL,
    next_box SMALLINT NOT NULL
);
