package com.example.pt.lab3.repository;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.domain.enumeration.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

import static com.example.pt.lab3.exception.ErrorFactory.gameNotFound;

@Repository
public interface GameRepository extends JpaRepository<GameInstance, UUID> {
    /**
     * Get all games by owner and if game not in states
     * @param owner Game owner
     * @param states Game states
     */
    Collection<GameInstance> findAllByOwnerNotAndStateIsIn(PlayerInstance owner, Collection<GameState> states);

    /**
     * Get all games by owner and if game in states
     * @param owner Game owner
     * @param states Game states
     */
    Collection<GameInstance> findAllByOwnerAndStateIsIn(PlayerInstance owner, Collection<GameState> states);

    default GameInstance mustFindById(UUID gameId) {
        return findById(gameId).orElseThrow(() -> gameNotFound(gameId));
    }
}
