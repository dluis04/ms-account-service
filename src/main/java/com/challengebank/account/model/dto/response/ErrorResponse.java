package com.challengebank.account.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

    public LocalDateTime timestamp;
    public int status;
    public String error;
    public String message;
    public String path;
    public List<FieldError> errors;
}
