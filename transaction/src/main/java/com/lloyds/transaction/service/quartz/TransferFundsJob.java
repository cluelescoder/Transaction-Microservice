package com.lloyds.transaction.service.quartz;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.exception.JobExecutionException;
import com.lloyds.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransferFundsJob extends QuartzJobBean {

    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        TransactionService transactionService;

        try {
            transactionService = (TransactionService) context.getScheduler().getContext().get("transactionService");
        } catch (SchedulerException e) {
            throw new com.lloyds.transaction.exception.SchedulerException("Failed to schedule transfer: " + e.getMessage());
        }

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        TransferRequestDTO transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId((Long) jobDataMap.get("senderAccountId"));
        transferRequest.setReceiverAccountId((Long) jobDataMap.get("receiverAccountId"));
        transferRequest.setReceiverName((String) jobDataMap.get("receiverName"));
        transferRequest.setNote((String) jobDataMap.get("note"));
        transferRequest.setTransactionType((String) jobDataMap.get("transactionType"));
        transferRequest.setAmount((Double) jobDataMap.get("amount"));

        try {
            log.info("Executing transfer job for sender account: {}", transferRequest.getSenderAccountId());
            transactionService.transferFunds(transferRequest, (Long) jobDataMap.get("customerID"));
        } catch (Exception e) {
            log.error("Unexpected error occurred while executing transfer job: {}", e.getMessage(), e);
            throw new JobExecutionException("An unexpected error occurred while executing transfer job");
        }
    }
}