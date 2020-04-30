package com.example.pt.lab3.pojo.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
/**
 * Example:
 * {
 *     "name": "Test Game",
 *     "number": 111
 * }
 * @see com.example.pt.lab3.controller.GameController#createGame(GameCreateRequest)
 */
public class GameCreateRequest {
    @NotNull
    private String name;
    @NotNull
    private Integer number;
}
