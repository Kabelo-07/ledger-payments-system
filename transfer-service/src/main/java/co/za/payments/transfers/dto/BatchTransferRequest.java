package co.za.payments.transfers.dto;

import co.za.payments.transfers.exception.InvalidBatchSizeException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchTransferRequest(@NotEmpty @NotNull List<AccountTransferRequest> transferRequests) {

        public void validate(int maxSize) {
          if (null == transferRequests || transferRequests.isEmpty()) {
             throw new InvalidBatchSizeException("At-least one transfer is required");
          }

          if (transferRequests.size() > maxSize) {
              throw new InvalidBatchSizeException(transferRequests.size(), maxSize);
          }
        }
}
