CREATE TABLE url_mappings (
                              short_url   VARCHAR(8)  PRIMARY KEY,
                              long_url    TEXT,
                              creator_id  BIGINT,
                              expiry_time TIMESTAMPTZ NOT NULL DEFAULT TIMESTAMPTZ 'epoch'
);

CREATE INDEX idx_url_mappings_expiry ON url_mappings (expiry_time);