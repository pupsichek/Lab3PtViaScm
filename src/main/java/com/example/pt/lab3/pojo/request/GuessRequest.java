package com.example.pt.lab3.pojo.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
/**
 * Example:
 * {
 *     "number": 111
 * }
 * @see com.example.pt.lab3.controller.GameController#guessRequest(UUID, GuessRequest)
 */
public class GuessRequest {
    @NotNull
    private Integer number;

    private UUID gameId;
}
