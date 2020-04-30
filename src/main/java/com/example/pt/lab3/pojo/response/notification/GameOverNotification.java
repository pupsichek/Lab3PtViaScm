package com.example.pt.lab3.pojo.response.notification;

import com.example.pt.lab3.pojo.type.NotificationType;

import static com.example.pt.lab3.pojo.type.NotificationType.INFO;

public class GameOverNotification extends NotificationResponse {
    private final static NotificationType notificationType = INFO;
    private final static String format = "\"%s\" game over";

    public GameOverNotification(String gameName) {
        super(notificationType, String.format(format, gameName));
    }
}
