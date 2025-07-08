package com.planitsquare.assignment_jaehyuk.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;

@Getter
@Builder
public class ErrorResponse  {
    private String errorCode;
    private String errorMessage;

    public ErrorResponse(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ErrorResponse(String errorCode, BindingResult bindingResult) {
        this.errorCode = errorCode;
        this.errorMessage = bindingResult.getFieldError().getDefaultMessage();
    }

    private String createErrorMessage(BindingResult bindingResult) {
        StringBuilder builder = new StringBuilder();

        bindingResult.getFieldErrors().forEach(fieldError -> {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append("[").append(fieldError.getField()).append("] ");
            builder.append(fieldError.getDefaultMessage());
        });

        return builder.toString();
    }
}
