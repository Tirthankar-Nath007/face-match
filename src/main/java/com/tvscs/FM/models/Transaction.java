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
@Table(name = "fm_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "vendor_id", length = 64, nullable = false, unique = true)
    private String vendorId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "vendor_reference_id", length = 64)
    private String vendorReferenceId;

    @Column(name = "vendor_transaction_id", length = 64)
    private String vendorTransactionId;

    @Column(name = "transaction_id", length = 36, nullable = false, unique = true)
    private String transactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
