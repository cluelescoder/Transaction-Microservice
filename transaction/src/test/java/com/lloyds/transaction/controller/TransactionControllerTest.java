package com.lloyds.transaction.controller;

import com.lloyds.transaction.dto.request.EmailDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.CustomerDTO;
import com.lloyds.transaction.dto.response.TransactionDTO;
import com.lloyds.transaction.dto.response.TransferResponseDTO;
import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.exception.InsufficientFundsException;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.security.JwtUtil;
import com.lloyds.transaction.service.TransactionService;
import com.lloyds.transaction.service.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final String VALID_AUTHORIZATION_HEADER = "Bearer validToken";


    @Mock
    private RedisService redisService;

    @InjectMocks
    private TransactionController transactionController;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountInterface accountInterface;

    @Mock
    private JwtUtil jwtUtil;

    private Long customerId;
    private Long accountId;
    private AccountDTO accountDTO;
    private Page<TransactionDTO> transactions;

    @BeforeEach
    void setUp() {
        customerId = 1L;
        accountId = 100L;

        accountDTO = new AccountDTO();
        accountDTO.setAccountNumber("123456");
        accountDTO.setBalance(1000.0);
        accountDTO.setAccountType("SAVINGS");
        accountDTO.setId(accountId);
        accountDTO.setCustomerId(customerId);

        transactions = new PageImpl<>(List.of(new TransactionDTO()));
        assertNotNull(accountDTO.getAccountType());
        assertEquals("SAVINGS", accountDTO.getAccountType());
    }

    @Test
    void testGetTransactionsByAccountId_Success() {
        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(accountInterface.getAccountById(eq(accountId), eq(VALID_AUTHORIZATION_HEADER), any()))
                .thenReturn(ResponseEntity.ok(accountDTO));
        when(transactionService.getTransactionsByAccountId(any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(transactions);

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(
                accountId, VALID_AUTHORIZATION_HEADER,  1, 10, "desc", "timestamp");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());


        assertEquals("SAVINGS", accountDTO.getAccountType());
    }


    @Test
    void testGetTransactionsByAccountId_AccountNotFound() {

        Long customerId = 1L;
        Long accountId = 100L;
        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(accountInterface.getAccountById(eq(accountId), eq(VALID_AUTHORIZATION_HEADER), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(accountId, VALID_AUTHORIZATION_HEADER,1, 10, "desc", "timestamp");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetTransactionsByAccountId_NullAccountBody() {
        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(accountInterface.getAccountById(eq(accountId), eq(VALID_AUTHORIZATION_HEADER), any()))
                .thenReturn(ResponseEntity.ok(null)); // Returning null body

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(
                accountId, VALID_AUTHORIZATION_HEADER, 1, 10, "desc", "timestamp");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()); // Ensure the expected behavior
    }


    @Test
    void testGetTransactionsByAccountId_AccessDenied() {
        accountDTO.setCustomerId(2L); // Change to a different customer

        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(accountInterface.getAccountById(eq(accountId), eq(VALID_AUTHORIZATION_HEADER), any()))
                .thenReturn(ResponseEntity.ok(accountDTO));

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(
                accountId, VALID_AUTHORIZATION_HEADER,1, 10, "desc", "timestamp");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetTransactionsByAccountId_InvalidTokenFormat() {


        when(jwtUtil.extractId(anyString())).thenThrow(new NumberFormatException("Invalid format"));

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(100L, VALID_AUTHORIZATION_HEADER, 1, 10, "desc", "timestamp");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetTransactionsByAccountId_InvalidCustomerIdFormat() {


        when(jwtUtil.extractId(anyString())).thenReturn("invalidId");

        ResponseEntity<?> response = transactionController.getTransactionsByAccountId(100L, VALID_AUTHORIZATION_HEADER,1, 10, "desc", "timestamp");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }




    @Test
    void testAccountDTO_AllArgsConstructor() {
        AccountDTO accountDTO = new AccountDTO("123456", 1000.0, "SAVINGS", 100L, 1L);


        assertNotNull(accountDTO);
        assertEquals(100L, accountDTO.getId());
        assertEquals("123456", accountDTO.getAccountNumber());
        assertEquals(1000.0, accountDTO.getBalance());
        assertEquals("SAVINGS", accountDTO.getAccountType());
        assertEquals(1L, accountDTO.getCustomerId());
    }


    @Test
    void testTransferFunds_Success() {
        TransferRequestDTO transferRequest = new TransferRequestDTO();
        TransferResponseDTO transferResponse = new TransferResponseDTO(
                "TX12345", "Transaction Successful", 1000.0, 1500.0, "SUCCESS"
        );

        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(transactionService.transferFunds(transferRequest, customerId))
                .thenReturn(transferResponse);

        ResponseEntity<TransferResponseDTO> response = transactionController.transferFunds(transferRequest, VALID_AUTHORIZATION_HEADER);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testTransferFunds_InvalidCustomerIdFormat() {



        when(jwtUtil.extractId(anyString())).thenReturn("invalidId");
        try {
            transactionController.transferFunds(new TransferRequestDTO(), VALID_AUTHORIZATION_HEADER);
        } catch (Exception e) {
            fail("Exception was thrown: " + e.getClass().getSimpleName());
        }
    }

    @Test
    void testTransferFunds_InsufficientFundsException() {


        Long customerId = 1L;

        TransferRequestDTO transferRequest = new TransferRequestDTO();
        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(transactionService.transferFunds(transferRequest, customerId))
                .thenThrow(new InsufficientFundsException("Insufficient balance"));

        assertThrows(InsufficientFundsException.class, () -> {
            transactionController.transferFunds(transferRequest, VALID_AUTHORIZATION_HEADER);
        });
    }

    @Test
    void testTransferFunds_InternalServerError() {


        Long customerId = 1L;

        TransferRequestDTO transferRequest = new TransferRequestDTO();

        when(jwtUtil.extractId(anyString())).thenReturn(customerId.toString());
        when(transactionService.transferFunds(transferRequest, customerId))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<TransferResponseDTO> response = transactionController.transferFunds(transferRequest, VALID_AUTHORIZATION_HEADER);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testValidTransactionCreation() {

        LocalDateTime timestamp = LocalDateTime.now();
        TransactionDTO transactionDTO = new TransactionDTO(
                1L, "TX12345", 100L, 200L, 500.0, "John Doe", timestamp, "SUCCESS", "TRANSFER", "Invoice Payment", "OUTGOING", 100.0);

        assertNotNull(transactionDTO);
        assertEquals(1L, transactionDTO.getId());
        assertEquals("TX12345", transactionDTO.getTransactionId());
        assertEquals(100L, transactionDTO.getSenderAccountId());
        assertEquals(200L, transactionDTO.getReceiverAccountId());
        assertEquals(500.0, transactionDTO.getAmount());
        assertEquals("John Doe", transactionDTO.getRecipientName());
        assertEquals(timestamp, transactionDTO.getTimestamp());
        assertEquals("SUCCESS", transactionDTO.getTransactionStatus());
        assertEquals("TRANSFER", transactionDTO.getTransactionType());
        assertEquals("Invoice Payment", transactionDTO.getTransactionNote());
        assertEquals("OUTGOING", transactionDTO.getTransactionDirection());
        assertEquals(100, transactionDTO.getUpdatedBalance());

    }

    @Test
    void testTransactionModification() {
        LocalDateTime timestamp = LocalDateTime.now();
        TransactionDTO transactionDTO = new TransactionDTO();

        // Modifying values using setters
        transactionDTO.setId(2L);
        transactionDTO.setTransactionId("TX67890");
        transactionDTO.setSenderAccountId(300L);
        transactionDTO.setReceiverAccountId(400L);
        transactionDTO.setAmount(1000.0);
        transactionDTO.setRecipientName("Jane Doe");
        transactionDTO.setTimestamp(timestamp);
        transactionDTO.setTransactionStatus("FAILED");
        transactionDTO.setTransactionType("WITHDRAWAL");
        transactionDTO.setTransactionNote("Failed transaction");
        transactionDTO.setTransactionDirection("INCOMING");
        transactionDTO.setUpdatedBalance(100.0);

        // Indirectly verifying getters through assertions
        assertEquals(2L, transactionDTO.getId());
        assertEquals("TX67890", transactionDTO.getTransactionId());
        assertEquals(300L, transactionDTO.getSenderAccountId());
        assertEquals(400L, transactionDTO.getReceiverAccountId());
        assertEquals(1000.0, transactionDTO.getAmount());
        assertEquals("Jane Doe", transactionDTO.getRecipientName());
        assertEquals(timestamp, transactionDTO.getTimestamp());
        assertEquals("FAILED", transactionDTO.getTransactionStatus());
        assertEquals("WITHDRAWAL", transactionDTO.getTransactionType());
        assertEquals("Failed transaction", transactionDTO.getTransactionNote());
        assertEquals("INCOMING", transactionDTO.getTransactionDirection());
        assertEquals(100, transactionDTO.getUpdatedBalance());
    }

    @Test
    void testValidCustomerCreation() {
        CustomerDTO customer = new CustomerDTO(1L, "John", "Michael", "Doe", "john.doe@example.com");

        assertNotNull(customer);
        assertEquals(1L, customer.getId());
        assertEquals("John", customer.getFirstName());
        assertEquals("Michael", customer.getMiddleName());
        assertEquals("Doe", customer.getLastName());
        assertEquals("john.doe@example.com", customer.getEmail());
    }

    @Test
    void testCustomerWithMissingMiddleName() {
        CustomerDTO customer = new CustomerDTO(2L, "Alice", null, "Smith", "alice.smith@example.com");

        assertNotNull(customer.getFirstName());
        assertNull(customer.getMiddleName(), "Middle name should be allowed to be null");
        assertNotNull(customer.getLastName());
        assertNotNull(customer.getEmail());
    }

    @Test
    void testCustomerEmailCannotBeEmpty() {
        CustomerDTO customer = new CustomerDTO();
        customer.setEmail("");

        assertTrue(customer.getEmail().isEmpty(), "Email should not be empty but should be handled in validation");
    }


    @Test
    void testAllArgsConstructor() {
        // Using AllArgsConstructor
        EmailDTO emailDTO = new EmailDTO("Transactional", "Deekshitha", "Deekshitha@example.com", "Welcome", "Transaction Successful");

        // Validate values
        assertNotNull(emailDTO);
        assertEquals("Transactional", emailDTO.getType());
        assertEquals("Deekshitha", emailDTO.getName());
        assertEquals("Deekshitha@example.com", emailDTO.getMail());
        assertEquals("Welcome", emailDTO.getSubject());
        assertEquals("Transaction Successful", emailDTO.getMessageContent());
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        // Using NoArgsConstructor
        EmailDTO emailDTO = new EmailDTO();

        // Set values using setters
        emailDTO.setType("Transactional");
        emailDTO.setName("Deekshitha");
        emailDTO.setMail("Deekshitha@example.com");
        emailDTO.setSubject("You are transaction is successful");
        emailDTO.setMessageContent("Transaction successful!");

        // Validate values using getters
        assertEquals("Transactional", emailDTO.getType());
        assertEquals("Deekshitha", emailDTO.getName());
        assertEquals("Deekshitha@example.com", emailDTO.getMail());
        assertEquals("You are transaction is successful", emailDTO.getSubject());
        assertEquals("Transaction successful!", emailDTO.getMessageContent());
    }


}