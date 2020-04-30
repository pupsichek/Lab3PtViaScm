package com.example.pt.lab3.pojo.response.content;

import com.example.pt.lab3.pojo.type.ResponseType;
import com.example.pt.lab3.pojo.response.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static com.example.pt.lab3.pojo.type.ResponseType.CONTENT;

@Getter
@RequiredArgsConstructor
class ContentResponse implements Response {
    private final ResponseType type = CONTENT;
    private final UUID playerId;
}
