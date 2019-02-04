package com.neetkee.example.error;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ErrorDetails {
    private String errorCode;
    private String errorMessage;
    private String originalExceptionMessage;
    private ZonedDateTime errorDateTime;
}
