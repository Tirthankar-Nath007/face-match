# FM Application - Quick Start Checklist

## ✅ Prerequisites
- [ ] Java 17 installed
- [ ] Maven 3.8+ installed  
- [ ] Oracle 19c+ database accessible
- [ ] Oracle JDBC connection details ready
- [ ] SQL*Plus or SQLDeveloper for manual DDL execution

## ✅ Step 1: Environment Setup

Create `.env` file in project root:
```
DIGIO_AUTH_TOKEN=your_token_here
FM_JWT_SECRET=generate-a-random-256-bit-secret
FM_ADMIN_API_KEY=generate-admin-api-key
FM_JWT_TTL_MINUTES=15
```

## ✅ Step 2: Database Schema Creation (First Time Only)

### Option A: Automatic (Recommended for first run)

1. Open `src/main/resources/application.yaml`
2. Uncomment the `schema-locations` line in `spring.sql.init` section
3. Change `mode: never` to `mode: always`
4. Run the application: `mvn spring-boot:run`
5. Wait for startup to complete (see "Tables created successfully" in logs)
6. Stop the application (Ctrl+C)
7. Change `mode` back to `never` in application.yaml
8. Uncomment the `schema-locations` line again

### Option B: Manual via SQL*Plus

Connect to Oracle and run:
```bash
sqlplus username/password@database_name @src/main/resources/schema-oracle.sql
```

## ✅ Step 3: Create Database Triggers

Run triggers script via SQL*Plus:
```bash
sqlplus username/password@database_name @src/main/resources/schema-triggers-oracle.sql
```

Verify triggers created:
```sql
SELECT trigger_name, status FROM user_triggers WHERE trigger_name LIKE 'TRG_FM%';
```

Should see 4 triggers:
- `TRG_FM_ACCOUNTS_UPDATED_AT`
- `TRG_FM_TRANSACTIONS_UPDATED_AT`
- `TRG_FM_AUDIT_UPDATED_AT`
- `TRG_FM_TRANSACTIONS_MANDATORY_AUDIT`

## ✅ Step 4: Create Initial API Keys (Admin Setup)

### Using POST /create-account:

```bash
curl -X POST http://localhost:8080/api/v1/create-account \
  -H "X-Admin-API-KEY: $(echo -n 'admin-secret-key' | base64)" \
  -H "X-Requested-By: admin" \
  -H "Content-Type: application/json" \
  -d '{
    "portfolio": "MyPortfolio"
  }'
```

Response will include the configuration ID, apiKey, and accountId.

## ✅ Step 5: Generate JWT Token

```bash
curl -X POST http://localhost:8080/api/v1/generate-token \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "abcd1234efgh5678",
    "accountId": "123456789"
  }'
```

Response:
```json
{
  "token": "eyJhbGc...",
  "expiresAt": "2026-03-10T18:52:00Z"
}
```

## ✅ Step 6: Test Face-Match Endpoint

```bash
curl -X POST http://localhost:8080/api/v1/face-match \
  -H "X-API-KEY: abcd1234efgh5678" \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "customer_name=John Doe" \
  -F "customer_identifier=CUST001" \
  -F "redirect_url=false" \
  -F "image=@/path/to/image.jpg"
```

## ✅ Step 7: Verify Transaction Logging

Query the audit log table:
```sql
SELECT id, vendor_id, endpoint, created_at, created_by 
FROM fm_audit 
ORDER BY created_at DESC 
FETCH FIRST 10 ROWS ONLY;
```

## 🔍 Troubleshooting

### Issue: Tables not created
- [ ] Ensure `mode: always` is set
- [ ] Check Oracle connection in application.yaml
- [ ] Review application logs for SQL errors
- [ ] Verify user has CREATE TABLE privileges

### Issue: ORA-00900 error at startup
- [ ] This is fixed! We removed PL/SQL triggers from automatic execution
- [ ] Ensure `spring.sql.init.mode=never` is set
- [ ] Triggers should be created manually

### Issue: Cannot authenticate to /face-match
- [ ] Verify API key exists in fm_accounts: `SELECT * FROM fm_accounts WHERE api_key = 'your_key';`
- [ ] Verify is_active = 1
- [ ] Verify accountId matches in both API key header and JWT claims
- [ ] Check JWT token hasn't expired

### Issue: No audit logs
- [ ] Check that /face-match requests are being made
- [ ] Review application logs for audit logging errors
- [ ] Verify user has INSERT privilege on fm_audit table

### Issue: Trigger prevents transaction creation
- [ ] The trigger `TRG_FM_TRANSACTIONS_MANDATORY_AUDIT` requires at least one audit log entry before creating a transaction
- [ ] Webhook processing inserts audit log first, then creates/updates transaction
- [ ] For testing, you may need to disable the trigger temporarily

## 📋 Useful SQL Queries

### View API Key Configurations
```sql
SELECT id, api_key, account_id, portfolio, is_active, created_at 
FROM fm_accounts 
ORDER BY created_at DESC;
```

### Count Transactions
```sql
SELECT COUNT(*) as total_transactions, 
       COUNT(DISTINCT vendor_id) as unique_vendors,
       COUNT(DISTINCT created_by) as unique_accounts
FROM fm_audit;
```

### Recent Transactions
```sql
SELECT vendor_id, endpoint, created_by, created_at 
FROM fm_audit 
WHERE created_at > SYSTATE - 7  -- Last 7 days
ORDER BY created_at DESC;
```

### View Audit Logs by Vendor
```sql
SELECT * FROM fm_audit 
WHERE vendor_id = 'KID260211170917566L9QB7LFSO9XB4L'
ORDER BY created_at DESC;
```

### Deactivate API Key
```sql
UPDATE fm_accounts 
SET is_active = 0, updated_by = 'admin' 
WHERE api_key = 'abcd1234efgh5678';
COMMIT;
```

## 🚀 Build & Run

### Development
```bash
mvn clean install
mvn spring-boot:run
```

### Production (JAR)
```bash
mvn clean package -DskipTests
java -jar target/FM-0.0.1-SNAPSHOT.jar
```

## 📝 Next Steps
- Review DATABASE_SETUP.md for detailed configuration
- Configure logging levels (currently set to DEBUG for com.tvscs.FM)
- Set up rate limiting if needed
- Configure CORS for cross-origin requests
- Enable HTTPS for production

---
For detailed documentation, see: [DATABASE_SETUP.md](./DATABASE_SETUP.md)
