package com.tvscs.FM.repository;

import com.tvscs.FM.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByApiKeyAndIsActive(String apiKey, Integer isActive);
    Optional<Account> findByApiKey(String apiKey);
    Optional<Account> findByAccountId(String accountId);
    Optional<Account> findByAccountIdAndIsActive(String accountId, Integer isActive);
    Optional<Account> findByPortfolioAndIsActive(String portfolio, Integer isActive);
}
