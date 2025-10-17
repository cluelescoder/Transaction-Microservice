Feature: Transaction Management
  As a bank customer,
  I want to perform transactions securely,
  So that I can transfer funds and view my transaction history.

  Background:
    Given a sender with account ID "12345" has sufficient balance
    And a receiver with account ID "67890" exists
    And a valid JWT token "validToken123" is provided
    And the user has a valid authorization token "Bearer token-containing-123"
    And an existing account with ID 300 and customer ID 123
    And a valid OTP "123456" is generated for the transaction

  Scenario: Successful fund transfer
    When the sender transfers "100.00" GBP to the receiver
    Then the transfer is successful
    And an email notification is sent

  Scenario: Insufficient funds during transfer
    Given a sender with account ID "23456" has insufficient balance
    When the sender transfers "5000.00" GBP to the receiver
    Then the transaction is declined with "Insufficient Funds"

  Scenario: Fetching transactions for an account
    Given an account with ID "12345" belongs to customer ID "9876"
    And a valid JWT token "validToken123" for customer ID "9876" is provided
    When the customer requests transactions for account ID "12345"
    Then the system  returns the list of transactions

  Scenario: Sender and receiver are the same
    Given a valid sender with account ID "12345" and sufficient balance
    And the sender attempts to transfer funds to their own account "12345"
    When the sender transfers "100.00" GBP to themselves
    Then the transaction is declined with "Transfer failed: Sender and receiver cannot be the same"

  Scenario: Transfer with non-numeric amount
    When the sender attempts to transfer "abc" GBP to the receiver
    Then the transaction is declined with "Invalid amount format"

  Scenario: Receiver account does not exist
    Given a receiver with account ID "56789" does not exist
    When the sender attempts to transfer "100.00" GBP to the receiver
    Then the transaction is declined with Receiver account not found

  Scenario: Sender account does not exist
    Given a sender with account ID "34529" does not exist
    When the sender attempts to transfer "100.00" GBP to the receiver
    Then the transaction is declined with "Transfer failed: Sender account does not exist"

  Scenario: Valid OTP for transaction and transaction made successfully
    When the sender attempts to transfer "100.00" GBP to the receiver using OTP "123456"
    Then the transaction is successful

  Scenario: Invalid OTP for transaction and transaction failed
    When the sender attempts to transfer "100.00" GBP to the receiver using OTP "654321"
    Then the transaction is declined with "Transaction Failed:Invalid OTP"

  Scenario: OTP is sent for transaction authentication
    When the sender attempts to transfer "100.00" GBP to the receiver
    Then an OTP is sent to the sender's email for authentication

  Scenario: Successfully get transactions for an account
    When the user sends a GET request to "/transactions/account/300"
    Then the response status is 200
    And the transactions list is returned

  Scenario: Get transactions fails when account is not found
    Given no account exists with ID 789
    When the user sends a GET request to "/transactions/account/789"
    Then the response status shows 404
    And the response message is "Account not found."

  Scenario: Get transactions fails due to customer ID mismatch
    Given an existing account with ID 300 and customer ID 999
    When the user sends a GET request to "/transactions/account/300"
    Then the response status shows 403
    And the response message is "Access denied: Customer ID mismatch."


  Scenario: Successfully sort transactions by amount in ascending order
    When the user sends a GET request to "/transactions/account/300?sort=amount&order=asc"
    Then the response status is 200
    And the transactions list is returned sorted by amount in ascending order

  Scenario: Successfully sort transactions by amount in descending order
    When the user sends a GET request to "/transactions/account/300?sort=amount&order=desc"
    Then the response status is 200
    And the transactions list is returned sorted by amount in descending order

  Scenario: Successfully sort transactions by date in ascending order
    When the user sends a GET request to "/transactions/account/300?sort=date&order=asc"
    Then the response status is 200
    And the transactions list is returned sorted by date in ascending order

  Scenario: Successfully sort transactions by date in descending order
    When the user sends a GET request to "/transactions/account/300?sort=date&order=desc"
    Then the response status is 200
    And the transactions list is returned sorted by date in descending order

  Scenario: No transactions available for an account
    Given an existing account with ID 400 and customer ID 789
    And the user has a valid authorization token "Bearer token-containing-789"
    When the user sends a GET request to "/transactions/account/400"
    Then the response status is 200
    And the response shows that no transactions are available

  Scenario: Unauthorized user attempts to retrieve transactions
    Given an existing account with ID 500 and customer ID 321
    And the user has an invalid authorization token "Bearer invalid-token"
    When the user sends a GET request to "/transactions/account/500"
    Then the response status is 401
    And the response message shows "Unauthorized access"

  Scenario: Successfully schedule a future transaction
    When the sender schedules a transfer of "200.00" GBP to the receiver on "2025-03-01"
    Then the transaction is scheduled successfully
    And an email notification is sent


  Scenario: Schedule a recurring transaction (Daily)
    When the sender schedules a recurring transfer of "100.00" GBP to the receiver with a "DAILY" recurrence pattern
    Then the recurring transaction is scheduled successfully
    And an email notification is sent

  Scenario: Schedule a recurring transaction (Weekly)
    When the sender schedules a recurring transfer of "100.00" GBP to the receiver with a "WEEKLY" recurrence pattern
    Then the recurring transaction is scheduled successfully
    And an email notification is sent

  Scenario: Schedule a recurring transaction (Monthly)
    When the sender schedules a recurring transfer of "100.00" GBP to the receiver with a "MONTHLY" recurrence pattern
    Then the recurring transaction is scheduled successfully
    And an email notification is sent

  Scenario: Fail to schedule a transaction with a past date
    Given the sender attempts to schedule a transfer of "150.00" GBP on "2024-09-01"
    When the system processes the request
    Then the transaction scheduling fails
    And an appropriate error message is returned

  Scenario: Fail to schedule a transaction due to insufficient balance
    Given the sender has an available balance of "50.00" GBP
    When the sender schedules a transfer of "200.00" GBP to the receiver on "2025-03-01"
    Then the transaction scheduling fails