package com.lloyds.transaction.dto.response;

import com.lloyds.transaction.entity.Transaction;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private String transactionId;
    private Long senderAccountId;
    private Long receiverAccountId;
    private double amount;
    private String recipientName;
    private LocalDateTime timestamp;
    private String transactionStatus;
    private String transactionType;
    private String transactionNote;
    private String transactionDirection;
    private Double updatedBalance;

    public TransactionDTO(Transaction transaction, String transactionDirection) {
        this.id = transaction.getId();
        this.transactionId = transaction.getTransactionId();
        this.senderAccountId = transaction.getSenderAccountId();
        this.receiverAccountId = transaction.getReceiverAccountId();
        this.amount = transaction.getAmount();
        this.timestamp = transaction.getTransactionTimestamp();
        this.transactionStatus = transaction.getTransactionStatus();
        this.transactionType = transaction.getTransactionType();
        this.transactionNote = transaction.getTransactionNote();
        this.transactionDirection = transactionDirection;
        this.recipientName = transaction.getRecipientName();
        this.updatedBalance = "Debit".equals(transactionDirection) ? transaction.getSenderBalance() : transaction.getReceiverBalance();

    }
}