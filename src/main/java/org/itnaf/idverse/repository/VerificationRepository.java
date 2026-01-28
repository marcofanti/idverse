package org.itnaf.idverse.repository;

import org.itnaf.idverse.model.VerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationRepository extends JpaRepository<VerificationRecord, Long> {

    List<VerificationRecord> findByReferenceId(String referenceId);

    List<VerificationRecord> findByPhoneNumber(String phoneNumber);
}
