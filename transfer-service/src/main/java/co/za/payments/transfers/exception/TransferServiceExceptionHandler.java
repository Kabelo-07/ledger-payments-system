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
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
@Slf4j
public class TransferServiceExceptionHandler {

    @ExceptionHandler(TransferApplicationException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(TransferApplicationException exception) {
        log.error("Error occurred locating resource", exception);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(LedgerServiceException.class)
    public ResponseEntity<ErrorResponse> handleLedgerServiceException(LedgerServiceException exception) {
        log.error("Error invoking ledger API", exception);

        var response = exception.getResponse();
        return ResponseEntity.status(response.status()).body(response);
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
                .body(new ErrorResponse(BAD_REQUEST.value(), INVALID_REQUEST, "Validation failed", errors));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Object> handleArgumentNotValid(MissingRequestHeaderException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(BAD_REQUEST.value(), MISSING_HEADER, ex.getMessage()));
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticException(Exception exception) {
        log.error("Optimistic lock error occurred ", exception);
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), AppConstants.CONFLICT_CODE, "Conflict error processing transfer"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
        log.error("Error occurred ", exception);

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.name(), exception.getMessage()));
    }

}
