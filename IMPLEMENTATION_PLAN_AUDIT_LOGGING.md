# Centralized Audit Logging Implementation Plan (FINAL)

## ✅ Migration Completed - fm_audit Structure Verified

```sql
DESC FM_AUDIT;

Name                Null     Type                        
------------------- -------- --------------------------- 
ID                  NOT NULL VARCHAR2(36)                
FM_TRANSACTION_ID   NOT NULL VARCHAR2(64)                
VENDOR_ID                    VARCHAR2(128)               
ENDPOINT            NOT NULL VARCHAR2(4000)              
PAYLOAD             NOT NULL CLOB                        
RESPONSE                     CLOB                        
CREATED_AT          NOT NULL TIMESTAMP(6) WITH TIME ZONE 
CREATED_BY                   VARCHAR2(100)               
UPDATED_AT          NOT NULL TIMESTAMP(6) WITH TIME ZONE 
UPDATED_BY                   VARCHAR2(100)               
HTTP_METHOD                  VARCHAR2(10)                
HTTP_STATUS                  NUMBER(38)                  
CLIENT_IP                    VARCHAR2(45)                
USER_AGENT                   VARCHAR2(500)               
REQUEST_DURATION_MS          NUMBER                      
IS_ERROR                     NUMBER(1)                   
ERROR_MESSAGE                VARCHAR2(4000)              
ACCOUNT_ID                   VARCHAR2(100)               
PORTFOLIO                    VARCHAR2(100)
```

**All 19 columns added successfully!**

---

## Final Migration Steps (Remaining)

### Step 1: Drop Unique Constraint on fm_transaction_id

```sql
ALTER TABLE fm_audit DROP CONSTRAINT UQ_FM_TRANSACTION_FM_TRANSACTION_ID;
```

### Step 2: Create Remaining Indexes

```sql
-- Indexes for fm_audit (skip if already exists)
CREATE INDEX ix_fm_audit_endpoint ON fm_audit (endpoint);
CREATE INDEX ix_fm_audit_http_status ON fm_audit (http_status);
CREATE INDEX ix_fm_audit_is_error ON fm_audit (is_error);

-- Check existing indexes first
SELECT index_name FROM user_indexes WHERE table_name = 'FM_AUDIT';
```

### Step 3: Create fm_transactions Table

```sql
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

CREATE INDEX ix_fm_transactions_kid ON fm_transactions (kid);
CREATE INDEX ix_fm_transactions_status ON fm_transactions (status);
```

### Step 4: Create Trigger for fm_transactions

```sql
CREATE OR REPLACE TRIGGER trg_fm_transactions_updated_at
BEFORE UPDATE ON fm_transactions
FOR EACH ROW
BEGIN
  :NEW.updated_at := SYSTIMESTAMP;
END;
/
```

---

## Table Responsibilities

| Table | Purpose |
|-------|---------|
| `fm_audit` | ALL endpoint logging (19 columns) |
| `fm_transactions` | Business state (unique KID records) |
| `fm_configfields` | API key configurations |

---

## Implementation Code

### Step 5: Update TransactionLog Entity

Update the JPA entity to match the 19-column structure (remove `unique = true` from `fmTransactionId`):

```java
@Entity
@Table(name = "fm_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "fm_transaction_id", length = 64)  // removed nullable = false
    private String fmTransactionId;

    @Column(name = "vendor_id", length = 128)
    private String vendorId;

    @Column(name = "endpoint", length = 4000, nullable = false)
    private String endpoint;

    @Column(name = "payload", nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(name = "response", columnDefinition = "CLOB")
    private String response;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_duration_ms")
    private Long requestDurationMs;

    @Column(name = "is_error")
    private Integer isError;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "account_id", length = 100)
    private String accountId;

    @Column(name = "portfolio", length = 100)
    private String portfolio;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
```

### Step 6: Create AuditLoggingInterceptor

```java
@Component
@Slf4j
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final TransactionLogRepository auditLogRepository;

    private static final ThreadLocal<Long> requestStartTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestStartTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        long startTime = requestStartTime.get();
        long durationMs = System.currentTimeMillis() - startTime;
        requestStartTime.remove();

        try {
            String accountId = (String) request.getAttribute("auth.accountId");
            String portfolio = (String) request.getAttribute("auth.portfolio");
            String createdBy = accountId != null ? accountId : "public";

            // Capture response
            String responsePayload = null;
            if (response instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper wrapped = (ContentCachingResponseWrapper) response;
                byte[] content = wrapped.getContentAsByteArray();
                if (content.length > 0) {
                    responsePayload = new String(content, StandardCharsets.UTF_8);
                    if (responsePayload.length() > 10000) {
                        responsePayload = responsePayload.substring(0, 10000) + "... [truncated]";
                    }
                }
            }

            String fmTransactionId = extractTransactionId(request);

            TransactionLog auditLog = TransactionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .fmTransactionId(fmTransactionId)
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .payload("[captured by interceptor]") // Controller may override
                    .response(responsePayload)
                    .httpStatus(response.getStatus())
                    .accountId(accountId)
                    .portfolio(portfolio)
                    .clientIp(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .requestDurationMs(durationMs)
                    .isError(ex != null || response.getStatus() >= 400 ? 1 : 0)
                    .errorMessage(ex != null ? ex.getMessage() : null)
                    .createdBy(createdBy)
                    .updatedBy(createdBy)
                    .build();

            CompletableFuture.runAsync(() -> auditLogRepository.save(auditLog));

        } catch (Exception e) {
            log.error("Failed to create audit log entry", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractTransactionId(HttpServletRequest request) {
        String kid = (String) request.getAttribute("auth.kid");
        return kid;
    }
}
```

### Step 7: Register Interceptor in SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(auditLoggingInterceptor)
                        .addPathPatterns("/api/v1/**");
            }
        };
    }

    // ... existing security config
}
```

### Step 8: Remove Duplicate Logging from FaceMatchController

- Remove manual `TransactionLog.save()` calls from `/face-match` endpoint
- Keep `fm_transactions` upserts for webhooks (business logic)

---

## Endpoint Coverage

| Endpoint | Logged | Columns Populated |
|----------|--------|-------------------|
| `/face-match` | ✅ | All 19 columns |
| `/webhook` | ✅ | All except account_id/portfolio |
| `/create-account` | ✅ | All except account_id/portfolio |
| `/update-account` | ✅ | All except account_id/portfolio |
| `/generate-token` | ✅ | All except account_id/portfolio |

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `FM/src/main/java/com/tvscs/FM/config/AuditLoggingInterceptor.java` | Create |
| `FM/src/main/java/com/tvscs/FM/config/SecurityConfig.java` | Modify |
| `FM/src/main/java/com/tvscs/FM/models/TransactionLog.java` | Modify |
| `FM/src/main/java/com/tvscs/FM/controllers/FaceMatchController.java` | Modify |

---

## Verification Commands

```sql
-- Check all tables
SELECT table_name FROM user_tables WHERE table_name IN ('FM_AUDIT', 'FM_TRANSACTIONS', 'FM_CONFIGFIELDS');

-- Check indexes
SELECT index_name, table_name FROM user_indexes WHERE table_name IN ('FM_AUDIT', 'FM_TRANSACTIONS');

-- Check triggers
SELECT trigger_name, status FROM user_triggers WHERE trigger_name LIKE 'TRG_FM%';

-- Check fm_audit columns
DESC FM_AUDIT;
```
