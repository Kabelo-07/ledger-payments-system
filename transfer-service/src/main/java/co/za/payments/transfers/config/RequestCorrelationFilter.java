package co.za.payments.transfers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static co.za.payments.transfers.config.AppConstants.REQUEST_ID_HEADER_NAME;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var correlationId = Optional
                .ofNullable(request.getHeader(REQUEST_ID_HEADER_NAME))
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(REQUEST_ID_HEADER_NAME, correlationId);
        response.setHeader(REQUEST_ID_HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_HEADER_NAME);
        }

    }
}
