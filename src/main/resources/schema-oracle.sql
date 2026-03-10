-- ==============================
-- Table: FM_ACCOUNTS (formerly FM_CONFIGFIELDS)
-- ==============================
CREATE TABLE fm_accounts (
  id                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  api_key            CHAR(16)          NOT NULL,
  account_id         CHAR(9)           NOT NULL,
  portfolio          VARCHAR2(10)      NOT NULL,
  created_by         VARCHAR2(100),
  created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by         VARCHAR2(100),
  is_active          NUMBER(1)         DEFAULT 1 NOT NULL,
  transaction_id     VARCHAR2(36)      NOT NULL UNIQUE,
  CONSTRAINT ck_fm_accounts_api_key CHECK (REGEXP_LIKE(api_key, '^[A-Za-z0-9]{16}$')),
  CONSTRAINT ck_fm_accounts_account_id CHECK (REGEXP_LIKE(account_id, '^[A-Za-z0-9]{9}$')),
  CONSTRAINT ck_fm_accounts_is_active CHECK (is_active IN (0,1)),
  CONSTRAINT uq_fm_accounts_api_key UNIQUE (api_key)
);

CREATE INDEX ix_fm_accounts_account_id ON fm_accounts (account_id);
CREATE INDEX ix_fm_accounts_portfolio  ON fm_accounts (portfolio);
CREATE INDEX ix_fm_accounts_is_active  ON fm_accounts (is_active);

-- ==============================
-- Table: FM_TRANSACTIONS
-- ==============================
CREATE TABLE fm_transactions (
  id                      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  vendor_id               VARCHAR2(64)      NOT NULL UNIQUE,
  status                  VARCHAR2(50),
  vendor_reference_id     VARCHAR2(64),
  vendor_transaction_id   VARCHAR2(64),
  transaction_id          VARCHAR2(36)      NOT NULL UNIQUE,
  created_at              TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at              TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX ix_fm_transactions_vendor_id ON fm_transactions (vendor_id);
CREATE INDEX ix_fm_transactions_created_at ON fm_transactions (created_at);
CREATE INDEX ix_fm_transactions_status ON fm_transactions (status);

-- ==============================
-- Table: FM_AUDIT
-- ==============================
CREATE TABLE fm_audit (
  id                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  vendor_id          VARCHAR2(64),
  endpoint           VARCHAR2(4000)    NOT NULL,
  payload            CLOB              NOT NULL,
  response           CLOB,
  http_method        VARCHAR2(10),
  http_status        NUMBER,
  client_ip          VARCHAR2(45),
  user_agent         VARCHAR2(500),
  request_duration_ms NUMBER,
  is_error           NUMBER(1),
  error_message      VARCHAR2(4000),
  account_id         VARCHAR2(100),
  portfolio          VARCHAR2(100),
  transaction_id     VARCHAR2(36)      NOT NULL UNIQUE,
  created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  created_by         VARCHAR2(100),
  updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by         VARCHAR2(100),
  CONSTRAINT ck_fm_audit_is_error CHECK (is_error IN (0,1))
);

CREATE INDEX ix_fm_audit_vendor_id ON fm_audit (vendor_id);
CREATE INDEX ix_fm_audit_created_at ON fm_audit (created_at);
CREATE INDEX ix_fm_audit_account_id ON fm_audit (account_id);
CREATE INDEX ix_fm_audit_endpoint ON fm_audit (endpoint);
CREATE INDEX ix_fm_audit_http_status ON fm_audit (http_status);
CREATE INDEX ix_fm_audit_is_error ON fm_audit (is_error);

-- Foreign Key: FM_AUDIT.VENDOR_ID -> FM_TRANSACTIONS.VENDOR_ID
-- (nullable, enforced only for non-null values via trigger)
ALTER TABLE fm_audit
  ADD CONSTRAINT fk_fm_audit_vendor_id
  FOREIGN KEY (vendor_id)
  REFERENCES fm_transactions (vendor_id)
  ENABLE NOVALIDATE;
