package com.lloyds.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Setter
@Getter
@NoArgsConstructor
@Entity
@Slf4j
@Table(
        name = "transaction",
        indexes = {
                @Index(name = "idx_sender_account", columnList = "senderAccountId"),
                @Index(name = "idx_receiver_account", columnList = "receiverAccountId"),
                @Index(name = "idx_transaction_timestamp", columnList = "transactionTimestamp"),
                @Index(name = "idx_transaction_amount", columnList = "amount")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionId; // UUID-based transaction ID

    @Column(nullable = false)
    private Long senderAccountId;

    @Column(nullable = false)
    private Long receiverAccountId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime transactionTimestamp;

    @Column(nullable = false)
    private String transactionStatus;


    private String transactionType;

    private String transactionNote = "";

    private Double senderBalance;

    private Double receiverBalance;


}
