package com.midascore.kafka.service;

import com.midascore.kafka.model.Incentive;
import com.midascore.kafka.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class IncentiveApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncentiveApiService.class);

    private final RestTemplate restTemplate;
    private final @NonNull String incentiveApiUrl;

    public IncentiveApiService(RestTemplate restTemplate,
                               @Value("${incentive.api.url:http://localhost:8080/incentive}") String incentiveApiUrl) {
        this.restTemplate = restTemplate;
        this.incentiveApiUrl = Objects.requireNonNull(incentiveApiUrl, "Incentive API URL must not be null");
    }

    public BigDecimal fetchIncentiveAmount(Transaction transaction) {
        try {
            Incentive response = restTemplate.postForObject(incentiveApiUrl, transaction, Incentive.class);
            if (response == null || response.getAmount() == null) {
                return BigDecimal.ZERO;
            }
            return response.getAmount().max(BigDecimal.ZERO);
        } catch (Exception exception) {
            LOGGER.warn("Unable to fetch incentive for transaction {} -> {}: {}", transaction.getSender(), transaction.getRecipient(), exception.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
