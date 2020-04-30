package com.example.pt.lab3.listener;

import com.example.pt.lab3.configuration.ws.Destination;
import com.example.pt.lab3.domain.ContractPlayerIdWithJSessionId;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.pojo.response.Response;
import com.example.pt.lab3.pojo.response.content.GameResponseForPlayer;
import com.example.pt.lab3.pojo.response.content.GamesResponseForPlayer;
import com.example.pt.lab3.pojo.response.error.InternalErrorResponse;
import com.example.pt.lab3.repository.ContractRepository;
import com.example.pt.lab3.service.ResponseCreatorService;
import com.example.pt.lab3.service.assistant.WebSocketSessionAssistant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Optional;
import java.util.UUID;

import static com.example.pt.lab3.util.WebSocketUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketListener {
    private final WebSocketSessionAssistant webSocketSessionAssistant;
    private final ResponseCreatorService creatorService;
    private final SimpMessagingTemplate template;
    private final ContractRepository contractRepository;

    @EventListener(SessionConnectedEvent.class)
    public void handleWsConnectedEvent(SessionConnectedEvent event) {
        String simpSessionId = getSessionIdFromEvent(event);
        log.info("Connected event, simpSessionId {}", simpSessionId);
        log.debug("Connected event {}", event);
    }

    @EventListener(SessionConnectEvent.class)
    public void handleWsConnectEvent(SessionConnectEvent event) {
        String simpSessionId = getSessionIdFromEvent(event);
        log.info("Connect event, simpSessionId {}", simpSessionId);
        log.debug("Connect event {}", event);
    }

    @EventListener(SessionSubscribeEvent.class)
    public void handleWsSubscribeEvent(SessionSubscribeEvent event) {
        ContractPlayerIdWithJSessionId currentContract = getContract(event);
        String simpSessionId = getSessionIdFromEvent(event);
        String simpDestination = getSimpDestinationFromEvent(event);
        String jSessionId = currentContract.getId();
        log.info("Subscribe event, simpSessionId {}, simpDestination {}", simpSessionId, simpDestination);
        log.debug("Subscribe event {}", event);
        webSocketSessionAssistant.persist(simpDestination, jSessionId, simpSessionId);

        PlayerInstance currentPlayer = currentContract.getPlayer();

        sendResponseInformationAfterSubscribe(simpSessionId, simpDestination, currentPlayer);

        log.info("Current subscription persisted and mapped to JSESSIONID {}", jSessionId);
    }

    /**
     * Send response after subscribe or send notifications
     * if destination is single game => get identifier id from destination and create Response for game via getGameResponseForPlayer
     * @see ResponseCreatorService#getGameResponseForPlayer(UUID, PlayerInstance)
     * if destination is all games => create Response for games via getGamesResponseForPlayer
     * @see ResponseCreatorService#getGamesResponseForPlayer(PlayerInstance)
     * if destination is unknown => send InternalErrorResponse with error message
     * @see InternalErrorResponse
     * @param simpSessionId webSocket Session Id
     * @param simpDestination web socket destination
     * @param currentPlayer Current Player
     */
    private void sendResponseInformationAfterSubscribe(String simpSessionId, String simpDestination, PlayerInstance currentPlayer) {
        try {
            if (Destination.Utils.isSpecifyGameDestination(simpDestination)) {
                Optional<UUID> gameId = Destination.Utils.getGameIdFromSpecifiedGameEndpoint(simpDestination);
                if (!gameId.isPresent()) {
                    throw new RuntimeException("Game id " + Destination.Utils.getIdAsStrFromSpecifiedGameEndpoint(simpDestination) + " is not correct");
                }
                String sendDest = Destination.Utils.convertSubscribeDestToSendDest(simpDestination);
                gameId.ifPresent(id -> {
                    GameResponseForPlayer response = creatorService.getGameResponseForPlayer(id, currentPlayer);
                    template.convertAndSendToUser(simpSessionId, sendDest, response);
                });
            } else if (Destination.Utils.isGamesDestination(simpDestination)) {
                GamesResponseForPlayer response = creatorService.getGamesResponseForPlayer(currentPlayer);
                String sendDest = Destination.Utils.convertSubscribeDestToSendDest(simpDestination);
                template.convertAndSendToUser(simpSessionId, sendDest, response);
            } else {
                throw new RuntimeException("This destination is not supported");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            Response response = new InternalErrorResponse(e.getMessage());
            String sendDest = Destination.Utils.convertSubscribeDestToSendDest(simpDestination);
            template.convertAndSendToUser(simpSessionId, sendDest, response);
            webSocketSessionAssistant.removeBySimpSessionId(simpSessionId);
        }
    }

    @EventListener(SessionDisconnectEvent.class)
    public void handleWsDisconnectEvent(SessionDisconnectEvent event) {
        String simpSessionId = getSessionIdFromEvent(event);
        log.info("Disconnect event, simpSessionId {}", simpSessionId);
        log.debug("Disconnect event {}", event);
        webSocketSessionAssistant.removeBySimpSessionId(simpSessionId);
    }

    @EventListener(SessionUnsubscribeEvent.class)
    public void handleWsUnSubscribeEvent(SessionUnsubscribeEvent event) {
        String simpSessionId = getSessionIdFromEvent(event);
        log.info("Unsubscribe event, simpSessionId {}", simpSessionId);
        log.debug("Unsubscribe event {}", event);
        webSocketSessionAssistant.removeBySimpSessionId(simpSessionId);
    }

    private ContractPlayerIdWithJSessionId getContract(AbstractSubProtocolEvent event) {
        String jSessionId = getJSessionId(event);
        return contractRepository.mustFindById(jSessionId);
    }
}
