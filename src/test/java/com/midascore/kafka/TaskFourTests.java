package com.midascore.kafka;

import com.midascore.kafka.model.Transaction;
import com.midascore.kafka.model.UserAccount;
import com.midascore.kafka.repository.UserAccountRepository;
import com.midascore.kafka.service.TransactionProcessorService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TaskFourTests {

    private static final MockWebServer INCENTIVE_SERVER = createServer();

    private static MockWebServer createServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start incentive mock server", exception);
        }
        return server;
    }

    @DynamicPropertySource
    static void overrideIncentiveApiUrl(DynamicPropertyRegistry registry) {
        registry.add("incentive.api.url", () -> INCENTIVE_SERVER.url("/incentive").toString());
    }

    @Autowired
    private TransactionProcessorService transactionProcessorService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUpAccounts() {
        userAccountRepository.deleteAll();
        List<UserAccount> accounts = new ArrayList<>();
        accounts.add(new UserAccount("alice", new BigDecimal("1000.00")));
        accounts.add(new UserAccount("bob", new BigDecimal("500.00")));
        accounts.add(new UserAccount("charlie", new BigDecimal("300.00")));
        accounts.add(new UserAccount("wilbur", new BigDecimal("150.00")));
        userAccountRepository.saveAll(accounts);
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        INCENTIVE_SERVER.shutdown();
    }

    @Test
    void processesTransactionsAndCapturesWilburBalance() {
        enqueueIncentive("10.00");
        enqueueIncentive("0.00");
        enqueueIncentive("5.00");
        enqueueIncentive("2.00");
        enqueueIncentive("1.50");

        List<Transaction> transactions = List.of(
                new Transaction("alice", "wilbur", new BigDecimal("120.00")),
                new Transaction("wilbur", "bob", new BigDecimal("30.00")),
                new Transaction("bob", "wilbur", new BigDecimal("70.00")),
                new Transaction("alice", "bob", new BigDecimal("60.00")),
                new Transaction("charlie", "wilbur", new BigDecimal("25.00"))
        );

        transactionProcessorService.processTransactions(transactions);

        BigDecimal wilburBalance = userAccountRepository.findById("wilbur")
                .orElseThrow()
                .getBalance();

        assertThat(wilburBalance).isEqualByComparingTo(new BigDecimal("351.50"));
    }

    private static void enqueueIncentive(String amount) {
        String body = String.format("{\"amount\": %s}", amount);
        INCENTIVE_SERVER.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));
    }
}
