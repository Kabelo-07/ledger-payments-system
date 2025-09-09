package co.za.payments.transfers.config;

import co.za.payments.transfers.exception.ErrorResponse;
import co.za.payments.transfers.exception.LedgerServiceException;
import co.za.payments.transfers.exception.TransferApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class LedgerFeignErrorDecoder implements ErrorDecoder {

    private final Default errorDecoder = new Default();

    @Override
    public Exception decode(String s, Response response) {
        String body = readBodyAsString(response);

        return switch (response.status()) {
            case 400, 404, 422, 500 -> mapToLedgerException(body);
            default -> errorDecoder.decode(s, response);
        };
    }

    private String readBodyAsString(Response response) {
        if (null == response.body()) {
            return Strings.EMPTY;
        }

        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException ex) {
            throw new TransferApplicationException("INTERNAL_ERROR", "Unable to process data");
        }
    }

    private LedgerServiceException mapToLedgerException(String body) {
        try {
            var resp = new ObjectMapper().readValue(body, ErrorResponse.class);
            return new LedgerServiceException(resp);
        } catch (JsonProcessingException e) {
            throw new TransferApplicationException("SERVICE_ERROR", "Unable to process service response");
        }
    }
}
