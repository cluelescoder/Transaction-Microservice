package com.lloyds.transaction.service;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.exception.AccountNotFoundException;
import com.lloyds.transaction.exception.SchedulerException;
import com.lloyds.transaction.exception.TransferSchedulingException;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.security.JwtUtil;
import com.lloyds.transaction.service.quartz.TransferFundsJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {

    private final Scheduler scheduler;
    private final AccountInterface accountFeignClient;
    private final JwtUtil jwtUtil;

    @Value("${service.api.key}")
    private String apiKey;

    public SchedulerServiceImpl(AccountInterface accountFeignClient, Scheduler scheduler, JwtUtil jwtUtil) {
        this.scheduler = scheduler;
        this.accountFeignClient = accountFeignClient;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public SchedulerResponseDTO scheduleTransfer(String authHeader, TransferRequestDTO transferRequest) throws SchedulerException {
        try {
            String jwtToken = authHeader.replace("Bearer ", "");
            String customerIdFromToken = jwtUtil.extractId(jwtToken);
            Long customerIdFromTokenAsLong = Long.parseLong(customerIdFromToken);

            log.info("Fetching accounts for customer ID: {}", customerIdFromTokenAsLong);
            ResponseEntity<List<AccountDTO>> response = accountFeignClient.getAccountsByCustomerId("Jwttoken", apiKey, customerIdFromTokenAsLong);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Failed to fetch sender's accounts, status: {}", response.getStatusCode());
                throw new AccountNotFoundException("Failed to fetch accounts, status: " + response.getStatusCode());
            }

            response.getBody()
                    .stream()
                    .filter(account -> account.getId().equals(transferRequest.getSenderAccountId()))
                    .findFirst()
                    .ifPresentOrElse(
                            account -> log.info("Sender account found: {}", account.getId()),
                            () -> {
                                log.error("Sender account not found: {}", transferRequest.getSenderAccountId());
                                throw new AccountNotFoundException("Sender account not found");
                            }
                    );


            JobDetail jobDetail = buildJobDetail(transferRequest, customerIdFromTokenAsLong);
            Trigger trigger;
            if (transferRequest.getRecurrencePattern() != null && !transferRequest.getRecurrencePattern().isEmpty()) {
                // Recurring transaction
                trigger = buildRecurringTrigger(jobDetail, transferRequest);
            } else {
                // One-time transaction
                ZonedDateTime dateTime = ZonedDateTime.of(transferRequest.getScheduledTime(), transferRequest.getTimeZone());
                trigger = buildJobTrigger(jobDetail, dateTime);
            }

            log.info("Scheduling transfer job for sender account: {}", transferRequest.getSenderAccountId());
            scheduler.scheduleJob(jobDetail, trigger);

            SchedulerResponseDTO schedulerResponse = new SchedulerResponseDTO();
            schedulerResponse.setStatus("SUCCESS");
            schedulerResponse.setMessage("Transfer scheduled successfully!");
            schedulerResponse.setJobId(jobDetail.getKey().getName());
            schedulerResponse.setTriggerId(trigger.getKey().getName());

            return schedulerResponse;
        } catch (org.quartz.SchedulerException e) {
            log.error("SchedulerException occurred while scheduling transfer: {}", e.getMessage(), e);
            throw new SchedulerException("Failed to schedule transfer: " + e.getMessage());
        } catch (AccountNotFoundException e) {
            log.error("AccountNotFoundException occurred: {}", e.getMessage(), e);
            throw new AccountNotFoundException(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred while scheduling transfer: {}", e.getMessage(), e);
            throw new TransferSchedulingException("An unexpected error occurred while scheduling transfer");
        }
    }

    public JobDetail buildJobDetail(TransferRequestDTO transferRequest, Long customerID) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("senderAccountId", transferRequest.getSenderAccountId());
        jobDataMap.put("receiverAccountId", transferRequest.getReceiverAccountId());
        jobDataMap.put("receiverName", transferRequest.getReceiverName());
        jobDataMap.put("note", transferRequest.getNote());
        jobDataMap.put("transactionType", transferRequest.getTransactionType());
        jobDataMap.put("amount", transferRequest.getAmount());
        jobDataMap.put("customerID", customerID);

        return JobBuilder.newJob(TransferFundsJob.class)
                .withIdentity(UUID.randomUUID().toString(), "transfer-jobs")
                .withDescription("Fund Transfer Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    public  Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "transfer-triggers")
                .withDescription("Fund Transfer Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }

    public Trigger buildRecurringTrigger(JobDetail jobDetail, TransferRequestDTO transferRequest) {
        String recurrencePattern = transferRequest.getRecurrencePattern();
        log.info("1st log Trigger request timezone for sender account: {}", transferRequest.getTimeZone());
        ZonedDateTime triggerStartAt = ZonedDateTime.of(transferRequest.getStartDate(), transferRequest.getTimeZone());
        ZonedDateTime triggerEndAt = ZonedDateTime.of(transferRequest.getEndDate(), transferRequest.getTimeZone());
        log.info("2nd log Trigger request timezone for sender account: {}", transferRequest.getTimeZone());

        CronScheduleBuilder cronSchedule = switch (recurrencePattern.toUpperCase()) {
            case "DAILY" -> CronScheduleBuilder.dailyAtHourAndMinute(
                    triggerStartAt.getHour(), triggerStartAt.getMinute());
            case "WEEKLY" -> {
                int dayOfWeek = triggerStartAt.getDayOfWeek().getValue() + 1; // Quartz uses 1-7 for Sunday-Saturday
                yield CronScheduleBuilder.weeklyOnDayAndHourAndMinute(
                        dayOfWeek, triggerStartAt.getHour(), triggerStartAt.getMinute());
            }
            case "MONTHLY" -> CronScheduleBuilder.monthlyOnDayAndHourAndMinute(
                    triggerStartAt.getDayOfMonth(), triggerStartAt.getHour(), triggerStartAt.getMinute());
            default -> throw new IllegalArgumentException("Invalid recurrence pattern: " + recurrencePattern);
        };

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "recurring-transfer-triggers")
                .withDescription("Recurring Fund Transfer Trigger")
                .startAt(Date.from(triggerStartAt.toInstant()))
                .endAt(Date.from(triggerEndAt.toInstant()))
                .withSchedule(cronSchedule)
                .build();
    }
}
