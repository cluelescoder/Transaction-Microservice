package com.lloyds.transaction.service;

import com.lloyds.transaction.dto.request.TransferRequestDTO;
import com.lloyds.transaction.dto.response.TransferResponseDTO;
import com.lloyds.transaction.dto.response.TransactionDTO;
import org.springframework.data.domain.Page;


public interface TransactionService {

    TransferResponseDTO transferFunds(TransferRequestDTO transferRequest, Long customerID);

    Page<TransactionDTO> getTransactionsByAccountId(
            Long accountId, int page, int size, String sortOrder, String sortBy);

    void publishTransactionCompletionEvent(TransferResponseDTO transferResponse, TransferRequestDTO transferRequest, Long customerId);
}
