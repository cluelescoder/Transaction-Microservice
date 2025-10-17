package com.lloyds.transaction.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
    private String accountNumber;
    private double balance;
    private String accountType;
    private Long id;
    private Long customerId;
}