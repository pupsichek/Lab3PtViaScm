package com.example.pt.lab3.service;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.exception.GameError;
import com.example.pt.lab3.pojo.response.Response;
import com.example.pt.lab3.pojo.response.notification.GameHasBeenUpdatedNotification;
import com.example.pt.lab3.pojo.response.notification.GameHasStartedForOwnerNotification;
import com.example.pt.lab3.pojo.response.notification.GameOverNotification;
import com.example.pt.lab3.pojo.response.notification.NotificationResponse;
import com.example.pt.lab3.pojo.type.NotificationType;
import com.example.pt.lab3.repository.ContractRepository;
import com.example.pt.lab3.repository.GamePartyRepository;
import com.example.pt.lab3.repository.PlayerRepository;
import com.example.pt.lab3.service.assistant.WebSocketSessionAssistant;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

import static com.example.pt.lab3.configuration.ws.Destination.*;
import static com.example.pt.lab3.wrapper.SimpMessagingTemplateWrapper.TEMPLATE_WRAPPER;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriberNotificationService {
    private final WebSocketSessionAssistant webSocketSessionAssistant;
    @Qualifier(value = TEMPLATE_WRAPPER)
    private final SimpMessagingTemplate template;
    private final ResponseCreatorService creatorService;
    private final ContractRepository contractRepository;
    private final GamePartyRepository partyRepository;
    private final PlayerRepository playerRepository;
    private final AsyncExecutor asyncExecutor;

    public void notifyAllSubscribersForGamesEndpointAsync() {
        asyncExecutor.execute(this::notifyAllSubscribersForGamesEndpoint);
    }

    /**
     * Уведомить всех игроков для пункта назначения все игры
     */
    public void notifyAllSubscribersForGamesEndpoint() {
        log.info("Sending notification for all subscribers for main page with games is started");
        Map<String, String> mappingJSessionToWsSession = webSocketSessionAssistant.
                getMappingJSessionToWsSessionByDestination(getSubscribeGamesEndpoint());
        if (MapUtils.isNotEmpty(mappingJSessionToWsSession)) {
            mappingJSessionToWsSession.forEach(
                    (jSessionId, simpSessionId) -> contractRepository.findById(jSessionId).ifPresent(contract -> {
                        PlayerInstance player = contract.getPlayer();
                        Response response = creatorService.getGamesResponseForPlayer(player);
                        template.convertAndSendToUser(simpSessionId, getSendGamesEndpoint(), response);
                        log.info("Notification was sent to destination {}, for user {}", getSendGamesEndpoint(), player.getId());
                        log.debug("Notifications payload {}", response);
                    })
            );
        }
        log.info("Sending notification for all subscribers for main page with games is complete");
    }

    public void notifySpecifiedPlayerWithGamesResponseAsync(UUID playerId) {
        asyncExecutor.execute(() -> notifySpecifiedPlayerWithGamesResponse(playerId));
    }

    /**
     * Уведомить конкретного игрока для пункта назначения со всеми играми
     */
    private void notifySpecifiedPlayerWithGamesResponse(UUID playerId) {
        log.info("Sending notification for specified player {} for main page with games is started asynchronously", playerId);
        playerRepository.findById(playerId).ifPresent(playerInstance ->
                contractRepository.findAllByPlayerId(playerId).forEach(contract -> {
                    String jSessionId = contract.getId();
                    Collection<String> wsSessionIds = webSocketSessionAssistant.getWsSessionIds(getSubscribeGamesEndpoint(), jSessionId);
                    Response response = creatorService.getGamesResponseForPlayer(playerInstance);
                    convertAndSendToUsersInternal(wsSessionIds, getSendGamesEndpoint(), response);
                    log.info("Notification was sent to destination {}, for user {}", getSendGamesEndpoint(), playerId);
                    log.debug("Notifications payload {}", response);
                }));
        log.info("Sending notification for specified player {} for main page with games is complete", playerId);
    }

    public void notifyAllSubscribersForGamesEndpointByCurrentGameAsync(GameInstance game) {
        asyncExecutor.execute(() -> notifyAllSubscribersForGamesEndpointByCurrentGame(game));
    }

    /**
     * Уведомить владельца и игрока если существует для пункта назначения со всеми играми
     */
    private void notifyAllSubscribersForGamesEndpointByCurrentGame(GameInstance game) {
        log.info("Sending notification for all players fo main page is started asynchronously");
        Set<PlayerInstance> userIds = new HashSet<PlayerInstance>() {{
            add(game.getOwner());
        }};
        partyRepository.findByGame(game).ifPresent(party -> userIds.add(party.getPlayer()));
        userIds.forEach(player -> contractRepository.findAllByPlayerId(player.getId()).forEach(contract -> {
            Response response = creatorService.getGamesResponseForPlayer(player);
            String jSessionId = contract.getId();
            Collection<String> wsSessionIds = webSocketSessionAssistant.getWsSessionIds(getSubscribeGamesEndpoint(), jSessionId);
            convertAndSendToUsersInternal(wsSessionIds, getSendGamesEndpoint(), response);
            log.info("Notification was sent to destination {}, for player {}, for simpSessionIds {}",
                    getSubscribeGamesEndpoint(), player.getId(), wsSessionIds);
        }));
        log.info("Sending notification for all players for main page is complete");
    }

    public void notifyAllSubscriptionsForGameEndpointAsync(GameInstance game) {
        asyncExecutor.execute(() -> notifyAllSubscriptionsForGameEndpoint(game));
    }

    /**
     * Уведомить владельца и игрока о доступных действиях в игре
     */
    private void notifyAllSubscriptionsForGameEndpoint(GameInstance game) {
        partyRepository.findByGame(game).ifPresent(party -> {
            log.info("Sending notification for all players for game {} is started", game.getId());
            Response notification = new GameHasBeenUpdatedNotification(game.getName());
            UUID gameId = game.getId();
            String subscribeDestinationEndpoint = getSubscribeGameEndpoint(gameId);
            String sendDestinationEndpoint = getSendGameEndpoint(gameId);
            PlayerInstance player = party.getPlayer();
            PlayerInstance owner = game.getOwner();
            Stream.of(player, owner).forEach(playerInstance -> contractRepository.findAllByPlayerId(playerInstance.getId())
                    .forEach(contract -> {
                                String jSessionId = contract.getId();
                                Collection<String> wsSessionIds = webSocketSessionAssistant.getWsSessionIds(subscribeDestinationEndpoint, jSessionId);
                                if (CollectionUtils.isEmpty(wsSessionIds)) {
                                    log.warn("Web socket sessions is not exist for endpoint {} for player {}", subscribeDestinationEndpoint, playerInstance.getId());
                                    Map<String, Collection<String>> wsSessionIdsWithEndpoint = webSocketSessionAssistant.getWsSessionIdsWithEndpoint(jSessionId);
                                    wsSessionIdsWithEndpoint.forEach((dest, wsSessions) -> {
                                        convertAndSendToUsersInternal(wsSessions, Utils.convertSubscribeDestToSendDest(dest), notification);
                                        log.info("Notification was sent to destination {}, for player {}, for simpSessionIds {}",
                                                Utils.convertSubscribeDestToSendDest(dest), playerInstance.getId(), wsSessionIds);
                                    });
                                } else {
                                    wsSessionIds.forEach(simpSessionId -> {
                                        Response response = getGameResponseForPlayer(game, playerInstance);
                                        template.convertAndSendToUser(simpSessionId, sendDestinationEndpoint, response);
                                        log.info("Response was sent to destination {}, for player {}, simpSessionId {}",
                                                sendDestinationEndpoint, playerInstance.getId(), simpSessionId);
                                    });
                                }
                            }
                    )
            );
        });
        log.info("Sending notification for all players for game {} is complete", game.getId());
    }

    public void notifyOwnerThatGameHasStartedAsync(GameInstance game) {
        asyncExecutor.execute(() -> notifyOwnerThatGameHasStarted(game));
    }

    /**
     * Уведомить владельца игры, что игра началась
     */
    private void notifyOwnerThatGameHasStarted(GameInstance game) {
        PlayerInstance owner = game.getOwner();
        Response ownerNotification = new GameHasStartedForOwnerNotification(game.getName());
        ImmutableList<UUID> playerIds = ImmutableList.of(owner.getId());
        notifyUsersAllDestinationsInternal(ownerNotification, playerIds);
        log.info("Notification that game has started was sent for user {}", owner.getId());
    }

    public void notifyCompleteGameAsync(GameInstance game) {
        asyncExecutor.execute(() -> notifyCompleteGame(game));
    }

    /**
     * Уведомить пользователей об окончании игры
     */
    private void notifyCompleteGame(GameInstance game) {
        Response notification = new GameOverNotification(game.getName());
        Set<UUID> userIds = new HashSet<UUID>() {{
            add(game.getOwner().getId());
        }};
        partyRepository.findByGame(game).ifPresent(party -> userIds.add(party.getPlayer().getId()));
        if (CollectionUtils.isNotEmpty(userIds)) {
            notifyUsersAllDestinationsInternal(notification, userIds);
            log.info("Notification that game deleted was sent for players {}", userIds);
        }
    }

    /**
     * Уведомить всех юзеров предоставленным ответом
     */
    private void notifyUsersAllDestinationsInternal(Response notification, Collection<UUID> userIds) {
        userIds.forEach(userId -> contractRepository.findAllByPlayerId(userId).forEach(contract -> {
            String jSessionId = contract.getId();
            Map<String, Collection<String>> wsSessionIdsWithEndpoint = webSocketSessionAssistant.getWsSessionIdsWithEndpoint(jSessionId);
            wsSessionIdsWithEndpoint.forEach((dest, wsSessions) ->
                    convertAndSendToUsersInternal(wsSessions, Utils.convertSubscribeDestToSendDest(dest), notification));
        }));
    }

    /**
     * Отправить сгенерированный ответ каждому юзера в списке
     * @param users список юзеров
     * @param destination пункт назначения
     */
    private void convertAndSendToUsersInternal(Collection<String> users, String destination, Response payload) throws MessagingException {
        if (CollectionUtils.isNotEmpty(users)) {
            users.forEach(user -> template.convertAndSendToUser(user, destination, payload));
        }
    }

    /**
     * Получить сгенерированный ответ для конкретной игры и для конкретного игрока
     */
    private Response getGameResponseForPlayer(GameInstance game, PlayerInstance player) {
        Response response;
        try {
            response = creatorService.getGameResponseForPlayer(game, player);
        } catch (GameError error) {
            String message = error.getMessage();
            log.error(message, error);
            response = new NotificationResponse(NotificationType.ERROR, message);
        }
        return response;
    }
}
