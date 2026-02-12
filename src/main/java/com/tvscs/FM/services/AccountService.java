package com.tvscs.FM.services;

import com.tvscs.FM.models.ConfigFields;
import com.tvscs.FM.repository.ConfigFieldRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AccountService {

    private final ConfigFieldRepository configFieldRepository;

    // Pattern for validating portfolio
    private static final Pattern PORTFOLIO_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");

    public AccountService(ConfigFieldRepository configFieldRepository) {
        this.configFieldRepository = configFieldRepository;
    }

    /**
     * Create a new API key configuration for a portfolio account.
     * Generates API key and account ID automatically.
     *
     * @param portfolio   The portfolio name
     * @param createdBy   The user creating this account
     * @return The created ConfigFields entity with auto-generated apiKey and accountId
     * @throws IllegalArgumentException if validation fails
     */
    public ConfigFields createAccount(String portfolio, String createdBy) {
        // Validate portfolio format
        if (portfolio == null || portfolio.isEmpty()) {
            throw new IllegalArgumentException("Portfolio name is required");
        }

        if (!PORTFOLIO_PATTERN.matcher(portfolio).matches()) {
            throw new IllegalArgumentException("Portfolio name must be 1-10 alphanumeric characters");
        }

        // Generate UUID for id
        String configFieldId = UUID.randomUUID().toString();

        // Generate random 16-character alphanumeric API key
        String apiKey = generateRandomApiKey();

        // Generate 9-character numeric account ID
        String accountId = generateAccountId();

        // Create and persist the entity
        ConfigFields configFields = new ConfigFields();
        configFields.setId(configFieldId);
        configFields.setApiKey(apiKey);
        configFields.setAccountId(accountId);
        configFields.setPortfolio(portfolio);
        configFields.setCreatedBy(createdBy);
        configFields.setIsActive(1);

        ConfigFields saved = configFieldRepository.save(configFields);
        log.info("Account created successfully: id={}, accountId={}, portfolio={}, apiKey={}", 
                saved.getId(), accountId, portfolio, apiKey);

        return saved;
    }

    /**
     * Update an existing API key configuration.
     * Allows rotation of API key (via rotateKey flag or newApiKey), toggling is_active, and changing portfolio.
     *
     * @param accountId     The business account identifier (9-digit numeric)
     * @param rotateKey     Flag to auto-generate a new API key
     * @param newApiKey     Custom new API key (nullable)
     * @param portfolio     New portfolio (nullable)
     * @param isActive      New is_active value (nullable)
     * @param updatedBy     The user performing the update
     * @return The updated ConfigFields entity
     * @throws IllegalArgumentException if account not found or validation fails
     */
    public ConfigFields updateAccount(String accountId, Boolean rotateKey, String newApiKey, String portfolio, Integer isActive, String updatedBy) {
        ConfigFields configFields = configFieldRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with accountId: " + accountId));

        // If rotateKey is true, auto-generate a new API key
        if (Boolean.TRUE.equals(rotateKey)) {
            configFields.setApiKey(generateRandomApiKey());
        }
        // If custom API key is provided (and rotateKey is not true), validate and use it
        else if (newApiKey != null && !newApiKey.isEmpty()) {
            if (!isValidApiKey(newApiKey)) {
                throw new IllegalArgumentException("Invalid new API key format");
            }

            // Check if the new key is already in use by another record
            Optional<ConfigFields> existing = configFieldRepository.findByApiKey(newApiKey);
            if (existing.isPresent() && !existing.get().getId().equals(configFields.getId())) {
                throw new IllegalArgumentException("New API key already exists");
            }

            configFields.setApiKey(newApiKey);
        }

        // Update portfolio if provided
        if (portfolio != null && !portfolio.isEmpty()) {
            if (!PORTFOLIO_PATTERN.matcher(portfolio).matches()) {
                throw new IllegalArgumentException("Portfolio must be 1-10 alphanumeric characters");
            }
            configFields.setPortfolio(portfolio);
        }

        // Update is_active if provided
        if (isActive != null) {
            if (isActive != 0 && isActive != 1) {
                throw new IllegalArgumentException("is_active must be 0 or 1");
            }
            configFields.setIsActive(isActive);
        }

        // Set updated_by
        configFields.setUpdatedBy(updatedBy);

        ConfigFields updated = configFieldRepository.save(configFields);
        log.info("Account updated successfully: accountId={}, updatedBy={}", accountId, updatedBy);

        return updated;
    }

    /**
     * Retrieve a configuration by ID.
     *
     * @param configFieldId The ID to retrieve
     * @return The ConfigFields entity or empty Optional if not found
     */
    public Optional<ConfigFields> getAccountById(String configFieldId) {
        return configFieldRepository.findById(configFieldId);
    }

    /**
     * Generate a random 16-character alphanumeric API key.
     * Format: Mix of uppercase and numbers to ensure it passes regex constraint
     */
    private String generateRandomApiKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder apiKey = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int index = (int) (Math.random() * chars.length());
            apiKey.append(chars.charAt(index));
        }
        return apiKey.toString();
    }

    /**
     * Generate a random 9-character numeric account ID.
     */
    private String generateAccountId() {
        String chars = "0123456789";
        StringBuilder accountId = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            int index = (int) (Math.random() * chars.length());
            accountId.append(chars.charAt(index));
        }
        return accountId.toString();
    }

    /**
     * Validate API key format (16 alphanumeric characters).
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && apiKey.matches("^[A-Za-z0-9]{16}$");
    }
}
