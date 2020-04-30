package com.example.pt.lab3.repository;

import com.example.pt.lab3.domain.GameActionRecord;
import com.example.pt.lab3.domain.GameParty;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface ActionRecordRepository extends JpaRepository<GameActionRecord, UUID> {
    /**
     * Find last action saved in database by game party
     * @param party Game party
     * @return last action record
     */
    Optional<GameActionRecord> findFirstByPartyOrderByDateUpdatedDesc(GameParty party);

    /**
     * Get count of action records where field approved equals true
     * @return count
     */
    long countGameActionRecordByApprovedTrue();

    @Transactional
    /**
     * Remove all approved actions
     */
    void deleteAllByApprovedTrue();
}
