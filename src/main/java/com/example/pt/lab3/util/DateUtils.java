package com.example.pt.lab3.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {
    public static Long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
