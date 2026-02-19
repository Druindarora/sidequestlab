CREATE TABLE memoquiz_settings (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
