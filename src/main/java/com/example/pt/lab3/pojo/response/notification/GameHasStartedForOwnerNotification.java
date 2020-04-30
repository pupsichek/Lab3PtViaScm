package com.example.pt.lab3.pojo.response.notification;

import com.example.pt.lab3.pojo.type.NotificationType;

import static com.example.pt.lab3.pojo.type.NotificationType.INFO;

public class GameHasStartedForOwnerNotification extends NotificationResponse {
    private final static NotificationType notificationType = INFO;
    private final static String format = "Your game \"%s\" is started...";

    public GameHasStartedForOwnerNotification(String gameName) {
        super(notificationType, String.format(format, gameName));
    }
}
