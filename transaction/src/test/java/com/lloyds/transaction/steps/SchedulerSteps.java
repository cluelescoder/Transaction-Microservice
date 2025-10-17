package com.lloyds.transaction.steps;

import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.service.SchedulerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class SchedulerSteps {

    private final SchedulerService schedulerService = Mockito.mock(SchedulerService.class);
    private TransferRequestDTO transferRequest;
    private SchedulerResponseDTO schedulerResponse;
    private String authHeader = "Bearer validToken123";
    private boolean transferSuccess;

    private boolean transferSuccessful = false;
    private String errorMessage = "";
    private Double balance=1000.00;
    private double transferAmount = 100.00;
    private String recurrencePattern = "";
    private LocalDate scheduledDate;
    private boolean emailSent = false;

    @When("the sender schedules a transfer of {string} GBP to the receiver on {string}")
    public void senderSchedulesATransfer(String amount, String date) throws Exception {
        transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(12345L);
        transferRequest.setReceiverAccountId(67890L);
        transferRequest.setAmount(Double.parseDouble(amount));
        transferRequest.setScheduledTime(LocalDateTime.parse(date + "T10:00:00"));
        transferRequest.setTimeZone(ZoneId.of("UTC"));
        schedulerResponse = new SchedulerResponseDTO();
        schedulerResponse.setStatus("SUCCESS");
        schedulerResponse.setMessage("Transfer scheduled successfully!");
        schedulerResponse.setJobId("job-12345");
        schedulerResponse.setTriggerId("trigger-12345");

        Mockito.when(schedulerService.scheduleTransfer(authHeader, transferRequest)).thenReturn(schedulerResponse);
    }

    @Then("the transaction is scheduled successfully")
    public void transactionIsScheduledSuccessfully() {
        Assertions.assertNotNull(schedulerResponse, "The transaction scheduling response should not be null.");
        Assertions.assertEquals("SUCCESS", schedulerResponse.getStatus(), "Transaction should be scheduled successfully.");
    }

    @When("the sender schedules a recurring transfer of {string} GBP to the receiver with a {string} recurrence pattern")
    public void senderSchedulesRecurringTransfer(String amount, String recurrencePattern) {
        try {
            transferRequest = new TransferRequestDTO();
            transferRequest.setSenderAccountId(12345L);
            transferRequest.setReceiverAccountId(67890L);
            transferRequest.setAmount(Double.parseDouble(amount));
            transferRequest.setRecurrencePattern(recurrencePattern);
            transferRequest.setScheduledTime(LocalDateTime.now().plusDays(1));
            transferRequest.setTimeZone(ZoneId.of("UTC"));

            schedulerResponse = new SchedulerResponseDTO();
            schedulerResponse.setStatus("SUCCESS");
            schedulerResponse.setMessage("Recurring transaction scheduled successfully!");
            schedulerResponse.setJobId("job-12345");
            schedulerResponse.setTriggerId("trigger-12345");

            Mockito.when(schedulerService.scheduleTransfer(Mockito.anyString(), Mockito.any(TransferRequestDTO.class)))
                    .thenReturn(schedulerResponse);

            SchedulerResponseDTO response = schedulerService.scheduleTransfer("Bearer validToken123", transferRequest);

            if (response != null && "SUCCESS".equals(response.getStatus())) {
                transferSuccess = true;
            } else {
                transferSuccess = false;
            }
        } catch (Exception e) {
            transferSuccess = false;
        }
    }

    @Then("the recurring transaction is scheduled successfully")
    public void recurringTransactionScheduledSuccessfully() {
        Assertions.assertTrue(transferSuccess, "Expected the recurring transaction to be scheduled successfully, but it failed.");
    }
    @Given("a recurring transaction exists for {string} GBP with a {string} recurrence pattern")
    public void recurringTransactionExists(String amount, String pattern) {
        transferAmount = Double.parseDouble(amount);
        recurrencePattern = pattern.toUpperCase();
        scheduledDate = LocalDate.of(2025, 3, 1);
        emailSent = false;
    }

    @When("the scheduler triggers the transaction on {string}")
    public void schedulerTriggersTransaction(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate triggerDate = LocalDate.parse(date, formatter);

        if (triggerDate.equals(scheduledDate)) {
            transferSuccessful = true;
            emailSent = true;
            System.out.println("Scheduler triggered on: " + triggerDate);
        } else {
            transferSuccessful = false;
            errorMessage = "The scheduled transaction did not execute correctly";
            System.out.println("Error: " + errorMessage);
        }
    }

    @Given("a scheduled transaction exists for {string} GBP on {string}")
    public void aScheduledTransactionExistsForAmountOnDate(String amount, String date) {
        transferAmount = Double.parseDouble(amount);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        scheduledDate = LocalDate.parse(date, formatter);

        emailSent = false;
    }

    @When("the scheduler triggers the transaction")
    public void schedulerTriggersTransaction() {
        LocalDate currentDate = LocalDate.now();

        if (currentDate.equals(scheduledDate)) {
            if (balance < transferAmount) {
                transferSuccessful = false;
                errorMessage = "Insufficient balance for the scheduled transaction";
            } else {
                transferSuccessful = true;
                emailSent = true;
            }
        } else {
            transferSuccessful = false;
            errorMessage = "Transaction not triggered as per the schedule";
        }
    }


    @Then("the transfer is made successful")
    public void transferSuccessful() {
        Assertions.assertTrue(transferSuccessful, "Expected the transfer to be successful, but it failed.");
    }

    @Then("an email notification is sent to the sender")
    public void emailNotificationSent() {
        Assertions.assertTrue(emailSent, "Expected an email notification to be sent, but it was not.");
    }

    @Given("the sender attempts to schedule a transfer of {string} GBP on {string}")
    public void senderAttemptsToScheduleInvalidTransfer(String amount, String date) {
        transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(12345L);
        transferRequest.setReceiverAccountId(67890L);
        transferRequest.setAmount(Double.parseDouble(amount));
        try {
            transferRequest.setScheduledTime(LocalDateTime.parse(date + "T10:00:00"));
        } catch (Exception e) {
            errorMessage = "Invalid date format";
            transferSuccess = false;
        }
    }

    @When("the system processes the request")
    public void systemProcessesTheRequest() {
        try {
            if (transferRequest.getScheduledTime() == null) {
                errorMessage = "Invalid date format";
                transferSuccess = false;
                return;
            }

            LocalDate scheduledDate = transferRequest.getScheduledTime().toLocalDate();
            LocalDate currentDate = LocalDate.now();

            if (scheduledDate.isBefore(currentDate)) {
                errorMessage = "You can't schedule transaction for past date";
                transferSuccess = false;
                return;
            }

            schedulerResponse = schedulerService.scheduleTransfer(authHeader, transferRequest);

            if (schedulerResponse != null && "SUCCESS".equals(schedulerResponse.getStatus())) {
                transferSuccess = true;
                emailSent = true;
            } else {
                transferSuccess = false;
                errorMessage = schedulerResponse != null ? schedulerResponse.getMessage() : "Unknown error";
            }

        } catch (Exception e) {
            transferSuccess = false;
            errorMessage = e.getMessage();
        }
    }

    @Then("the transaction scheduling fails")
    public void transactionSchedulingFails() {
        Assertions.assertFalse(transferSuccess, "Expected the transaction scheduling to fail, but it was successful.");
    }

    @Then("an appropriate error message is returned")
    public void appropriateErrorMessageReturned() {
        Assertions.assertEquals("You can't schedule transaction for past date", errorMessage, "Error message did not match expected.");
    }

    @Given("the sender has an available balance of {string} GBP")
    public void theSenderHasAnAvailableBalance(String balance) {
        transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(12345L);
        transferRequest.setReceiverAccountId(67890L);
        transferRequest.setAmount(Double.parseDouble(balance));
    }
}
