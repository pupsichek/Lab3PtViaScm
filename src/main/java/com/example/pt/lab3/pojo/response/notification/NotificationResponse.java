package com.example.pt.lab3.pojo.response.notification;

import com.example.pt.lab3.pojo.type.NotificationType;
import com.example.pt.lab3.pojo.type.ResponseType;
import com.example.pt.lab3.pojo.response.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.example.pt.lab3.pojo.type.ResponseType.NOTIFICATION;

@RequiredArgsConstructor
@Getter
public class NotificationResponse implements Response {
    private final ResponseType type = NOTIFICATION;
    private final NotificationType notificationType;
    private final String message;
}
