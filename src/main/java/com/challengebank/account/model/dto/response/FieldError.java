package com.challengebank.account.model.dto.response;

public class FieldError {

    public String field;
    public String message;

    public FieldError() {
    }

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
