package com.lloyds.transaction.controller;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.exception.SchedulerException;
import com.lloyds.transaction.exception.TransferSchedulingException;
import com.lloyds.transaction.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class SchedulerControllerTest {

    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private SchedulerController schedulerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void scheduleTransfer_Success() {
        // Arrange
        String authHeader = "Bearer token";
        TransferRequestDTO transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(100002L);
        SchedulerResponseDTO expectedResponse = new SchedulerResponseDTO();
        when(schedulerService.scheduleTransfer(eq(authHeader), any(TransferRequestDTO.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<SchedulerResponseDTO> responseEntity = schedulerController.scheduleTransfer(authHeader, transferRequest);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    }


}
