package com.example.pt.lab3.pojo.action;

import com.example.pt.lab3.pojo.response.content.GameResponseForPlayer;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

import static com.example.pt.lab3.exception.ErrorFactory.currentActionIsNotExist;

/**
 * Enum need to set current action in GameResponse
 * @see GameResponseForPlayer
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum PartyAction {
    MORE_NUMBER("more", "Check that number more alleged"), // for owner
    LESS_NUMBER("less", "Check that number less alleged"), // for owner
    GUESS_NUMBER("guess", "Set a number to guess"); // for player

    private static Map<String, PartyAction> approveActions = ImmutableMap.of(
            MORE_NUMBER.key, MORE_NUMBER,
            LESS_NUMBER.key, LESS_NUMBER
    );

    public static PartyAction approveOf(String key) {
        return Optional.ofNullable(approveActions.get(key)).orElseThrow(() -> currentActionIsNotExist(key, approveActions.keySet()));
    }

    private final String key;
    private final String value;
}
