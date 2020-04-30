package com.example.pt.lab3.pojo.response.content;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.pojo.response.projection.GameProjectionForView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class GamesResponseForPlayer extends ContentResponse {
    private final Collection<GameProjectionForView> createdGames;
    private final Collection<GameProjectionForView> availableGames;
    private final Collection<GameProjectionForView> currentGames;

    public GamesResponseForPlayer(PlayerInstance player, Collection<GameInstance> createdGames,
                                  Collection<GameInstance> availableGames, Collection<GameInstance> currentGames) {
        super(player.getId());
        this.createdGames = projectToView(createdGames);
        this.availableGames = projectToView(availableGames);
        this.currentGames = projectToView(currentGames);
    }

    private Collection<GameProjectionForView> projectToView(Collection<GameInstance> pageGames) {
        return pageGames.stream().map(GameProjectionForView::new).collect(toList());
    }
}
