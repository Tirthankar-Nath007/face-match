-- ==============================
-- Table: FM_CONFIGFIELDS
-- ==============================
CREATE TABLE fm_configfields (
  id            VARCHAR2(36)      NOT NULL,
  api_key       CHAR(16)          NOT NULL,
  account_id    CHAR(9)           NOT NULL,
  portfolio     VARCHAR2(10)      NOT NULL,
  created_by    VARCHAR2(100),
  created_at    TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at    TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by    VARCHAR2(100),
  is_active     NUMBER(1)         DEFAULT 1 NOT NULL,
  CONSTRAINT pk_fm_configfields PRIMARY KEY (id),
  CONSTRAINT uq_fm_configfields_api_key UNIQUE (api_key),
  CONSTRAINT ck_fm_configfields_api_key CHECK (REGEXP_LIKE(api_key, '^[A-Za-z0-9]{16}$')),
  CONSTRAINT ck_fm_configfields_account_id CHECK (REGEXP_LIKE(account_id, '^[A-Za-z0-9]{9}$')),
  CONSTRAINT ck_fm_configfields_is_active CHECK (is_active IN (0,1))
);

CREATE INDEX ix_fm_configfields_account_id ON fm_configfields (account_id);
CREATE INDEX ix_fm_configfields_portfolio  ON fm_configfields (portfolio);
CREATE INDEX ix_fm_configfields_is_active  ON fm_configfields (is_active);

-- ==============================
-- Table: FM_TRANSACTION
-- ==============================
CREATE TABLE fm_transaction (
  id                 VARCHAR2(36)      NOT NULL,
  fm_transaction_id  VARCHAR2(64)      NOT NULL,
  vendor_id          VARCHAR2(128),
  endpoint           VARCHAR2(4000)    NOT NULL,
  payload            CLOB              NOT NULL,
  response           CLOB,
  created_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  created_by         VARCHAR2(100),
  updated_at         TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by         VARCHAR2(100),
  CONSTRAINT pk_fm_transaction PRIMARY KEY (id),
  CONSTRAINT uq_fm_transaction_fm_transaction_id UNIQUE (fm_transaction_id),
  CONSTRAINT ck_fm_transaction_payload_json CHECK (payload IS JSON),
  CONSTRAINT ck_fm_transaction_response_json CHECK (response IS JSON)
);

CREATE INDEX ix_fm_transaction_created_at ON fm_transaction (created_at);
CREATE INDEX ix_fm_transaction_vendor_id  ON fm_transaction (vendor_id);
