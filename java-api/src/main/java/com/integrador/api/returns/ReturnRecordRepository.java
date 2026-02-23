package com.integrador.api.returns;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    long deleteByProductId(Long productId);
}
