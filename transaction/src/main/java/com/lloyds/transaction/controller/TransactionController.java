package com.lloyds.transaction.controller;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.TransferResponseDTO;
import com.lloyds.transaction.exception.InsufficientFundsException;
import com.lloyds.transaction.dto.response.TransactionDTO;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.security.JwtUtil;
import com.lloyds.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountInterface accountFiegnClient;
    private final JwtUtil jwtUtil;

    @Value("${service.api.key}")
    private String apiKey;


    /**
     * API endpoint to initiate a fund transfer.
     * @param transferRequest The transfer request data containing sender and receiver information.
     * @param authHeader The authorization header containing the JWT token.
     * @return ResponseEntity with the transfer response details.
     */
    @PostMapping("/transferFunds")
    public ResponseEntity<TransferResponseDTO> transferFunds(
            @RequestBody TransferRequestDTO transferRequest, // Accept the transfer request body as JSON
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader // Extract JWT from Authorization header
    ) {
        try {
            String jwtToken = authHeader.replace("Bearer ", "");
            String customerIdFromToken = jwtUtil.extractId(jwtToken);
            Long customerIdFromTokenAsLong = Long.parseLong(customerIdFromToken);
            TransferResponseDTO transferResponse = transactionService.transferFunds(transferRequest, customerIdFromTokenAsLong);

            return ResponseEntity.ok(transferResponse);
        }
        catch(InsufficientFundsException e){
            throw e;
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TransferResponseDTO());
        }
    }



    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionDTO>> getTransactionsByAccountId(
            @PathVariable Long accountId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestParam(defaultValue = "1", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(defaultValue = "desc", required = false) String sortOrder,
            @RequestParam(defaultValue = "timestamp", required = false) String sortBy) {
        try {

            String token = authorizationHeader.replace("Bearer ", "");
            String customerIdFromToken = jwtUtil.extractId(token);
            Long customerIdFromTokenAsLong = Long.parseLong(customerIdFromToken);

            ResponseEntity<AccountDTO> accountResponse = accountFiegnClient.getAccountById(accountId, authorizationHeader, apiKey);

            if (!accountResponse.getStatusCode().is2xxSuccessful() || accountResponse.getBody() == null) {
                log.warn("Failed to fetch account details for account {}", accountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            AccountDTO accountDTO = accountResponse.getBody();

            if (!Objects.equals(accountDTO.getCustomerId(), customerIdFromTokenAsLong)) {
                log.warn("Access denied: Customer ID mismatch for account {}", accountId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Page<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(
                    accountId, page, size, sortOrder, sortBy);

            return ResponseEntity.ok(transactions);

        } catch (NumberFormatException e) {

            log.error("Invalid customer ID format in token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

