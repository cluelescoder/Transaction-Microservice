package com.lloyds.transaction.feign;


import com.lloyds.transaction.dto.response.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.context.annotation.PropertySource;


import java.math.BigDecimal;
import java.util.List;

@PropertySource("classpath:config.properties")
@FeignClient(name = "account-service", url = "${lloyds.account.service.url}")
public interface AccountInterface {

    // Get all accounts for a customer by customer ID
    @GetMapping("/accounts")
    ResponseEntity<List<AccountDTO>> getAccountsByCustomerId(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestHeader("X-Service-API-Key") String apiKey,
            @RequestParam Long customerID

    );

    @GetMapping("/accounts/{id}")
     ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,  @RequestHeader("X-Service-API-Key") String apiKey
    );

    // Update the balance of a particular account
    @PutMapping("/accounts/updateBalance/{accountId}")
    void updateAccountBalance(@PathVariable Long accountId, @RequestParam BigDecimal newBalance,
                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,  @RequestHeader("X-Service-API-Key") String apiKey);

}
