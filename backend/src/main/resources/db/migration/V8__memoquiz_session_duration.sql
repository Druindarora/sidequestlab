ALTER TABLE memoquiz_session
    ADD COLUMN ended_at TIMESTAMPTZ,
    ADD COLUMN duration_seconds INTEGER,
    ADD CONSTRAINT memoquiz_session_duration_seconds_non_negative
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0);
