package co.za.payments.transfers.exception;

import co.za.payments.transfers.config.AppConstants;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

import static co.za.payments.transfers.config.AppConstants.INVALID_REQUEST;
import static co.za.payments.transfers.config.AppConstants.MISSING_HEADER;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
@Slf4j
public class TransferServiceExceptionHandler {

    @ExceptionHandler(TransferApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(TransferApplicationException exception) {
        log.error("Application Error occurred ", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(LedgerServiceException.class)
    public ResponseEntity<ErrorResponse> handleLedgerServiceException(LedgerServiceException ex) {
        log.error("Error invoking ledger API", ex);

        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        DefaultMessageSourceResolvable::getDefaultMessage)
                );

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(INVALID_REQUEST, "Validation failed", errors));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Object> handleArgumentNotValid(MissingRequestHeaderException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(MISSING_HEADER, ex.getMessage()));
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticException(Exception exception) {
        log.error("Optimistic lock error occurred ", exception);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(AppConstants.CONFLICT_CODE, "Conflict error processing transfer"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
        log.error("Error occurred ", exception);

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), exception.getMessage()));
    }

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransferNotException(TransferNotFoundException exception) {
        log.error("Transfer error occurred ", exception);

        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(SystemInternalException.class)
    public ResponseEntity<ErrorResponse> handleInternalException(SystemInternalException exception) {
        log.error("Internal error occurred", exception);

        return ResponseEntity.internalServerError().body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidBatchSizeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBatchSize(InvalidBatchSizeException exception) {
        log.error("Error processing batch transfers", exception);

        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

}
