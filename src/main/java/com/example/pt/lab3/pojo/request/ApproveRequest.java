package com.example.pt.lab3.pojo.request;

import com.example.pt.lab3.pojo.action.PartyAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
/**
 * Example:
 * {
 *     "action": "less"
 * }
 * @see com.example.pt.lab3.controller.GameController#approveRequest(UUID, ApproveRequest)
 */
public class ApproveRequest {

    private final PartyAction action;
    @Setter
    private UUID gameId;

    @JsonCreator
    public ApproveRequest(@JsonProperty("action") String actionKey) {
        this.action = PartyAction.approveOf(actionKey);
    }
}
