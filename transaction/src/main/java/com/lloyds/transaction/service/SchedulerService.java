package com.lloyds.transaction.service;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.SchedulerResponseDTO;
import com.lloyds.transaction.exception.SchedulerException;

public interface SchedulerService {
    SchedulerResponseDTO scheduleTransfer(String authHeader, TransferRequestDTO transferRequest) throws SchedulerException;
}
