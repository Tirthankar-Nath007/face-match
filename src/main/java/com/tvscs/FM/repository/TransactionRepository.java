package com.tvscs.FM.repository;

import com.tvscs.FM.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByKid(String kid);
    boolean existsByKid(String kid);
}
