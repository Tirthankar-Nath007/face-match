package com.tvscs.FM.services;

import com.tvscs.FM.models.Account;
import com.tvscs.FM.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    private static final Pattern PORTFOLIO_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(String portfolio, String createdBy) {
        if (portfolio == null || portfolio.isEmpty()) {
            throw new IllegalArgumentException("Portfolio name is required");
        }

        if (!PORTFOLIO_PATTERN.matcher(portfolio).matches()) {
            throw new IllegalArgumentException("Portfolio name must be 1-10 alphanumeric characters");
        }

        String transactionId = UUID.randomUUID().toString().toLowerCase();

        String apiKey = generateRandomApiKey();
        String accountId = generateAccountId();

        Account account = new Account();
        account.setApiKey(apiKey);
        account.setAccountId(accountId);
        account.setPortfolio(portfolio);
        account.setCreatedBy(createdBy);
        account.setIsActive(1);
        account.setTransactionId(transactionId);

        Account saved = accountRepository.save(account);
        log.info("Account created successfully: id={}, accountId={}, portfolio={}, apiKey={}", 
                saved.getId(), accountId, portfolio, apiKey);

        return saved;
    }

    public Account updateAccount(String accountId, Boolean rotateKey, String newApiKey, String portfolio, Integer isActive, String updatedBy) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with accountId: " + accountId));

        if (Boolean.TRUE.equals(rotateKey)) {
            account.setApiKey(generateRandomApiKey());
        }
        else if (newApiKey != null && !newApiKey.isEmpty()) {
            if (!isValidApiKey(newApiKey)) {
                throw new IllegalArgumentException("Invalid new API key format");
            }

            Optional<Account> existing = accountRepository.findByApiKey(newApiKey);
            if (existing.isPresent() && !existing.get().getId().equals(account.getId())) {
                throw new IllegalArgumentException("New API key already exists");
            }

            account.setApiKey(newApiKey);
        }

        if (portfolio != null && !portfolio.isEmpty()) {
            if (!PORTFOLIO_PATTERN.matcher(portfolio).matches()) {
                throw new IllegalArgumentException("Portfolio must be 1-10 alphanumeric characters");
            }
            account.setPortfolio(portfolio);
        }

        if (isActive != null) {
            if (isActive != 0 && isActive != 1) {
                throw new IllegalArgumentException("is_active must be 0 or 1");
            }
            account.setIsActive(isActive);
        }

        account.setUpdatedBy(updatedBy);

        Account updated = accountRepository.save(account);
        log.info("Account updated successfully: accountId={}, updatedBy={}", accountId, updatedBy);

        return updated;
    }

    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    private String generateRandomApiKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder apiKey = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int index = (int) (Math.random() * chars.length());
            apiKey.append(chars.charAt(index));
        }
        return apiKey.toString();
    }

    private String generateAccountId() {
        String chars = "0123456789";
        StringBuilder accountId = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            int index = (int) (Math.random() * chars.length());
            accountId.append(chars.charAt(index));
        }
        return accountId.toString();
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && apiKey.matches("^[A-Za-z0-9]{16}$");
    }
}
