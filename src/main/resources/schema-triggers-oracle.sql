-- ======================================================================
-- ORACLE TRIGGERS FOR FM_ACCOUNTS, FM_TRANSACTIONS, AND FM_AUDIT
-- ======================================================================
-- These triggers must be executed manually using SQL*Plus, SQLDeveloper, 
-- or a tool that understands Oracle PL/SQL syntax.
-- 
-- Spring Boot's JDBC script executor does not support Oracle's "/" 
-- statement terminator for PL/SQL blocks. Execute this file separately 
-- AFTER the main schema-oracle.sql has initialized the tables.
--
-- Usage in SQL*Plus:
--   SQL> @schema-triggers-oracle.sql
--
-- ======================================================================

-- Auto-update trigger for fm_accounts.updated_at
CREATE OR REPLACE TRIGGER trg_fm_accounts_updated_at
BEFORE UPDATE ON fm_accounts
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Auto-update trigger for fm_transactions.updated_at
CREATE OR REPLACE TRIGGER trg_fm_transactions_updated_at
BEFORE UPDATE ON fm_transactions
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Auto-update trigger for fm_audit.updated_at
CREATE OR REPLACE TRIGGER trg_fm_audit_updated_at
BEFORE UPDATE ON fm_audit
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Trigger to enforce "no lonely transactions" - at least one audit log must exist
-- before a transaction can be created/updated with a VENDOR_ID
CREATE OR REPLACE TRIGGER trg_fm_transactions_mandatory_audit
AFTER INSERT OR UPDATE OF vendor_id ON fm_transactions
FOR EACH ROW
DECLARE
  v_count NUMBER;
BEGIN
  IF :NEW.vendor_id IS NOT NULL THEN
    SELECT COUNT(*)
    INTO v_count
    FROM fm_audit
    WHERE vendor_id = :NEW.vendor_id;

    IF v_count = 0 THEN
      RAISE_APPLICATION_ERROR(-20001, 
        'Cannot create/update transaction without at least one matching audit log (VENDOR_ID = ' || :NEW.vendor_id || ')');
    END IF;
  END IF;
END;
/

-- Commit changes
COMMIT;

-- Verify trigger creation
SELECT trigger_name, trigger_type, triggering_event, status
FROM user_triggers
WHERE trigger_name LIKE 'TRG_FM%'
ORDER BY trigger_name;
