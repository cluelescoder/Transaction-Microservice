package com.lloyds.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.lloyds.transaction.dto.request.EmailDTO;
import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.AccountDTO;
import com.lloyds.transaction.dto.response.CustomerDTO;
import com.lloyds.transaction.dto.response.TransactionDTO;
import com.lloyds.transaction.dto.response.TransferResponseDTO;
import com.lloyds.transaction.entity.Transaction;
import com.lloyds.transaction.exception.AccountNotFoundException;
import com.lloyds.transaction.exception.DataAccessException;
import com.lloyds.transaction.exception.InsufficientFundsException;
import com.lloyds.transaction.feign.AccountInterface;
import com.lloyds.transaction.feign.CustomerInterface;
import com.lloyds.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountInterface accountFiegnClient;
    private final CustomerInterface customerFeignClient;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private static final String JWT_TOKEN = "jwtToken";

    @Value("${service.api.key}")
    private String apiKey;

    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountInterface accountFiegnClient, CustomerInterface customerFeignClient, PubSubTemplate pubSubTemplate ,ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.accountFiegnClient = accountFiegnClient;
        this.customerFeignClient = customerFeignClient;
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }


    @Override
    @Transactional
    public TransferResponseDTO transferFunds(TransferRequestDTO transferRequest, Long customerID) {
        log.info("Initiating fund transfer. Sender Account ID: {}, Receiver Account ID: {}, Amount: {}",
                transferRequest.getSenderAccountId(), transferRequest.getReceiverAccountId(), transferRequest.getAmount());

        log.info("Fetch accounts for the customer using Feign client");
        ResponseEntity<List<AccountDTO>> response = accountFiegnClient.getAccountsByCustomerId(JWT_TOKEN,apiKey, customerID);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to fetch sender's accounts, status: {}", response.getStatusCode());
            throw new AccountNotFoundException("Failed to fetch accounts, status: " + response.getStatusCode());
        }

        log.info("Fetch receiver account");
        ResponseEntity<AccountDTO> receiverResponse = accountFiegnClient.getAccountById(transferRequest.getReceiverAccountId(), JWT_TOKEN, apiKey);
        if (receiverResponse.getStatusCode() != HttpStatus.OK || receiverResponse.getBody() == null) {
            log.error("Failed to fetch receiver account, status: {}", receiverResponse.getStatusCode());
            throw new AccountNotFoundException("Failed to fetch receiver account, status: " + receiverResponse.getStatusCode());
        }

        AccountDTO senderAccount = null;
        AccountDTO receiverAccount = null;
        List<AccountDTO> accounts = response.getBody();
        receiverAccount = receiverResponse.getBody();

        log.info("Find sender account in the fetched accounts list");
        for (AccountDTO account : accounts) {
            if (account.getId().equals(transferRequest.getSenderAccountId())) {
                senderAccount = account;
            }
        }

        if (senderAccount == null) {
            log.error("Sender account not found: {}", transferRequest.getSenderAccountId());
            throw new AccountNotFoundException("Sender account not found");
        }

        log.info(" Validate sufficient funds in the sender account");
        if (senderAccount.getBalance() < transferRequest.getAmount()) {
            log.warn("Insufficient funds in sender account: {}. Requested amount: {}, Available balance: {}",
                    senderAccount.getId(), transferRequest.getAmount(), senderAccount.getBalance());
            throw new InsufficientFundsException("Insufficient funds in the sender account");
        }

        log.info("Perform the transfer");
        double senderNewBalance = senderAccount.getBalance() - transferRequest.getAmount();
        double receiverNewBalance = receiverAccount.getBalance() + transferRequest.getAmount();

        log.info("Update account balances using Feign Client");
        log.info("Updating balances for sender account: {} and receiver account: {}", senderAccount.getId(), receiverAccount.getId());
        accountFiegnClient.updateAccountBalance(senderAccount.getId(), BigDecimal.valueOf(senderNewBalance), JWT_TOKEN, apiKey);
        accountFiegnClient.updateAccountBalance(receiverAccount.getId(), BigDecimal.valueOf(receiverNewBalance), JWT_TOKEN, apiKey);

        log.info("Generate a unique transaction ID");
        String transactionId = "LLB" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        log.info("Generated unique transaction ID: {}", transactionId);

        log.info("Create and save the transaction record");
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setSenderAccountId(senderAccount.getId());
        transaction.setReceiverAccountId(receiverAccount.getId());
        transaction.setAmount(transferRequest.getAmount());
        transaction.setTransactionStatus("Success");
        transaction.setTransactionTimestamp(LocalDateTime.now());
        transaction.setTransactionNote(transferRequest.getNote());
        transaction.setTransactionType(transferRequest.getTransactionType());
        transaction.setSenderBalance(senderNewBalance);
        transaction.setReceiverBalance(receiverNewBalance);
        transaction.setRecipientName(transferRequest.getReceiverName());
        transactionRepository.save(transaction);

        TransferResponseDTO responseDTO = new TransferResponseDTO();
        responseDTO.setTransactionId(transactionId);
        responseDTO.setSourceBalance(senderNewBalance);
        responseDTO.setDestinationBalance(receiverNewBalance);
        responseDTO.setMessage("Transfer successful");
        responseDTO.setTransactionStatus(transaction.getTransactionStatus());
        publishTransactionCompletionEvent(responseDTO, transferRequest,customerID);
        return responseDTO;
    }


    public Page<TransactionDTO> getTransactionsByAccountId(
            Long accountId, int page, int size, String sortOrder, String sortBy) {
        log.info("Fetching transactions for account ID: {}, status: {}, startDate: {}, endDate: {}, page: {}, size: {}, sortBy: {}, sortOrder: {}",
                accountId, page, size, sortBy, sortOrder);

        try {
            if (page < 1) {
                log.warn("Invalid page index: {}. Must be 1 or greater.", page);
                throw new IllegalArgumentException("Page index must be 1 or greater.");
            }

            Sort sort = createSort(sortBy, sortOrder);

            PageRequest pageRequest = PageRequest.of(page - 1, size, sort);

            Page<Transaction> transactions = transactionRepository.findTransactionsByAccountId(
                    accountId,
                    pageRequest);

            log.info("Fetched {} transactions for account ID: {}", transactions.getTotalElements(), accountId);

            return transactions.map(transaction -> {
                if (transaction.getSenderAccountId() == null) {
                    throw new NullPointerException("Sender account ID is null for transaction ID: " + transaction.getId());
                }
                String transactionDirection = transaction.getSenderAccountId().equals(accountId) ? "Debit" : "Credit";
                return new TransactionDTO(transaction, transactionDirection);
            });

        } catch (IllegalArgumentException e) {
            log.error("Invalid pagination parameters: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid pagination parameters: " + e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("Database error while fetching transactions", e);
            throw new DataAccessException(e.getMessage() + " while fetching transactions") {};
        } catch (NullPointerException e) {
            log.error("Unexpected null value encountered: {}", e.getMessage(), e);
            throw new NullPointerException("Unexpected null value encountered: " + e.getMessage());
        }
    }

    private Sort createSort(String sortBy, String sortOrder) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "timestamp"; // Default sorting field
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "asc"; // Default sorting order
        }

        Sort.Direction direction = Sort.Direction.fromString(sortOrder);

        switch (sortBy.toLowerCase()) {
            case "amount":
                return Sort.by(direction, "amount");
            case "timestamp":
                return Sort.by(direction, "transactionTimestamp");
            default:
                throw new IllegalArgumentException("Invalid sortBy parameter. Supported values: 'amount', 'timestamp'");
        }
    }

    public void publishTransactionCompletionEvent(TransferResponseDTO transferResponse, TransferRequestDTO transferRequest, Long customerId) {

        ResponseEntity<CustomerDTO> customerResponse = customerFeignClient.getCustomerById(customerId);
        CustomerDTO customer = Optional.ofNullable(customerResponse.getBody())
                .orElseThrow(() -> {
                    log.error("Customer not found for ID: {}", customerId);
                    return new NullPointerException("Customer not found");
                });

        ResponseEntity<AccountDTO> senderResponse = accountFiegnClient.getAccountById(transferRequest.getReceiverAccountId(), JWT_TOKEN, apiKey);
        AccountDTO account = Optional.ofNullable(senderResponse.getBody())
                .orElseThrow(() -> {
                    log.error("Account not found for ID: {}", transferRequest.getReceiverAccountId());
                    return new IllegalArgumentException("Receiver account not found");
                });

        try {
            EmailDTO transactionMessage = new EmailDTO(
                    "transactioncomplete",
                    customer.getFirstName(),
                    customer.getEmail(),
                    "Your Transaction has been successfully completed",
                    "We are pleased to inform you that your recent transaction has been successfully processed.\n\n" +
                            "An amount of GBP" + transferRequest.getAmount() + " has been debited from your account " +
                            maskAccountNumber(account.getAccountNumber()) + " with transaction ID " + transferResponse.getTransactionId()+"."
            );

            String jsonMessage = objectMapper.writeValueAsString(transactionMessage);
            pubSubTemplate.publish("Transaction_Complete_Management", jsonMessage);
            log.info("Published transaction completion message to Pub/Sub for transaction ID: {}", transferResponse.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish transaction completion event: {}", e.getMessage());
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "XXXX" + accountNumber.substring(accountNumber.length() - 4);
    }

}
