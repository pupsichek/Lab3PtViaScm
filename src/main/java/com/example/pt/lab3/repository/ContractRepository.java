package com.example.pt.lab3.repository;

import com.example.pt.lab3.domain.ContractPlayerIdWithJSessionId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<ContractPlayerIdWithJSessionId, String> {
    /**
     * Get all contracts by player identifier
     */
    Collection<ContractPlayerIdWithJSessionId> findAllByPlayerId(UUID playerId);

    default ContractPlayerIdWithJSessionId mustFindById(String jSessionId) {
        ContractPlayerIdWithJSessionId contract = findById(jSessionId).orElseThrow(() -> new OptimisticLockingFailureException("Contract should be presented"));
        contract.setUpdatedDate(LocalDateTime.now());
        return save(contract);
    }
}
