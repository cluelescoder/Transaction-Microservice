package com.lloyds.transaction.service.impl;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.exception.AccountNotFoundException;
import com.lloyds.transaction.exception.SchedulerException;
import com.lloyds.transaction.exception.TransferSchedulingException;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.security.JwtUtil;
import com.lloyds.transaction.service.SchedulerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private AccountInterface accountFeignClient;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    private TransferRequestDTO transferRequest;
    private List<AccountDTO> accountList;
    private JobDetail jobDetail;
    private  JobKey jobKey;


    @BeforeEach
    void setUp() {
        transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(5L);
        transferRequest.setReceiverAccountId(2L);
        transferRequest.setReceiverName("John Doe");
        transferRequest.setNote("Payment for services");
        transferRequest.setTransactionType("TRANSFER");
        transferRequest.setAmount(2.00);
        transferRequest.setScheduledTime(LocalDateTime.of(2025, 2, 17, 16, 28));
        transferRequest.setTimeZone(ZoneId.of("Asia/Kolkata"));


        AccountDTO accountDTO = new AccountDTO();
        accountDTO = new AccountDTO();
        accountDTO.setId(5L);
        accountDTO.setAccountNumber("ACC12345");
        accountDTO.setBalance(1000.00);
        accountDTO.setAccountType("Savings Account");
        accountDTO.setCustomerId(67890L);
        accountList = Collections.singletonList(accountDTO);

        jobDetail = mock(JobDetail.class);
       jobKey = new JobKey("testJob", "testGroup");

        ReflectionTestUtils.setField(schedulerService, "apiKey", "test-api-key");
    }

    @Test
    void testScheduleTransfer_Success() throws Exception {
        // Arrange
        String authHeader = "Bearer test-token";
        transferRequest.setRecurrencePattern("");
        when(jwtUtil.extractId(anyString())).thenReturn("1");
        when(accountFeignClient.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(ResponseEntity.ok(accountList));
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(null);

        // Act
        SchedulerResponseDTO response = schedulerService.scheduleTransfer(authHeader, transferRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Transfer scheduled successfully!");
        assertThat(response.getJobId()).isNotNull(); // Ensure jobId is set
        assertThat(response.getTriggerId()).isNotNull(); // Ensure triggerId is set

        assertThat(response.toString()).contains("SUCCESS", "Transfer scheduled successfully!");

        SchedulerResponseDTO response2 = new SchedulerResponseDTO("SUCCESS", "Transfer scheduled successfully!", response.getJobId(), response.getTriggerId());


        verify(jwtUtil, times(1)).extractId(anyString());
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(anyString(), anyString(), anyLong());
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void testScheduleReccuringTransfer_Success() throws Exception {
        // Arrange
        String authHeader = "Bearer test-token";
        transferRequest.setRecurrencePattern("MONTHLY");
        transferRequest.setStartDate(ZonedDateTime.now().toLocalDateTime());
        transferRequest.setEndDate(ZonedDateTime.now().plusDays(10).toLocalDateTime());
        when(jwtUtil.extractId(anyString())).thenReturn("1");
        when(accountFeignClient.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(ResponseEntity.ok(accountList));
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(null);

        // Act
        SchedulerResponseDTO response = schedulerService.scheduleTransfer(authHeader, transferRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Transfer scheduled successfully!");
        assertThat(response.getJobId()).isNotNull(); // Ensure jobId is set
        assertThat(response.getTriggerId()).isNotNull(); // Ensure triggerId is set

        assertThat(response.toString()).contains("SUCCESS", "Transfer scheduled successfully!");

        SchedulerResponseDTO response2 = new SchedulerResponseDTO("SUCCESS", "Transfer scheduled successfully!", response.getJobId(), response.getTriggerId());
        verify(jwtUtil, times(1)).extractId(anyString());
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(anyString(), anyString(), anyLong());
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void testScheduleTransfer_AccountNotFoundException() {
        // Arrange
        String authHeader = "Bearer token";
        when(jwtUtil.extractId(any(String.class))).thenReturn("1");
        when(accountFeignClient.getAccountsByCustomerId(any(String.class), any(String.class), any(Long.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        // Act & Assert
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () ->
                schedulerService.scheduleTransfer(authHeader, transferRequest));
        assertThat(exception.getMessage()).isEqualTo("Sender account not found");

        verify(jwtUtil, times(1)).extractId(any(String.class));
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(any(String.class), any(String.class), any(Long.class));
        verifyNoInteractions(scheduler);
    }

    @Test
    void testScheduleTransfer_AccountNotFound() {
        // Arrange
        String authHeader = "Bearer token";
        when(jwtUtil.extractId(any(String.class))).thenReturn("1");
        when(accountFeignClient.getAccountsByCustomerId(any(String.class), any(String.class), any(Long.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND));

        // Act & Assert
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () ->
                schedulerService.scheduleTransfer(authHeader, transferRequest));
        assertThat(exception.getMessage()).isEqualTo("Failed to fetch accounts, status: 404 NOT_FOUND");

        verify(jwtUtil, times(1)).extractId(any(String.class));
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(any(String.class), any(String.class), any(Long.class));
        verifyNoInteractions(scheduler);
    }

    @Test
    void testScheduleTransfer_SchedulerException() throws Exception {
        // Arrange
        String authHeader = "Bearer token";
        when(jwtUtil.extractId(anyString())).thenReturn("1");
        when(accountFeignClient.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(accountList, HttpStatus.OK));
        doThrow(new org.quartz.SchedulerException("Scheduler error")).when(scheduler).scheduleJob(any(), any());

        // Act & Assert
        SchedulerException exception = assertThrows(SchedulerException.class, () ->
                schedulerService.scheduleTransfer(authHeader, transferRequest));
        assertThat(exception.getMessage()).isEqualTo("Failed to schedule transfer: Scheduler error");

        verify(jwtUtil, times(1)).extractId(anyString());
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(anyString(), anyString(), anyLong());
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of(new RuntimeException("Unexpected error"), TransferSchedulingException.class, "An unexpected error occurred while scheduling transfer"),
                Arguments.of(new ResponseEntity<List<AccountDTO>>((List<AccountDTO>) null, HttpStatus.OK), AccountNotFoundException.class, "Failed to fetch accounts, status: 200 OK")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testScheduleTransfer_ExceptionScenarios(Object response, Class<? extends Exception> expectedException, String expectedMessage) {
        // Arrange
        String authHeader = "Bearer token";
        when(jwtUtil.extractId(anyString())).thenReturn("1");

        if (response instanceof Exception) {
            when(accountFeignClient.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                    .thenThrow((Exception) response);
        } else if (response instanceof ResponseEntity) {
            @SuppressWarnings("unchecked")
            ResponseEntity<List<AccountDTO>> castedResponse = (ResponseEntity<List<AccountDTO>>) response;
            when(accountFeignClient.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                    .thenReturn(castedResponse);
        }

        // Act & Assert
        Exception exception = assertThrows(expectedException, () ->
                schedulerService.scheduleTransfer(authHeader, transferRequest));

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);

        verify(jwtUtil, times(1)).extractId(anyString());
        verify(accountFeignClient, times(1)).getAccountsByCustomerId(anyString(), anyString(), anyLong());
        verifyNoInteractions(scheduler);
    }



    @ParameterizedTest
    @ValueSource(strings = {"DAILY", "WEEKLY", "MONTHLY"})
    void testBuildRecurringTrigger(String recurrencePattern) {
        // Arrange
        transferRequest.setRecurrencePattern(recurrencePattern);
        transferRequest.setStartDate(ZonedDateTime.now().toLocalDateTime());
        transferRequest.setEndDate(ZonedDateTime.now().plusDays(10).toLocalDateTime());

        when(jobDetail.getKey()).thenReturn(jobKey);

        // Act
        Trigger trigger = schedulerService.buildRecurringTrigger(jobDetail, transferRequest);

        // Assert
        assertThat(trigger).isNotNull();
        assertThat(trigger.getDescription()).isEqualTo("Recurring Fund Transfer Trigger");
        assertThat(trigger.getJobKey()).isEqualTo(jobKey);
    }

    @Test
    void testBuildRecurringTrigger_InvalidPattern() {
        // Arrange
        transferRequest.setRecurrencePattern("INVALID");
        transferRequest.setStartDate(ZonedDateTime.now().toLocalDateTime());
        transferRequest.setEndDate(ZonedDateTime.now().plusDays(10).toLocalDateTime());
       // when(jobDetail.getKey()).thenReturn(jobKey);
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schedulerService.buildRecurringTrigger(null, transferRequest));
        assertThat(exception.getMessage()).isEqualTo("Invalid recurrence pattern: INVALID");
    }

}