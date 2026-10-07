package com.example.banfigo.consent.repository;

import com.example.banfigo.consent.entity.Consent;
import com.example.banfigo.consent.entity.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    List<Consent> findAllByOrderByCreatedAtDesc();

    List<Consent> findByStatusOrderByCreatedAtDesc(ConsentStatus status);

    // Marks pending/authorised consents whose expiry time has passed as EXPIRED. Returns the number updated.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Consent c SET c.status = :expired "
            + "WHERE c.expiresAt < :now AND c.status IN :active")
    int expireOverdue(@Param("now") LocalDateTime now,
                      @Param("expired") ConsentStatus expired,
                      @Param("active") List<ConsentStatus> active);
}
