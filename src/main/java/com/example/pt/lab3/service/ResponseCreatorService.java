package com.example.pt.lab3.service;

import com.example.pt.lab3.domain.GameActionRecord;
import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.GameParty;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.domain.enumeration.GameState;
import com.example.pt.lab3.exception.internal.GameNotEndedButNumberGuessed;
import com.example.pt.lab3.pojo.action.PartyAction;
import com.example.pt.lab3.pojo.response.content.GameResponseForPlayer;
import com.example.pt.lab3.pojo.response.content.GamesResponseForPlayer;
import com.example.pt.lab3.repository.ActionRecordRepository;
import com.example.pt.lab3.repository.GamePartyRepository;
import com.example.pt.lab3.repository.GameRepository;
import com.example.pt.lab3.util.RandomUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.example.pt.lab3.configuration.GameConfiguration.OPTIMISTIC_RETRY;
import static com.example.pt.lab3.exception.ErrorFactory.notMemberForGame;
import static com.example.pt.lab3.exception.ErrorFactory.playerIsTheOwnerOfTheGame;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class ResponseCreatorService {

    private static final String GAME_ALREADY_ENDED = "Game already ended";
    private static final String LAST_ACTION_ALREADY_APPROVED = "Last action already approved";
    private static final String GAME_IS_NOT_APPROVED_YET = "Game is not approved yet";
    private final GameRepository gameRepository;
    private final GamePartyRepository partyRepository;
    private final ActionRecordRepository recordRepository;
    @Qualifier(OPTIMISTIC_RETRY)
    private final RetryTemplate retryTemplate;

    /**
     * Get games where current player is owner
     *
     * @return Page with games
     * @see GameRepository#findAllByOwnerAndStateIsIn(PlayerInstance, Collection) this method find all games
     * for current player, where this player is owner and where game states is specified in the collection
     */
    private Collection<GameInstance> getGamesByOwner(PlayerInstance owner) {
        return gameRepository.findAllByOwnerAndStateIsIn(owner, ImmutableList.of(GameState.CREATED, GameState.RUNNING));
    }

    /**
     * Get available games for playing
     *
     * @return Page with games
     * @see GameRepository#findAllByOwnerNotAndStateIsIn(PlayerInstance, Collection) this method find all games
     * for current player, where this player is not owner and where game states is specified in the collection
     */
    private Collection<GameInstance> getGamesNotForOwner(PlayerInstance owner) {
        return gameRepository.findAllByOwnerNotAndStateIsIn(owner, ImmutableList.of(GameState.CREATED));
    }

    /**
     * Get games where current player is playing
     *
     * @return Page with games
     * @see GamePartyRepository#findAllByPlayerAndGame_StateIsIn(PlayerInstance, Collection) this method find all parties
     * for current player where game states is specified in the collection
     */
    private Collection<GameInstance> getGamesWherePlayerAct(PlayerInstance player) {
        return partyRepository.findAllByPlayerAndGame_StateIsIn(player, ImmutableList.of(GameState.RUNNING))
                .stream()
                .map(GameParty::getGame)
                .collect(toList());
    }

    /**
     * Create response for Player, which includes available games, created games, current playing games
     * @return response for current player
     */
    public GamesResponseForPlayer getGamesResponseForPlayer(PlayerInstance player) {
        Collection<GameInstance> gamesForOwner = getGamesByOwner(player);
        Collection<GameInstance> gamesNotForOwner = getGamesNotForOwner(player);
        Collection<GameInstance> gamesForPlayer = getGamesWherePlayerAct(player);
        return new GamesResponseForPlayer(player, gamesForOwner, gamesNotForOwner, gamesForPlayer);
    }

    /**
     * Create response for player for specified game
     * @param gameId Game identifier
     * @return response
     * @see GameResponseForPlayer
     */
    public GameResponseForPlayer getGameResponseForPlayer(UUID gameId, PlayerInstance player) {
        GameInstance game = gameRepository.mustFindById(gameId);
        return getGameResponseForPlayer(game, player);
    }

    /**
     * Create response for player for specified game
     * @return response
     * @see GameResponseForPlayer
     */
    GameResponseForPlayer getGameResponseForPlayer(GameInstance game, PlayerInstance player) {
        GameParty party = partyRepository.mustFindByGame(game);
        boolean isOwner = player.getId().equals(game.getOwner().getId());
        boolean isPlayer = player.getId().equals(party.getPlayer().getId());
        if (isOwner && isPlayer) {
            throw playerIsTheOwnerOfTheGame(game.getId()); // владелец не может играть в свою игру
        }

        Optional<GameActionRecord> lastActionRecordOpt = recordRepository.findFirstByPartyOrderByDateUpdatedDesc(party);

        if (isOwner) {
            if (lastActionRecordOpt.isPresent()) {
                GameActionRecord record = lastActionRecordOpt.get();
                if (record.isResult()) {
                    return new GameResponseForPlayer(player, game, true, Collections.emptyList(), GAME_ALREADY_ENDED);
                } else if (record.isApproved()) {
                    return new GameResponseForPlayer(player, game, false, Collections.emptyList(), LAST_ACTION_ALREADY_APPROVED);
                } else {

                    if (Objects.equals(game.getNumber(), record.getTryNumber())) {
                        throw new GameNotEndedButNumberGuessed();
                    }

                    long countApproved = recordRepository.countGameActionRecordByApprovedTrue();

                    Integer random = RandomUtils.random();
                    retryTemplate.execute(ctx -> {
                        if (!record.isCanLie() && random < countApproved) { // if true set can lie property is true
                            recordRepository.deleteAllByApprovedTrue();
                            record.setCanLie(true);
                            recordRepository.save(record);
                        }
                        return null;
                    });


                    String content = "Check following player number: " + record.getTryNumber();

                    if (record.isCanLie()) {
                        return new GameResponseForPlayer(player, game, false, ImmutableSet.of(PartyAction.LESS_NUMBER, PartyAction.MORE_NUMBER), content);
                    }

                    Collection<PartyAction> availableActions = record.getTryNumber() > game.getNumber() ?
                            Collections.singleton(PartyAction.LESS_NUMBER) :
                            Collections.singleton(PartyAction.MORE_NUMBER);

                    return new GameResponseForPlayer(player, game, false, availableActions, content);
                }
            } else {
                return new GameResponseForPlayer(player, game, false, Collections.emptyList());
            }
        }

        if (isPlayer) {
            if (lastActionRecordOpt.isPresent()) {
                GameActionRecord record = lastActionRecordOpt.get();
                if (record.isResult()) {
                    return new GameResponseForPlayer(player, game, true, Collections.emptyList(), GAME_ALREADY_ENDED);
                } else if (record.isNotApproved()) {
                    return new GameResponseForPlayer(player, game, false, Collections.emptyList(), GAME_IS_NOT_APPROVED_YET);
                } else {
                    String content = null;
                    if (record.getAction().equals(PartyAction.LESS_NUMBER)) {
                        content = "The owner answer that the number is less than " + record.getTryNumber();
                    }
                    if (record.getAction().equals(PartyAction.MORE_NUMBER)) {
                        content = "The owner answer that the number is greater than " + record.getTryNumber();
                    }
                    if (StringUtils.isEmpty(content)) {
                        throw new NullPointerException();
                    }
                    return new GameResponseForPlayer(player, game, false, Collections.singleton(PartyAction.GUESS_NUMBER), content);
                }
            } else {
                return new GameResponseForPlayer(player, game, false, Collections.singleton(PartyAction.GUESS_NUMBER), "Guess a first number");
            }
        }

        throw notMemberForGame(game.getId());
    }
}
