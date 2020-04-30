package com.example.pt.lab3.repository;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.GameParty;
import com.example.pt.lab3.domain.enumeration.GameState;
import com.example.pt.lab3.domain.PlayerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static com.example.pt.lab3.exception.ErrorFactory.partyNotFound;

@Repository
public interface GamePartyRepository extends JpaRepository<GameParty, UUID> {
    /**
     * Get game party by game
     */
    Optional<GameParty> findByGame(GameInstance game);

    /**
     * Get game party by game identifier
     */
    Optional<GameParty> findByGame_Id(UUID gameId);
    Collection<GameParty> findAllByPlayerAndGame_StateIsIn(PlayerInstance player, Collection<GameState> states);

    default GameParty mustFindByGame(GameInstance game) {
        return findByGame(game).orElseThrow(() -> partyNotFound(game.getId()));
    }

    default GameParty mustFindByGameId(UUID gameId) {
        return findByGame_Id(gameId).orElseThrow(() -> partyNotFound(gameId));
    }
}
