package com.example.pt.lab3.pojo.response.error;

import com.example.pt.lab3.pojo.type.ResponseType;
import com.example.pt.lab3.pojo.response.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.example.pt.lab3.pojo.type.ResponseType.INTERNAL_ERROR;

@RequiredArgsConstructor
@Getter
public class InternalErrorResponse implements Response {
    private final ResponseType type = INTERNAL_ERROR;
    private final String message;
}
