package com.lloyds.transaction.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.lloyds.transaction.dto.request.EmailDTO;
import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.CustomerDTO;
import com.lloyds.transaction.dto.response.TransactionDTO;
import com.lloyds.transaction.dto.response.TransferResponseDTO;
import com.lloyds.transaction.entity.Transaction;
import com.lloyds.transaction.exception.DataAccessException;
import com.lloyds.transaction.exception.InsufficientFundsException;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.feign.CustomerInterface;
import com.lloyds.transaction.repository.TransactionRepository;
import com.lloyds.transaction.service.TransactionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;


import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountInterface accountInterface;

    @Mock
    private CustomerInterface customerInterface;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PubSubTemplate pubSubTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private static final Long ACCOUNT_ID = 1L;

    private TransferResponseDTO transferResponse;
    private TransferRequestDTO transferRequest;
    private CustomerDTO customer;
    private AccountDTO account;
    private Long customerId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "apiKey", "test-api-key");

        transferResponse = new TransferResponseDTO();
        transferResponse.setTransactionId("TXN12345");

        transferRequest = new TransferRequestDTO();
        transferRequest.setAmount(500.0);
        transferRequest.setReceiverAccountId(200L);

        customer = new CustomerDTO();
        customer.setFirstName("John");
        customer.setEmail("john@example.com");
        customer.setLastName("jue");
        customer.setMiddleName("kong");
        customer.setId(1L);


        account = new AccountDTO();
        account.setAccountNumber("1234567890");

        customerId = 1L;

    }

    @Test
    void transferFunds_SuccessfulTransfer() {

        TransferRequestDTO request = new TransferRequestDTO();
        request.setSenderAccountId(1L);
        request.setReceiverAccountId(2L);
        request.setAmount(100.0);
        request.setNote("Test Transfer");

        // Create sender and receiver accounts
        AccountDTO sender = new AccountDTO();
        sender.setAccountNumber("1234567890");
        sender.setBalance(500.0);
        sender.setAccountType("Checking");
        sender.setId(1L);
        sender.setCustomerId(101L);

        AccountDTO receiver = new AccountDTO();
        receiver.setAccountNumber("0987654321");
        receiver.setBalance(300.0);
        receiver.setAccountType("Savings");
        receiver.setId(2L);
        receiver.setCustomerId(102L);

        List<AccountDTO> senderAccounts = Collections.singletonList(sender);


        lenient().when(accountInterface.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(senderAccounts, HttpStatus.OK));
        lenient().when(accountInterface.getAccountById(anyLong(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(receiver, HttpStatus.OK));
        lenient().when(customerInterface.getCustomerById(anyLong())).thenReturn(ResponseEntity.ok(customer));

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponseDTO response = transactionService.transferFunds(request, 101L);


        assertNotNull(response);
        assertEquals(400.0, response.getSourceBalance());
        assertEquals(400.0, response.getDestinationBalance());
        assertEquals("Transfer successful", response.getMessage());
        assertEquals("Success", response.getTransactionStatus());

        // Verify method calls
        verify(accountInterface, times(1))
                .updateAccountBalance(eq(1L), eq(BigDecimal.valueOf(400.0)), anyString(), anyString());
        verify(accountInterface, times(1))
                .updateAccountBalance(eq(2L), eq(BigDecimal.valueOf(400.0)), anyString(), anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        TransferRequestDTO transferRequest = new TransferRequestDTO();
        transferRequest.setSenderAccountId(1L);
        transferRequest.setReceiverAccountId(2L);
        transferRequest.setAmount(500.0);

        AccountDTO senderAccount = new AccountDTO();
        senderAccount.setAccountNumber("123456");
        senderAccount.setBalance(300.0);
        senderAccount.setAccountType("Checking");
        senderAccount.setId(1L);
        senderAccount.setCustomerId(10L);

        AccountDTO receiverAccount = new AccountDTO();
        receiverAccount.setAccountNumber("654321");
        receiverAccount.setBalance(1000.0);
        receiverAccount.setAccountType("Savings");
        receiverAccount.setId(2L);
        receiverAccount.setCustomerId(10L);

        when(accountInterface.getAccountsByCustomerId(anyString(), anyString(),anyLong()))
                .thenReturn(new ResponseEntity<>(List.of(senderAccount), HttpStatus.OK));
        when(accountInterface.getAccountById(eq(2L), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(receiverAccount, HttpStatus.OK));

        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transactionService.transferFunds(transferRequest, 1L));

        assertEquals("Insufficient funds in the sender account", exception.getMessage());
    }

    @Test
    void transferFunds_SenderAccountNotFound() {

        TransferRequestDTO request = new TransferRequestDTO();
        request.setSenderAccountId(1L);
        request.setReceiverAccountId(2L);
        request.setAmount(100.0);
        when(accountInterface.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));
        when(accountInterface.getAccountById(eq(2L), anyString(), anyString()))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body(new AccountDTO()));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.transferFunds(request, 1L));

        assertEquals("Sender account not found", exception.getMessage());
    }


    @ParameterizedTest
    @CsvSource({
            "NOT_FOUND", // Case where the receiver account API returns NOT_FOUND
            "OK"        // Case where the API returns OK but the body is null
    })
    void transferFunds_ReceiverAccountNotFound(HttpStatus status) {
        // Arrange
        TransferRequestDTO request = new TransferRequestDTO();
        request.setSenderAccountId(1L);
        request.setReceiverAccountId(2L);
        request.setAmount(100.0);

        AccountDTO sender = new AccountDTO();
        sender.setAccountNumber("1234567890");
        sender.setBalance(500.0);
        sender.setAccountType("Checking");
        sender.setId(1L);
        sender.setCustomerId(101L);

        List<AccountDTO> senderAccounts = Collections.singletonList(sender);

        when(accountInterface.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(senderAccounts, HttpStatus.OK));

        when(accountInterface.getAccountById(eq(2L), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(null, status)); // Dynamic status

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.transferFunds(request, 1L));

        assertTrue(exception.getMessage().contains("Failed to fetch receiver account"));
    }



    @Test
    void shouldFetchTransactionsSuccessfully() {
        Transaction debitTransaction = new Transaction();
        debitTransaction.setId(1L);
        debitTransaction.setSenderAccountId(ACCOUNT_ID); // Matches accountId, should be "Debit"
        debitTransaction.setAmount(100.0);
        debitTransaction.setTransactionTimestamp(LocalDateTime.now());
        debitTransaction.setReceiverBalance(100.0);
        debitTransaction.setSenderBalance(200.0);

        Transaction creditTransaction = new Transaction();
        creditTransaction.setId(2L);
        creditTransaction.setSenderAccountId(999L); // Different accountId, should be "Credit"
        creditTransaction.setAmount(200.0);
        creditTransaction.setTransactionTimestamp(LocalDateTime.now());

        Page<Transaction> mockPage = new PageImpl<>(List.of(debitTransaction, creditTransaction));

        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID),any()))
                .thenReturn(mockPage);

        Page<TransactionDTO> result = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID,
                1, 5, "asc", "timestamp");

        assertEquals(2, result.getTotalElements());
        assertEquals(200.0, result.getContent().get(0).getUpdatedBalance());

        // Validate the transaction direction logic
        assertEquals("Debit", result.getContent().get(0).getTransactionDirection());
        assertEquals("Credit", result.getContent().get(1).getTransactionDirection());
    }

    @Test
    void shouldThrowExceptionWhenPageIsInvalid() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.getTransactionsByAccountId(
                        ACCOUNT_ID, 0, 5, "asc", "timestamp"));

        assertEquals("Invalid pagination parameters: Page index must be 1 or greater.", exception.getMessage());
    }

    @Test
    void shouldHandleEmptyFilters() {

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderAccountId(ACCOUNT_ID);
        transaction.setAmount(100.0);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID), any()))
                .thenReturn(mockPage);
        Page<TransactionDTO> result = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID, 1, 5, "desc", "amount");

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldHandleEmptyStatusFilter() {
        Page<Transaction> mockPage = new PageImpl<>(Collections.emptyList());

        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID),any()))
                .thenReturn(mockPage);

        Page<TransactionDTO> result = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID,
                1, 5, "asc", "timestamp");

        assertEquals(0, result.getTotalElements()); // Expecting empty page result
    }



    @Test
    void shouldHandleNullSenderAccountId() {

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderAccountId(null);
        transaction.setAmount(100.0);
        transaction.setTransactionTimestamp(LocalDateTime.now());

        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID), any()))
                .thenReturn(mockPage);

        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                transactionService.getTransactionsByAccountId(
                        ACCOUNT_ID,
                        1, 5, "asc", "timestamp"));

        assertTrue(exception.getMessage().contains("Sender account ID is null for transaction ID"));
    }

    @Test
    void shouldThrowExceptionForInvalidSortBy() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transactionService.getTransactionsByAccountId(
                        ACCOUNT_ID,
                        1, 5, "asc", "invalidField"));

        assertTrue(exception.getMessage().contains("Invalid sortBy parameter. Supported values: 'amount', 'timestamp'"));
    }


    @Test
    void shouldSortByAmountDescending() {

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderAccountId(ACCOUNT_ID);
        transaction.setAmount(200.0);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID), any()))
                .thenReturn(mockPage);
        Page<TransactionDTO> result = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID, 1, 5, "desc", "amount");
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldUseDefaultSortingWhenSortByIsNullOrEmpty() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderAccountId(ACCOUNT_ID);
        transaction.setAmount(200.0);
        transaction.setTransactionTimestamp(LocalDateTime.now());

        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID), any()))
                .thenReturn(mockPage);

        // Case 1: sortBy is null
        Page<TransactionDTO> resultWithNullSortBy = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID, 1, 5, "desc", null);
        assertEquals(1, resultWithNullSortBy.getTotalElements());

        // Case 2: sortBy is empty string ""
        Page<TransactionDTO> resultWithEmptySortBy = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID,1, 5, "desc", "");
        assertEquals(1, resultWithEmptySortBy.getTotalElements());
    }


    @Test
    void shouldUseDefaultSortingWhenSortOrderIsNullOrEmpty() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setSenderAccountId(ACCOUNT_ID);
        transaction.setAmount(200.0);
        transaction.setTransactionTimestamp(LocalDateTime.now());

        Page<Transaction> mockPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsByAccountId(eq(ACCOUNT_ID), any()))
                .thenReturn(mockPage);

        // Case: sortOrder is null
        Page<TransactionDTO> resultWithNullSortOrder = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID,1, 5, null, "amount");
        assertEquals(1, resultWithNullSortOrder.getTotalElements());

        // Case: sortOrder is empty
        Page<TransactionDTO> resultWithEmptySortOrder = transactionService.getTransactionsByAccountId(
                ACCOUNT_ID,1, 5, "", "amount");
        assertEquals(1, resultWithEmptySortOrder.getTotalElements());
    }



    @Test
    void testPublishTransactionCompletionEvent_CustomerNotFound() {

        Long customerId = 1L;
        TransferRequestDTO transferRequest = new TransferRequestDTO();
        transferRequest.setReceiverAccountId(123L);
        TransferResponseDTO transferResponse = new TransferResponseDTO();
        transferResponse.setTransactionId("TXN12345");
        ResponseEntity<CustomerDTO> customerResponse = ResponseEntity.ok(null);
        when(customerInterface.getCustomerById(customerId)).thenReturn(customerResponse);
        assertThrows(NullPointerException.class, () -> {
            transactionService.publishTransactionCompletionEvent(transferResponse, transferRequest, customerId);
        });
        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }

    @Test
    void shouldThrowExceptionWhenReceiverAccountNotFound() {
        // Arrange
        when(customerInterface.getCustomerById(anyLong()))
                .thenReturn(ResponseEntity.ok(customer));

        when(accountInterface.getAccountById(eq(transferRequest.getReceiverAccountId()), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.publishTransactionCompletionEvent(transferResponse, transferRequest, customerId);
        });

        assertEquals("Receiver account not found", exception.getMessage());
        verify(accountInterface, times(1)).getAccountById(eq(transferRequest.getReceiverAccountId()), anyString(), anyString());
    }


    @Test
    void testMaskAccountNumber_UsingReflection() throws Exception {

        TransactionServiceImpl transactionService = Mockito.mock(TransactionServiceImpl.class);
        Method method = TransactionServiceImpl.class.getDeclaredMethod("maskAccountNumber", String.class);
        method.setAccessible(true);
        assertNull(method.invoke(transactionService, (Object) null));
        assertEquals("123", method.invoke(transactionService, "123"));
        assertEquals("4567", method.invoke(transactionService, "4567"));
        assertEquals("XXXX5678", method.invoke(transactionService, "12345678"));
        assertEquals("XXXX2345", method.invoke(transactionService, "98762345"));
    }

    @Test
    void testPublishTransactionCompletionEvent_Success() throws JsonProcessingException {

        when(customerInterface.getCustomerById(anyLong())).thenReturn(ResponseEntity.ok(customer));
        when(accountInterface.getAccountById(anyLong(), anyString(), anyString()))
                .thenReturn(ResponseEntity.ok(account));
        when(objectMapper.writeValueAsString(any(EmailDTO.class))).thenReturn("mockJsonMessage");

        transactionService.publishTransactionCompletionEvent(transferResponse, transferRequest, customerId);

        verify(pubSubTemplate, times(1)).publish("Transaction_Complete_Management","mockJsonMessage");
    }

    @Test
    void testPublishTransactionCompletionEvent_ExceptionHandling() throws JsonProcessingException {

        when(customerInterface.getCustomerById(anyLong())).thenReturn(ResponseEntity.ok(customer));
        when(accountInterface.getAccountById(anyLong(), anyString(), anyString()))
                .thenReturn(ResponseEntity.ok(account));
        when(objectMapper.writeValueAsString(any(EmailDTO.class))).thenThrow(new JsonProcessingException("JSON error") {
        });

        transactionService.publishTransactionCompletionEvent(transferResponse, transferRequest, customerId);

        verify(pubSubTemplate, never()).publish(anyString(), anyString());
    }

    @Test
    void transferFunds_ShouldThrowException_WhenFetchingAccountsFails() {

        when(accountInterface.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                transactionService.transferFunds(transferRequest, 1L));

        assertEquals("Failed to fetch accounts, status: 500 INTERNAL_SERVER_ERROR", exception.getMessage());
        verify(accountInterface, times(1)).getAccountsByCustomerId(anyString(), anyString(), anyLong());
    }


    @Test
    void testGetTransactionsByAccountId_DataAccessException() {
        // Arrange
        Long accountId = 1L;
        int page = 1;
        int size = 10;
        String sortOrder = "desc";
        String sortBy = "timestamp";

        // Mock the repository to throw a DataAccessException
        when(transactionRepository.findTransactionsByAccountId(
                eq(accountId),
                any(PageRequest.class))
        ).thenThrow(new DataAccessException("Database error") {
        });

        // Act & Assert
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            transactionService.getTransactionsByAccountId(accountId, page, size, sortOrder, sortBy);
        });

        // Verify the exception message
        assertEquals("Database error while fetching transactions", exception.getMessage());

        // Verify that the repository method was called
        verify(transactionRepository).findTransactionsByAccountId(
                eq(accountId),
                any(PageRequest.class)
        );
    }
    @Test
    void transferFunds_SenderAccountMismatch() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setSenderAccountId(1L);  // Sender ID is 1
        request.setReceiverAccountId(2L);
        request.setAmount(100.0);

        // Create an account with a different ID (simulating mismatch)
        AccountDTO sender = new AccountDTO();
        sender.setAccountNumber("1234567890");
        sender.setBalance(500.0);
        sender.setAccountType("Checking");
        sender.setId(99L); // Different ID, should not match request.getSenderAccountId()
        sender.setCustomerId(101L);

        List<AccountDTO> senderAccounts = Collections.singletonList(sender);

        when(accountInterface.getAccountsByCustomerId(anyString(), anyString(), anyLong()))
                .thenReturn(new ResponseEntity<>(senderAccounts, HttpStatus.OK));

        when(accountInterface.getAccountById(eq(2L), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(new AccountDTO(), HttpStatus.OK));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.transferFunds(request, 101L));

        assertEquals("Sender account not found", exception.getMessage());
    }




}
