package com.lloyds.transaction.dto.response;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponseDTO {
    private String transactionId;
    private String message;
    private double sourceBalance;
    private double destinationBalance;
    private String transactionStatus;
}