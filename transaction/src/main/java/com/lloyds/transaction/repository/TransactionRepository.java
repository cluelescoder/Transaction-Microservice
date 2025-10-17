package com.lloyds.transaction.repository;

import com.lloyds.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.senderAccountId = :accountId OR t.receiverAccountId = :accountId) ")
    Page<Transaction> findTransactionsByAccountId(
            @Param("accountId") Long accountId,
            Pageable pageable);
}
