package com.lloyds.transaction.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SchedulerResponseDTO {
    private String status;
    private String message;
    private String jobId;
    private String triggerId;
}