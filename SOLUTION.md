## Batch Transfers Processing (POST /transfers/batch)
* #### Concurrency Approach
    * Transfers in batch are processed in parallel to improve system throughput, this is achieved by using `CompleatableFuture` with a customer `ExecutorService`
    * Each transfer in the batch runs independently and allows multiple ledger entries to be created concurrently.
* #### Thread Configuration
    * A dedicated `ThreadPoolTaskExecutor` is used for batch processing
    * The executor is a managed Spring `@Bean` with configurations (pool size, queue capacity etc..) configured in the application properties file 

## Application Concurrency
 * Adopted optimistic locking implementation, which uses versioning to handle race conditions when multiple transfers hit the same account.
 * Optimistic locking scales well under normal load as it is non-blocking and is an ideal approach in instances where conflicts are rare i.e. for "normal" day to day account transfers. In instances where an account is updated by multiple system throughout the day, Pessimistic locking might be the better solution as it prevents race condition efficiently by locking rows for update.
 * In this solution, when an optimistic lock exception case occurs, Spring `@Retryable` has been implemented to auto-retry the transfer.

## Idempotency
  * Idempotency is forced through the usage of a `Idempotency-Key` header -> this ensures that transfer requests with the same key return previously the processed response to avoid double charges
  * Idempotency is mandatory in the single transfer processing operation (POST /transfer), and is optional in the Batch transfer processing -> if not provided in batch, there is no check done for previous batch transfer

## Error Handling
* The transfer service integrates with the ledger-service, when a transfer is initiated, it needs to be recorded in the ledger.
* The outbox pattern is employed in the transfer service, when a transfer is initiated a outbox transfer event is created and scheduled for processing. This is also responsible for handling error failures and initiate re-tries using exponential backoff in instances where the Ledger API invocation fails
* The `TransferOutboxPlublisher` has a scheduled "job" that runs periodically, picks up events that have been created and not yet processed and invokes the Ledger API to create
  * Create Ledger entries (DEBIT, CREDIT)
  * Debit the source account (fromAccount) and Credit the destination account (toAccount)
* Once this is done, the event is marked as PROCESSED and the transfer status is marked as COMPLETED
* In the event of a failure the retry attempts are updated and backoff policy applied for event to retried `X` number of times
* Once a event reaches `X` number of retries, it is marked as FAILED and the transfer is also marked is FAILED.

## Authentication
No authentication or authorization of requests was implemented in these services

* #### Future design
  * Services should support authentication and authorization with minimal changes
  * All calls done from Transfer Service -> Ledger Service are made through a dedicated `FeignClient` where we can easily inject auth-headers with tokens
  * Controllers and service method structure allows security annotations (`@PreAuthorize`, `@RolesAllowed`) to be added. 
  * Custom Authorization annotations can also be implemented using customer filter auth logic without changing any business logic or structure
* #### Implementation approach
  * Use Spring Security with OAuth2 for service-to-service calls
  * Add filters to validate incoming requests and propagate auth tokens downstream
  * Secure endpoints with role-based and/or permission-based access control


## CI Pipeline
* CI Pipeline has been created using GitHub actions. Each service has its own workflow configuration file under the `.github/workflows` directory in the root payments-system root directory
* On push to remote branch, the CI pipeline starts and runs the below steps
  * Builds the project and checks for bugs
  * runs unit tests
  * runs integration tests