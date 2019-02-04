package com.neetkee.example.controller;

import com.neetkee.example.error.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeoutException;

@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(ErrorDetails.class)
//    public final ResponseEntity<ErrorMessage> handleApiException(ErrorDetails ex, WebRequest request) {
//        HttpStatus status = ex.getStatus();
//        ErrorMessage errorDetails = new ErrorMessage().timestamp(ex.getTimestamp())
//                .status(status.value()).error(status.getReasonPhrase()).message(ex.getMessage());
//        if (request instanceof ServletWebRequest) {
//            ServletWebRequest servletWebRequest = (ServletWebRequest) request;
//            HttpServletRequest servletRequest =
//                    servletWebRequest.getNativeRequest(HttpServletRequest.class);
//            if (servletRequest != null) {
//                errorDetails = errorDetails.path(servletRequest.getRequestURI());
//            }
//        }
//        return new ResponseEntity<>(errorDetails, status);
//    }

    @ExceptionHandler(TimeoutException.class)
    public final ResponseEntity<ErrorDetails> handleTimeoutException(TimeoutException e, WebRequest request) {
        //ErrorMessage errorDetails = new ErrorMessage(e);
        //
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setErrorCode("SYSTEM-HTTP-502");
        errorDetails.setErrorDateTime(ZonedDateTime.now());
        errorDetails.setErrorMessage("Request timed out");
        errorDetails.setOriginalExceptionMessage(e.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_GATEWAY);
    }


}
