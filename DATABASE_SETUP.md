# FM (Face Match) - Oracle Database Setup Guide

## Overview
The FM application uses Oracle 19c+ with two main tables:
- `fm_configfields`: Stores API key configurations
- `fm_transaction`: Stores face-match transaction logs

## Database Initialization

### Step 1: Create Tables (Automatic on First Run)

The application automatically creates tables and indexes when started for the first time. However, **automatic trigger creation is disabled** because Spring Boot's JDBC script executor does not support Oracle PL/SQL syntax.

**To enable automatic table creation:**

1. Edit `src/main/resources/application.yaml`
2. Change `spring.sql.init.mode` from `never` to `always`
3. Start the application once
4. Change it back to `never` to prevent re-execution

```yaml
spring:
  sql:
    init:
      mode: always  # Change to 'always' on first run only
      schema-locations: classpath:schema-oracle.sql
```

### Step 2: Create Triggers (Manual)

Triggers must be created manually because they use Oracle PL/SQL syntax (`BEGIN...END;` blocks) which requires the `/` statement terminator that JDBC doesn't support.

**Using SQL*Plus:**
```bash
sqlplus username/password@database_name @src/main/resources/schema-triggers-oracle.sql
```

**Using SQLDeveloper:**
1. Open SQL*Plus worksheet
2. Open `src/main/resources/schema-triggers-oracle.sql`
3. Execute the script (F5 or Ctrl+Enter)
4. Verify triggers were created:
   ```sql
   SELECT trigger_name, status FROM user_triggers WHERE trigger_name LIKE 'TRG_FM%';
   ```

### Step 3: Verify Schema Creation

After tables and triggers are created, verify the structure:

```sql
-- Check tables
DESC fm_configfields;
DESC fm_transaction;

-- Check indexes
SELECT index_name, table_name FROM user_indexes 
WHERE table_name IN ('fm_configfields', 'fm_transaction');

-- Check triggers
SELECT trigger_name, status FROM user_triggers 
WHERE trigger_name LIKE 'TRG_FM%';

-- Check constraints
SELECT constraint_name, constraint_type, table_name 
FROM user_constraints 
WHERE table_name IN ('FM_CONFIGFIELDS', 'FM_TRANSACTION');
```

## Application Configuration

### Environment Variables (.env file)

Create a `.env` file in the project root:

```env
# Digio API Configuration
DIGIO_AUTH_TOKEN=your_digio_auth_token
DIGIO_EXPIRE_DAYS=90
DIGIO_TEMPLATE_NAME=SELFIE COMPARE
CALLBACK_URL=https://your-callback-url.com

# FM (Face Match) Security Configuration
FM_JWT_SECRET=your-256-bit-secret-key-change-this-in-production
FM_JWT_TTL_MINUTES=15
FM_ADMIN_API_KEY=your-admin-api-key

# Logging
LOG_LEVEL=DEBUG
```

### application.yaml Configuration

The `application.yaml` is pre-configured with:
- Oracle JDBC connection details
- JPA/Hibernate settings (ddl-auto=none)
- Spring Security settings
- Logging configuration

**Important Notes:**
- `spring.jpa.hibernate.ddl-auto=none` (Spring does NOT auto-create/drop tables)
- `spring.sql.init.mode=never` (Manual schema initialization)
- Database credentials are in the YAML (move to environment variables for production)

## Running the Application

### First Time Setup:

1. Ensure Oracle database is accessible
2. Set `spring.sql.init.mode=always` in `application.yaml`
3. Run: `mvn spring-boot:run` or start from IDE
4. Wait for tables to be created
5. Change `spring.sql.init.mode` back to `never`
6. Manually create triggers using SQL*Plus (see Step 2 above)
7. Restart the application

### Subsequent Runs:

```bash
mvn spring-boot:run
```

The application will start with:
- Tables already created
- Triggers already created
- Ready to accept requests

## API Endpoints

### Public Endpoints
- `POST /api/v1/generate-token` - Generate JWT token (no auth required)
- `POST /api/v1/webhook` - Receive vendor webhooks (no auth required)

### Admin-Protected Endpoints (require X-Admin-API-KEY header)
- `POST /api/v1/create-account` - Create API key configuration
- `PUT /api/v1/update-account/{configFieldId}` - Update account configuration

### Authenticated Endpoints (require X-API-KEY + Bearer JWT)
- `POST /api/v1/face-match` - Submit face-match request

## Troubleshooting

### Tables Not Created
- Check Oracle database connectivity
- Verify `spring.sql.init.mode=always` is set
- Check logs for SQL errors
- Ensure `schema-oracle.sql` is in `src/main/resources/`

### Triggers Not Working
- Run `schema-triggers-oracle.sql` manually via SQL*Plus
- Verify trigger status: `SELECT * FROM user_triggers WHERE trigger_name LIKE 'TRG_FM%';`
- Check for `INVALID` triggers and recompile if needed

### ORA-00900: Invalid SQL Statement
- This appears when JDBC tries to execute PL/SQL directly
- Solution: Use `spring.sql.init.mode=never` and load triggers manually

### Database Connection Issues
- Verify Oracle JDBC URL format: `jdbc:oracle:thin:@//HOST:PORT/SERVICE`
- Check username/password
- Ensure firewall allows connection to Oracle port (usually 1521)
- Test with SQL*Plus: `sqlplus username/password@hostname:port/service_name`

## Data Retention & Cleanup

Implement regular cleanup jobs for `fm_transaction`:
```sql
-- Archive old transactions (>90 days)
DELETE FROM fm_transaction WHERE created_at < TRUNC(SYSDATE) - 90;
COMMIT;
```

## Security Notes

1. **Change FM_JWT_SECRET** immediately in production
2. **Change FM_ADMIN_API_KEY** immediately in production
3. Move all credentials to a secrets vault (AWS Secrets Manager, HashiCorp Vault)
4. Enable Oracle user with minimal privileges (SELECT, INSERT, UPDATE on FM tables only)
5. Use SSL for database connections
6. Enable audit logging for sensitive transactions

## Performance Tuning

### Index Usage
Indexes are pre-created on:
- `fm_configfields`: account_id, portfolio, is_active
- `fm_transaction`: created_at, vendor_id

### Query Optimization
For high-volume logging, consider:
- Partitioning `fm_transaction` by created_at (monthly)
- Archiving old transactions to separate table
- Creating filtered indexes on is_active=1

### Connection Pooling
HikariCP is configured with default settings. Adjust if needed:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## Flyway Migration (Optional)

For better database version control, implement Flyway:

1. Add Flyway dependency to pom.xml
2. Create migrations in `src/main/resources/db/migration/`
3. Set `spring.sql.init.mode=never` to avoid conflicts

Example: `V1__Initial_Schema.sql` would contain both tables and triggers.

---

For issues or questions, review the application logs and Oracle error codes.
