package com.tvscs.FM.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fm_configfields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFields {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "api_key", length = 16, nullable = false, unique = true)
    private String apiKey;

    @Column(name = "account_id", length = 9, nullable = false)
    private String accountId;

    @Column(name = "portfolio", length = 10, nullable = false)
    private String portfolio;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    // Explicit getters for clarity (Lombok @Data should generate these)
    public String getApiKey() {
        return apiKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPortfolio() {
        return portfolio;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public String getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    // Setters
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
