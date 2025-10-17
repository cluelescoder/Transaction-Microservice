package com.lloyds.transaction.controller;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;


    @PostMapping("/transfer")
    public ResponseEntity<SchedulerResponseDTO> scheduleTransfer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                                                 @RequestBody TransferRequestDTO transferRequest) {

            log.info("Received request to schedule transfer for sender account: {}", transferRequest.getSenderAccountId());
            SchedulerResponseDTO response = schedulerService.scheduleTransfer(authHeader, transferRequest);
            return ResponseEntity.ok(response);

    }
}