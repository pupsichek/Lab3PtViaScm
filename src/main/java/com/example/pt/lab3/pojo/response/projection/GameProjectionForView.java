package com.example.pt.lab3.pojo.response.projection;

import com.example.pt.lab3.domain.GameInstance;
import com.example.pt.lab3.domain.enumeration.GameState;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import static com.example.pt.lab3.util.DateUtils.toEpochMilli;

@Getter
@Setter
public class GameProjectionForView {
    private UUID id;
    private String name;
    private Long createdDate;
    private GameState state;

    public GameProjectionForView(GameInstance gameInstance) {
        this.id = gameInstance.getId();
        this.name = gameInstance.getName();
        this.createdDate = toEpochMilli(gameInstance.getCreatedDate());
        this.state = gameInstance.getState();
    }
}
