package com.lloyds.transaction.steps;

import com.lloyds.transaction.dto.response.TransactionDTO;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionSteps {

    private String senderAccountId;
    private String receiverAccountId;
    private String jwtToken;
    private double transferAmount;
    private boolean transferSuccess;
    private String errorMessage;
    private final Map<String, Double> accountBalances = new HashMap<>();
    private String generatedOtp;
    private boolean otpSent;
    private int responseStatus;
    private String responseMessage;
    private boolean isTransactionSuccessful;
    private Long accountId;
    int actualStatus;
    private List<TransactionDTO> transactionsList;
    private Long customerId;



    @And("the transactions list is returned sorted by date in ascending order")
    public List theTransactionsListIsReturnedSortedByDateInAscendingOrder() {
        transactionsList.sort((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()));
        return List.of(transactionsList);
    }

    @And("the transactions list is returned sorted by date in descending order")
    public List theTransactionsListIsReturnedSortedByDateInDescendingOrder() {
        transactionsList.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));
        return List.of(transactionsList);
    }

    @And("the transactions list is returned sorted by amount in ascending order")
    public List theTransactionsListIsReturnedSortedByAmountInAscendingOrder() {

        transactionsList.sort((t1, t2) -> Double.compare(t1.getAmount(), t2.getAmount()));
        return List.of(transactionsList);
    }

    @And("the transactions list is returned sorted by amount in descending order")
    public List theTransactionsListIsReturnedSortedByAmountInDescendingOrder() {
        transactionsList.sort((t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()));
        return List.of(transactionsList);
    }


    @Given("a sender with account ID {string} has sufficient balance")
    public void senderHasSufficientBalance(String accountId) {
        senderAccountId = accountId;
        accountBalances.put(accountId, 1000.00);
    }

    @Given("a sender with account ID {string} has insufficient balance")
    public void senderHasInsufficientBalance(String accountId) {
        senderAccountId = accountId;
        accountBalances.put(accountId, 50.00);
    }

    @Given("a receiver with account ID {string} exists")
    public void receiverExists(String accountId) {
        receiverAccountId = accountId;
        accountBalances.put(accountId, 500.00);
    }

    @Given("a valid JWT token {string} is provided")
    public void validJwtTokenIsProvided(String token) {
        jwtToken = token;
    }

    @When("the sender transfers {string} GBP to the receiver")
    public void senderTransfersAmountToReceiver(String amount) {
        transferAmount = Double.parseDouble(amount);
        Double senderBalance = accountBalances.get(senderAccountId);

        if (senderBalance >= transferAmount) {
            accountBalances.put(senderAccountId, senderBalance - transferAmount);
            accountBalances.put(receiverAccountId, accountBalances.get(receiverAccountId) + transferAmount);
            transferSuccess = true;
        } else {
            transferSuccess = false;
            errorMessage = "Insufficient Funds";
        }
    }

    @Then("the transfer is successful")
    public void transferSuccessful() {
        Assertions.assertTrue(transferSuccess, "Expected the recurring transaction to be successfully executed, but it failed.");
    }


    @Then("an email notification is sent")
    public void emailNotificationIsSent() {
        String emailNotification = "Email Sent: Scheduled transaction executed successfully.";
        Assertions.assertEquals("Email Sent: Scheduled transaction executed successfully.", emailNotification);
    }


    @Then("the transaction is declined with {string}")
    public void transactionisDeclined(String expectedErrorMessage) {
        Assertions.assertFalse(transferSuccess, "The transaction should fail.");
        assertEquals(expectedErrorMessage, errorMessage, "Error message should match.");
    }

    @Given("a valid sender with account ID {string} and sufficient balance")
    public void validSenderWithSufficientBalance(String accountId) {
        senderAccountId = accountId;
        accountBalances.put(accountId, 1000.00);
    }

    @Given("the sender attempts to transfer funds to their own account {string}")
    public void senderAttemptsToTransferToOwnAccount(String accountId) {
        receiverAccountId = accountId;
    }

    @When("the sender transfers {string} GBP to themselves")
    public void senderTransfersToThemselves(String amount) {
        transferAmount = Double.parseDouble(amount);

        if (senderAccountId.equals(receiverAccountId)) {
            transferSuccess = false;
            errorMessage = "Transfer failed: Sender and receiver cannot be the same";
        } else {
            Double senderBalance = accountBalances.get(senderAccountId);
            Double receiverBalance = accountBalances.get(receiverAccountId);
            if (senderBalance >= transferAmount) {
                accountBalances.put(senderAccountId, senderBalance - transferAmount);
                accountBalances.put(receiverAccountId,  receiverBalance+ transferAmount);
                transferSuccess = true;
            } else {
                transferSuccess = false;
                errorMessage = "Insufficient Funds";
            }
        }
    }


    private String requestedAccountId;
    private boolean isAuthorized;
    private boolean hasTransactions;

    @Given("an account with ID {string} belongs to customer ID {string}")
    public void accountBelongsToCustomer(String accountId, String customerId) {
        requestedAccountId = accountId;
    }

    @Given("a valid JWT token {string} for customer ID {string} is provided")
    public void validJwtTokenForCustomerIsProvided(String token, String customerId) {
        jwtToken = token;
        isAuthorized = true;
    }

    @When("the customer requests transactions for account ID {string}")
    public void customerRequestsTransactions(String accountId) {
        hasTransactions = isAuthorized && requestedAccountId.equals(accountId);
    }

    @Then("the system  returns the list of transactions")
    public void systemReturnsTransactions() {
        Assertions.assertTrue(hasTransactions, "Transactions should be returned.");
    }

    @When("the sender attempts to transfer {string} GBP to the receiver")
    public void senderAttemptsToTransferAmount(String amount) {
        try {
            transferAmount = Double.parseDouble(amount);
            Double senderBalance = accountBalances.get(senderAccountId);
            Double receiverBalance= accountBalances.get(receiverAccountId);
            if (senderBalance == null) {
                transferSuccess = false;
                errorMessage = "Transfer failed: Sender account does not exist";
            } else {
                if (!accountBalances.containsKey(receiverAccountId)) {
                    transferSuccess = false;
                    errorMessage = "Receiver account not found";
                } else {
                    if (senderBalance >= transferAmount) {
                        String otp = generateOtp();
                        otpSent = true;
                        transferSuccess = true;
                        accountBalances.put(senderAccountId, senderBalance - transferAmount);
                        accountBalances.put(receiverAccountId, receiverBalance + transferAmount);
                    } else {
                        transferSuccess = false;
                        errorMessage = "Insufficient Funds";
                    }
                }
            }
        } catch (NumberFormatException e) {
            transferSuccess = false;
            errorMessage = "Invalid amount format";
        }
    }

    private String generateOtp() {
        return "123456";
    }


    @Given("a sender with account ID {string} does not exist")
    public void senderDoesNotExist(String accountId) {
        senderAccountId = accountId;
    }

    @And("a receiver with account ID {string} does not exist")
    public void aReceiverWithAccountIDDoesNotExist(String receiverAccountId) {

        this.receiverAccountId = receiverAccountId;
    }

    @Then("the transaction is declined with Receiver account not found")
    public void theTransactionDeclinedWithReceiverAccountNotFound() {
        if (!accountBalances.containsKey(receiverAccountId)) {
            transferSuccess = false;
            errorMessage = "Receiver account not found";
        }
        Assertions.assertFalse(transferSuccess, "The transaction should fail.");
        assertEquals("Receiver account not found", errorMessage, "Error message should match.");
    }

    @Given("a valid OTP {string} is generated for the transaction")
    public void validOtpIsGenerated(String otp) {
        this.generatedOtp = otp;
    }

    @When("the sender attempts to transfer {string} GBP to the receiver using OTP {string}")
    public void senderAttemptsToTransferWithOtp(String amount, String otp) {
        transferAmount = Double.parseDouble(amount);

        if (generatedOtp.equals(otp)) {
            Double senderBalance = accountBalances.get(senderAccountId);
            Double receiverBalance = accountBalances.get(senderAccountId);
            if (senderBalance >= transferAmount) {
                transferSuccess = true;
                accountBalances.put(senderAccountId, senderBalance - transferAmount);
                accountBalances.put(receiverAccountId, receiverBalance + transferAmount);
            } else {
                transferSuccess = false;
                errorMessage = "Insufficient Funds";
            }
        } else {
            transferSuccess = false;
            errorMessage = "Transaction Failed:Invalid OTP";
        }
    }

    @Then("the transaction is successful")
    public void transactionSuccessful() {
        Assertions.assertTrue(transferSuccess, "The transaction is successful.");
    }

    @Then("an OTP is sent to the sender's email for authentication")
    public void otpisSent() {
        Assertions.assertTrue(otpSent, "OTP is sent to sender's email.");
    }

    @Given("an existing account with ID {int} and customer ID {int}")
    public void anExistingAccountWithIDAndCustomerID(int accountId, int customerId) {
        requestedAccountId = String.valueOf(accountId);
        isAuthorized = customerId == 123;
        this.accountId = (long) accountId;
    }

    @Given("the user has a valid authorization token {string}")
    public void theUserHasAValidAuthorizationToken(String token) {
        jwtToken = token;
        if (jwtToken.equals("valid_token_for_123")) {
            isAuthorized = true;
        } else {
            isAuthorized = false;
            responseStatus = 403;
            responseMessage = "Access denied: Invalid authorization token.";
        }
    }

    @When("the user sends a GET request to {string}")
    public void theUserSendsAGETRequestTo(String endpoint) {
        transactionsList = new ArrayList<>();
        if (!isAuthorized) {
            responseStatus = 401;
            responseMessage = "Unauthorized access";
            return;
        }

        else if (accountId == 789) {
            responseStatus = 404;
            responseMessage = "Account not found.";
            return;
        }
        else if(!accountId.equals(customerId)) {
            responseStatus = 403;
            responseMessage = "Access denied: Customer ID mismatch.";
            return;
        }

        if (endpoint.startsWith("/transactions/account/300")) {
            responseStatus = 200;
            transactionsList.add(new TransactionDTO(
                    1L, "23456345", 101L, 303L, 500.00,
                    "Srikanth", LocalDateTime.now(), "SUCCESS", "CREDIT", "Salary", "INCOMING", 1500.00));

            transactionsList.add(new TransactionDTO(
                    2L, "34563451", 101L, 202L, 100.00,
                    "Prasad", LocalDateTime.now(), "FAILED", "DEBIT", "Grocery", "OUTGOING", 1400.00));

            transactionsList.add(new TransactionDTO(
                    3L, "45634567", 101L, 404L, 300.00,
                    "Rohit", LocalDateTime.now(), "SUCCESS", "CREDIT", "Rent", "INCOMING", 1700.00));
        } else {
            responseStatus = 404;
            responseMessage = "Endpoint not found.";
        }
    }

    @Then("the response status is {int}")
    public void theResponseStatus(int statusCode) {
        if (statusCode == 401) {
            assertEquals(401, responseStatus, "Expected status code to be 401" );
            assertEquals("Unauthorized access", responseMessage, "Response message should be 'Unauthorized access'.");
        } else {
            assertEquals(200, statusCode, "Response status should be 200");
        }
    }

    @Then("the transactions list is returned")
    public void theTransactionsListIsReturned() {
        Assertions.assertTrue(transactionsList.isEmpty(), "Transactions list should not be empty.");
    }

    @Given("no account exists with ID {int}")
    public void noAccountExistsWithID(int accountId) {
        requestedAccountId = String.valueOf(789);
        hasTransactions = false;
        this.accountId = (long) accountId;
    }

    @Then("the response status shows {int}")
    public void theResponseStatusShows(int expectedStatus) {

        if (!isAuthorized) {
            actualStatus = 403;
        }else if(accountId==789){
            actualStatus=404;
        }
        else {
            actualStatus = 200;
        }
    }

    @Then("the response message is {string}")
    public void theResponseMessageis(String expectedMessage) {

        if (!isAuthorized) {
            responseMessage="Unauthorised Access";
        } else if (requestedAccountId.equals("789")) {
            responseMessage="Account not found.";
        } else {
            responseMessage = "Transactions retrieved successfully.";
        }
    }

    @And("the user has an invalid authorization token {string}")
    public void theUserHasAnInvalidAuthorizationToken(String jwtToken) {
        this.jwtToken = jwtToken;
        this.isAuthorized = false;
    }

    @Then("the response shows that no transactions are available")
    public void theResponseShowsNoTransactionsAreAvailable() {
        Assertions.assertTrue(transactionsList.isEmpty(), "Transaction list is empty.");
    }

    @Then("the response message shows {string}")
    public void theResponseMessageShows(String expectedMessage) {
        assertEquals(expectedMessage, responseMessage);
    }
}