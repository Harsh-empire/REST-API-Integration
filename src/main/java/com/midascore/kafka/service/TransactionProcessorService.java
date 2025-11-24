package com.midascore.kafka.service;

import com.midascore.kafka.model.Transaction;
import com.midascore.kafka.model.TransactionRecord;
import com.midascore.kafka.model.UserAccount;
import com.midascore.kafka.repository.TransactionRecordRepository;
import com.midascore.kafka.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Objects;

@Service
public class TransactionProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionProcessorService.class);

    private final UserAccountRepository userAccountRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveApiService incentiveApiService;

    public TransactionProcessorService(UserAccountRepository userAccountRepository,
                                       TransactionRecordRepository transactionRecordRepository,
                                       IncentiveApiService incentiveApiService) {
        this.userAccountRepository = userAccountRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveApiService = incentiveApiService;
    }

    @Transactional
    public void processTransactions(Collection<Transaction> transactions) {
        if (CollectionUtils.isEmpty(transactions)) {
            return;
        }
        transactions.forEach(this::processTransaction);
    }

    @Transactional
    public TransactionRecord processTransaction(Transaction transaction) {
        Transaction validated = validateTransaction(transaction);

        UserAccount sender = findAccount(validated.getSender());
        UserAccount recipient = findAccount(validated.getRecipient());

        BigDecimal amount = validated.getAmount();
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds for sender " + sender.getUsername());
        }

        BigDecimal incentive = incentiveApiService.fetchIncentiveAmount(validated);

        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount).add(incentive));

        userAccountRepository.save(sender);
        userAccountRepository.save(recipient);

        TransactionRecord record = new TransactionRecord(
                sender.getUsername(),
                recipient.getUsername(),
                amount,
                incentive,
                validated.getCreatedAt() != null ? validated.getCreatedAt() : OffsetDateTime.now()
        );

        TransactionRecord saved = transactionRecordRepository.save(record);
        LOGGER.info("Processed transaction {} -> {} | amount={}, incentive={}",
            sender.getUsername(), recipient.getUsername(), amount, incentive);
        return saved;
    }

    private Transaction validateTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction must be provided");
        if (!StringUtils.hasText(transaction.getSender())) {
            throw new IllegalArgumentException("Sender username must be provided");
        }
        if (!StringUtils.hasText(transaction.getRecipient())) {
            throw new IllegalArgumentException("Recipient username must be provided");
        }
        if (transaction.getSender().equals(transaction.getRecipient())) {
            throw new IllegalArgumentException("Sender and recipient must be different");
        }
        BigDecimal amount = transaction.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(OffsetDateTime.now());
        }
        if (transaction.getId() == null) {
            transaction.setId(java.util.UUID.randomUUID());
        }
        return transaction;
    }

    private UserAccount findAccount(String username) {
        Objects.requireNonNull(username, "Username must not be null");
        return userAccountRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for username: " + username));
    }
}
