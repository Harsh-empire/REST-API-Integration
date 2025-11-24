package com.midascore.kafka.model;

import java.math.BigDecimal;

public class Incentive {

    private BigDecimal amount = BigDecimal.ZERO;

    public Incentive() {
    }

    public Incentive(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
