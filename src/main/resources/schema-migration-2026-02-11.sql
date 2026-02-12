-- ============================================================================
-- FM DATABASE MIGRATION - Rename tables and create new structures
-- ============================================================================
-- Execute in SQL Developer or SQL*Plus
-- ============================================================================

-- 1. Rename existing fm_transaction to fm_audit
ALTER TABLE fm_transaction RENAME TO fm_audit;

-- 2. Rename associated objects (if any indexes/triggers exist)
-- Check for indexes on old table name
SELECT index_name FROM user_indexes WHERE table_name = 'FM_AUDIT';

-- Rename trigger if needed (Oracle doesn't support direct rename, drop and recreate)
-- DROP TRIGGER trg_fm_transaction_updated_at;
-- (Recreate trigger below if needed)

-- 3. Create new FM_TRANSACTIONS table for business state (no duplicates)
CREATE TABLE fm_transactions (
    id                 VARCHAR2(36)      NOT NULL,
    kid                VARCHAR2(64)      NOT NULL,
    status             VARCHAR2(50),
    reference_id       VARCHAR2(64),
    transaction_id     VARCHAR2(64),
    created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_fm_transactions PRIMARY KEY (id),
    CONSTRAINT uq_fm_transactions_kid UNIQUE (kid)
);

-- 4. Create trigger for auto-updating updated_at on fm_transactions
CREATE OR REPLACE TRIGGER trg_fm_transactions_updated_at
BEFORE UPDATE ON fm_transactions
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- 5. Create index for faster lookups
CREATE INDEX ix_fm_transactions_kid ON fm_transactions (kid);
CREATE INDEX ix_fm_transactions_status ON fm_transactions (status);

-- 6. Rename old indexes on fm_audit (if needed)
-- Oracle doesn't support renaming indexes directly, recreate if needed

-- 7. Create FM_REQUEST_RESPONSE_LOG table for centralized audit logging
CREATE TABLE fm_request_response_log (
    id                     VARCHAR2(36)          NOT NULL,
    endpoint               VARCHAR2(4000)        NOT NULL,
    http_method            VARCHAR2(10),
    request_payload        CLOB,
    response_payload       CLOB,
    http_status            INTEGER,
    account_id             VARCHAR2(100),
    portfolio              VARCHAR2(100),
    api_key_hash           VARCHAR2(64),
    client_ip              VARCHAR2(45),
    user_agent             VARCHAR2(500),
    request_duration_ms    BIGINT,
    is_error               NUMBER(1)             CHECK (is_error IN (0, 1)),
    error_message          VARCHAR2(4000),
    created_at             TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_fm_request_response_log PRIMARY KEY (id)
);

-- 8. Create trigger for auto-updating updated_at on fm_request_response_log
CREATE OR REPLACE TRIGGER trg_fm_request_response_log_updated_at
BEFORE UPDATE ON fm_request_response_log
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- 9. Create indexes for fm_request_response_log
CREATE INDEX ix_rrl_endpoint ON fm_request_response_log (endpoint);
CREATE INDEX ix_rrl_account_id ON fm_request_response_log (account_id);
CREATE INDEX ix_rrl_created_at ON fm_request_response_log (created_at);
CREATE INDEX ix_rrl_is_error ON fm_request_response_log (is_error);
CREATE INDEX ix_rrl_http_status ON fm_request_response_log (http_status);

-- 10. Verify tables
SELECT table_name FROM user_tables WHERE table_name IN (
    'FM_AUDIT', 'FM_TRANSACTIONS', 'FM_CONFIGFIELDS', 'FM_REQUEST_RESPONSE_LOG'
);

-- 11. Verify triggers
SELECT trigger_name, status FROM user_triggers WHERE trigger_name IN (
    'TRG_FM_TRANSACTIONS_UPDATED_AT', 'TRG_FM_REQUEST_RESPONSE_LOG_UPDATED_AT'
);

-- 12. Verify indexes
SELECT index_name, table_name FROM user_indexes WHERE table_name IN (
    'FM_AUDIT', 'FM_TRANSACTIONS', 'FM_REQUEST_RESPONSE_LOG'
);
