package com.lloyds.transaction.exception;

import com.lloyds.transaction.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponseEntity(String error, String message, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(error, message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Invalid Argument", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        log.error("DataAccessException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Database Access Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex) {
        log.error("NullPointerException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Null Pointer Exception", "A required object was null: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Runtime Exception", "An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);
        return buildResponseEntity("Unexpected Error", "An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<String> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SchedulerException.class)
    public ResponseEntity<ErrorResponse> handleSchedulerException(SchedulerException ex) {
        log.error("SchedulerException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Scheduler Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TransferSchedulingException.class)
    public ResponseEntity<ErrorResponse> handleTransferSchedulingException(TransferSchedulingException ex) {
        log.error("TransferSchedulingException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Transfer Scheduling Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JobExecutionException.class)
    public ResponseEntity<ErrorResponse> handleJobExecutionException(JobExecutionException ex) {
        log.error("JobExecutionException: {}", ex.getMessage(), ex);
        return buildResponseEntity("Job Execution Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }




}
