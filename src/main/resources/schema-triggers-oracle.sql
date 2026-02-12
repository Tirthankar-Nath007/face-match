-- ======================================================================
-- ORACLE TRIGGERS FOR FM_CONFIGFIELDS AND FM_TRANSACTION
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

-- Auto-update trigger for fm_configfields.updated_at
CREATE OR REPLACE TRIGGER trg_fm_configfields_updated_at
BEFORE UPDATE ON fm_configfields
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Auto-update trigger for fm_transaction.updated_at  
CREATE OR REPLACE TRIGGER trg_fm_transaction_updated_at
BEFORE UPDATE ON fm_transaction
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- Commit changes
COMMIT;
/

-- Verify trigger creation
SELECT trigger_name, trigger_type, triggering_event, status
FROM user_triggers
WHERE trigger_name LIKE 'TRG_FM%'
ORDER BY trigger_name;
/
