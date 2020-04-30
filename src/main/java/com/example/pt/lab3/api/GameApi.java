package com.example.pt.lab3.api;

import com.example.pt.lab3.domain.GameActionRecord;
import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.GameParty;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.domain.enumeration.GameState;
import com.example.pt.lab3.exception.ErrorFactory;
import com.example.pt.lab3.pojo.action.PartyAction;
import com.example.pt.lab3.pojo.request.ApproveRequest;
import com.example.pt.lab3.pojo.request.GameCreateRequest;
import com.example.pt.lab3.pojo.request.GuessRequest;
import com.example.pt.lab3.repository.ActionRecordRepository;
import com.example.pt.lab3.repository.GamePartyRepository;
import com.example.pt.lab3.repository.GameRepository;
import com.example.pt.lab3.service.AsyncExecutor;
import com.example.pt.lab3.service.SubscriberNotificationService;
import com.example.pt.lab3.service.assistant.CurrentHttpSessionAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.example.pt.lab3.configuration.GameConfiguration.OPTIMISTIC_RETRY;
import static com.example.pt.lab3.exception.ErrorFactory.*;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class GameApi {

    private final GameRepository gameRepository;
    private final GamePartyRepository partyRepository;
    private final ActionRecordRepository recordRepository;
    private final CurrentHttpSessionAssistant sessionAssistant;
    @Qualifier(OPTIMISTIC_RETRY)
    private final RetryTemplate retryTemplate;
    private final SubscriberNotificationService notificationService;
    private final AsyncExecutor asyncExecutor;

    /**
     * Create new GameInstance from GameCreateRequest
     * Scheduling remove gameInstance via GameCleanerService
     *
     * @param createRequest pojo which contains all needed fields to create GameInstance
     * @see GameInstance created game
     * @see GameCreateRequest param of this method
     */
    public void createGame(GameCreateRequest createRequest) {
        PlayerInstance currentPlayer = sessionAssistant.getCurrentPlayer();
        asyncExecutor.execute(() -> {
            GameInstance draftGame = GameInstance
                    .builder()
                    .name(createRequest.getName())
                    .number(createRequest.getNumber())
                    .state(GameState.CREATED)
                    .owner(currentPlayer)
                    .build();
            gameRepository.save(draftGame);
            notificationService.notifyAllSubscribersForGamesEndpoint();
        });
    }

    /**
     * Start game for current user
     * Method get current player and create GameParty for current game and current player
     *
     * @param gameId selected game
     * @see GameParty
     * @see CurrentHttpSessionAssistant#getCurrentContract()
     */
    public void playGame(UUID gameId) {
        retryTemplate.execute(ctx -> {
            validatePlayGame(gameId);
            PlayerInstance currentPlayer = sessionAssistant.getCurrentPlayer();
            GameInstance game = gameRepository.mustFindById(gameId);
            if (GameState.RUNNING.equals(game.getState())) {
                throw gameAlreadyIsRunning(gameId);
            }
            game.setState(GameState.RUNNING);
            GameInstance savedGame = gameRepository.save(game);
            partyRepository.save(GameParty.builder().game(savedGame).player(currentPlayer).build());
            notificationService.notifyOwnerThatGameHasStartedAsync(game);
            notificationService.notifyAllSubscribersForGamesEndpointAsync();
            return null;
        });
    }

    /**
     * Check that current player is not owner of this game, owner can not play in his game
     * Then check the game is not ended or running, because player can not play in game which already running or ended
     *
     * @param gameId simpSessionId of game
     */
    private void validatePlayGame(UUID gameId) {
        PlayerInstance currentPlayer = sessionAssistant.getCurrentPlayer();
        GameInstance game = gameRepository.mustFindById(gameId);
        if (currentPlayer.getId().equals(game.getOwner().getId())) {
            throw cannotPlayInCreatedGame(gameId);
        }
        if (GameState.RUNNING.equals(game.getState()) || GameState.ENDED.equals(game.getState())) {
            throw cannotPlayInRunningOrEndedGame(gameId);
        }
    }

    public void guess(UUID gameId, GuessRequest guessRequest) {
        retryTemplate.execute(ctx -> {
            GameParty party = partyRepository.mustFindByGameId(gameId);
            GameInstance game = party.getGame();
            guessRequest.setGameId(gameId);
            checkSameUser(party.getPlayer());
            Optional<GameActionRecord> recordOpt = recordRepository.findFirstByPartyOrderByDateUpdatedDesc(party);
            if (recordOpt.isPresent()) {
                GameActionRecord actionRecord = recordOpt.get();
                if (actionRecord.isNotApproved()) {
                    throw cannotGuessNumberIfNotApprovedYet(guessRequest.getNumber(), gameId);
                }
                if (actionRecord.isResult()) {
                    throw noActionForEndedGame(gameId);
                }
            }
            GameActionRecord draft = GameActionRecord.builder()
                    .approved(false)
                    .party(party)
                    .result(false)
                    .tryNumber(guessRequest.getNumber())
                    .action(PartyAction.GUESS_NUMBER)
                    .build();
            if (game.getNumber().equals(guessRequest.getNumber())) {
                completeGame(game, draft);
            }
            recordRepository.save(draft);
            notificationService.notifyAllSubscriptionsForGameEndpointAsync(game);
            return null;
        });
    }

    private void completeGame(GameInstance game, GameActionRecord actionRecord) {
        game.setState(GameState.ENDED);
        gameRepository.save(game);
        notificationService.notifyAllSubscribersForGamesEndpointByCurrentGameAsync(game);
        notificationService.notifyCompleteGameAsync(game);
        actionRecord.setResult(true);
    }

    public void approve(UUID gameId, ApproveRequest approveRequest) {
        retryTemplate.execute(ctx -> {
            GameParty party = partyRepository.mustFindByGameId(gameId);
            approveRequest.setGameId(gameId);

            checkSameUser(party.getGame().getOwner());

            GameActionRecord actionRecord = recordRepository.findFirstByPartyOrderByDateUpdatedDesc(party)
                    .orElseThrow(ErrorFactory::approveIsNotAvailableBeforeGuess);

            if (actionRecord.isResult()) {
                throw noActionForEndedGame(gameId);
            }
            if (actionRecord.isApproved()) {
                throw approveIsNotAvailableIfAlreadyApproved();
            }

            actionRecord.setApproved(true);
            actionRecord.setAction(approveRequest.getAction());
            recordRepository.save(actionRecord);

            notificationService.notifyAllSubscriptionsForGameEndpointAsync(party.getGame());
            return null;
        });
    }

    private void checkSameUser(PlayerInstance player) {
        PlayerInstance currentPlayer = sessionAssistant.getCurrentPlayer();
        if (!currentPlayer.getId().equals(player.getId())) {
            throw actionIsNotAvailableForUser();
        }
    }
}
