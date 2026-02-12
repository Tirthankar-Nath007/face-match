package com.tvscs.FM.repository;

import com.tvscs.FM.models.ConfigFields;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigFieldRepository extends JpaRepository<ConfigFields, String> {
    Optional<ConfigFields> findByApiKeyAndIsActive(String apiKey, Integer isActive);
    Optional<ConfigFields> findByApiKey(String apiKey);
    Optional<ConfigFields> findByAccountId(String accountId);
    Optional<ConfigFields> findByAccountIdAndIsActive(String accountId, Integer isActive);
    Optional<ConfigFields> findByPortfolioAndIsActive(String portfolio, Integer isActive);
}
