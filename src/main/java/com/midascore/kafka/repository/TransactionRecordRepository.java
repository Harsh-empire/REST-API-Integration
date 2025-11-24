package com.midascore.kafka.repository;

import com.midascore.kafka.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
}
