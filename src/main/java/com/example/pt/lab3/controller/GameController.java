package com.example.pt.lab3.controller;

import com.example.pt.lab3.api.GameApi;
import com.example.pt.lab3.pojo.request.ApproveRequest;
import com.example.pt.lab3.pojo.request.GameCreateRequest;
import com.example.pt.lab3.pojo.request.GuessRequest;
import com.example.pt.lab3.repository.PlayerRepository;
import com.example.pt.lab3.service.SubscriberNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

import static com.example.pt.lab3.service.enrich.SessionEnrichment.HEADER_USER_NAME;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GameController {

    private final GameApi gameApi;
    private final PlayerRepository playerRepository;
    private final SubscriberNotificationService notificationService;

    @PostMapping("/create")
    public ResponseEntity createGame(@RequestBody @Valid GameCreateRequest gameCreateRequest) {
        gameApi.createGame(gameCreateRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/{gameId}/play")
    public ResponseEntity playGame(@PathVariable UUID gameId) {
        gameApi.playGame(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/guess")
    public ResponseEntity guessRequest(@PathVariable UUID gameId,
                                       @RequestBody @Valid GuessRequest guessRequest) {
        gameApi.guess(gameId, guessRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/approve")
    public ResponseEntity approveRequest(@PathVariable UUID gameId,
                                       @RequestBody @Valid ApproveRequest approveRequest) {
        gameApi.approve(gameId, approveRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-user")
    public ResponseEntity changeUser(@RequestHeader(name = HEADER_USER_NAME) UUID playerId) {
        if (playerRepository.existsById(playerId)) {
            notificationService.notifySpecifiedPlayerWithGamesResponseAsync(playerId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.unprocessableEntity().build();
    }
}
