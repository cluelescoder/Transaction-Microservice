package com.lloyds.transaction.exception;

import com.lloyds.transaction.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

 class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
     void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
     void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid Argument");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
    }

    @Test
     void testHandleDataAccessException() {
        // Using the correct exception type: org.springframework.dao.DataAccessException
        org.springframework.dao.DataAccessException exception = new org.springframework.dao.DataAccessException("Database error") {};

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataAccessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Database Access Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Database error");
    }


    @Test
     void testHandleNullPointerException() {
        NullPointerException exception = new NullPointerException("Null value");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNullPointerException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Null Pointer Exception");
        assertThat(response.getBody().getMessage()).isEqualTo("A required object was null: Null value");
    }

    @Test
     void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Runtime error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Runtime Exception");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred: Runtime error");
    }

    @Test
     void testHandleException() {
        Exception exception = new Exception("General error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Unexpected Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred: General error");
    }

    @Test
     void testHandleAccountNotFoundException() {
        AccountNotFoundException exception = new AccountNotFoundException("Account not found");

        ResponseEntity<String> response = globalExceptionHandler.handleAccountNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Account not found");
    }

    @Test
     void testHandleInsufficientFundsException() {
        InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

        ResponseEntity<String> response = globalExceptionHandler.handleInsufficientFunds(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Insufficient funds");
    }

    @Test
     void testHandleSchedulerException() {
        SchedulerException exception = new SchedulerException("Scheduler error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleSchedulerException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Scheduler Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Scheduler error");
    }

    @Test
     void testHandleTransferSchedulingException() {
        TransferSchedulingException exception = new TransferSchedulingException("Transfer scheduling error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTransferSchedulingException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Transfer Scheduling Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Transfer scheduling error");
    }

    @Test
     void testHandleJobExecutionException() {
        JobExecutionException exception = new JobExecutionException("Job execution error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJobExecutionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Job Execution Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Job execution error");
    }


}