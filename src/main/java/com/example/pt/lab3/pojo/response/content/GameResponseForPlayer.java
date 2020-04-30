package com.example.pt.lab3.pojo.response.content;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.pojo.action.PartyAction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.UUID;

import static com.example.pt.lab3.util.DateUtils.toEpochMilli;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class GameResponseForPlayer extends ContentResponse {
    private final UUID gameId;
    private final boolean isComplete;
    private final Long gameCreatedAt;
    private final Long gameUpdatedAt;
    private final Collection<PartyAction> availableActions;
    private String content;

    public GameResponseForPlayer(PlayerInstance player, GameInstance game, boolean isComplete, Collection<PartyAction> availableActions) {
        super(player.getId());
        this.gameId = game.getId();
        this.gameCreatedAt = toEpochMilli(game.getCreatedDate());
        this.gameUpdatedAt = toEpochMilli(game.getUpdatedDate());
        this.isComplete = isComplete;
        this.availableActions = availableActions;
    }

    public GameResponseForPlayer(PlayerInstance player, GameInstance game, boolean isComplete, Collection<PartyAction> availableActions, String content) {
        this(player, game, isComplete, availableActions);
        this.content = content;
    }
}
