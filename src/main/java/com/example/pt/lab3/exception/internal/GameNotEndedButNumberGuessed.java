package com.example.pt.lab3.exception.internal;

import org.springframework.dao.OptimisticLockingFailureException;

public class GameNotEndedButNumberGuessed extends OptimisticLockingFailureException {
    public GameNotEndedButNumberGuessed() {
        super("The game is not over yet, but the number is guessed");
    }
}
