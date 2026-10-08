package com.example.banfigo.transfer.repository;

import com.example.banfigo.transfer.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, String> {
}
