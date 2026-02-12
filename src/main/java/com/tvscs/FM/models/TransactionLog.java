package com.tvscs.FM.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

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

    @Column(name = "fm_transaction_id", length = 64)
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
