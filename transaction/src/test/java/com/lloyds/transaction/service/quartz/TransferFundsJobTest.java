package com.lloyds.transaction.service.quartz;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.exception.JobExecutionException;
import com.lloyds.transaction.exception.SchedulerException;
import com.lloyds.transaction.service.TransactionService;
import com.lloyds.transaction.service.quartz.TransferFundsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TransferFundsJobTest {

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private Scheduler scheduler;

    @Mock
    private SchedulerContext schedulerContext;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransferFundsJob transferFundsJob;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
        when(scheduler.getContext()).thenReturn(schedulerContext);
        when(schedulerContext.get("transactionService")).thenReturn(transactionService);
    }

    @Test
    void executeInternal_Success() throws Exception {
        // Arrange
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("senderAccountId", 12345L);
        jobDataMap.put("receiverAccountId", 67890L);
        jobDataMap.put("receiverName", "John Doe");
        jobDataMap.put("note", "Payment for services");
        jobDataMap.put("transactionType", "TRANSFER");
        jobDataMap.put("amount", 100.0);
        jobDataMap.put("customerID", 1L);

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

        // Act
        transferFundsJob.executeInternal(jobExecutionContext);

        // Assert
        verify(transactionService, times(1)).transferFunds(any(TransferRequestDTO.class), eq(1L));
    }

    @Test
    void executeInternal_SchedulerException() throws Exception {
        // Arrange
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
        when(scheduler.getContext()).thenThrow(new org.quartz.SchedulerException("Scheduler context error"));

        // Act & Assert
        SchedulerException exception = assertThrows(SchedulerException.class, () ->
                transferFundsJob.executeInternal(jobExecutionContext));
        assertThat(exception.getMessage(), is("Failed to schedule transfer: Scheduler context error"));
    }


    @Test
    void executeInternal_UnexpectedException() throws Exception {
        // Arrange
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("senderAccountId", 12345L);
        jobDataMap.put("receiverAccountId", 67890L);
        jobDataMap.put("receiverName", "John Doe");
        jobDataMap.put("note", "Payment for services");
        jobDataMap.put("transactionType", "TRANSFER");
        jobDataMap.put("amount", 100.0);
        jobDataMap.put("customerID", 1L);

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        doThrow(new RuntimeException("Unexpected error")).when(transactionService).transferFunds(any(TransferRequestDTO.class), eq(1L));

        // Act & Assert
        JobExecutionException exception = assertThrows(JobExecutionException.class, () ->
                transferFundsJob.executeInternal(jobExecutionContext));
        assertThat(exception.getMessage(), is("An unexpected error occurred while executing transfer job"));
    }
}
