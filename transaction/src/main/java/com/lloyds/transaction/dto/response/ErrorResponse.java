package com.lloyds.transaction.dto.response;

import lombok.Getter;


public class ErrorResponse {
    @Getter
    private String error;
    @Getter
    private String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

}