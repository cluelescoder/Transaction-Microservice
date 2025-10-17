package com.lloyds.transaction.feign;


import com.lloyds.transaction.dto.response.CustomerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@PropertySource("classpath:config.properties")
@FeignClient(name = "customer-service", url = "${lloyds.customer.service.url}") // URL can be your customer service URL
public interface CustomerInterface {

    @GetMapping("customers/{id}")
    ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) ;

}

