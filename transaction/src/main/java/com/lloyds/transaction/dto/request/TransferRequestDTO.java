package com.lloyds.transaction.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Setter
@Getter
@NoArgsConstructor
public class TransferRequestDTO implements Serializable {
    private Long senderAccountId;
    private Long receiverAccountId;
    private String receiverName;
    private String note;
    private String transactionType;
    private LocalDateTime scheduledTime;
    private ZoneId timeZone;
    private double amount;
    private String recurrencePattern;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
