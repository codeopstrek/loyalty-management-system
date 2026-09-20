package com.loyalty.loyaltyprogram.filter;

import com.loyalty.loyaltyprogram.enums.TransactionType;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates MDC with per-request context — requestId, transactionType,
 * serviceName, instanceId — so every structured log line can be traced
 * back to its request, service, and instance once merged in Elasticsearch.
 * Cleared in {@code finally} to avoid leaking into pooled threads.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingContextFilter implements Filter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String TXN_TYPE_KEY = "transactionType";
    public static final String SERVICE_NAME_KEY = "serviceName";
    public static final String INSTANCE_ID_KEY = "instanceId";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Value("${spring.application.name:loyalty-program}")
    private String applicationName;

    private static final String HOST_NAME = System.getenv().getOrDefault("HOSTNAME", "localhost");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. Populate Application & Host Metadata
            MDC.put(SERVICE_NAME_KEY, applicationName);
            MDC.put(INSTANCE_ID_KEY, HOST_NAME);

            // 2. Handle Request Tracing ID
            String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (!StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(REQUEST_ID_KEY, requestId);

            // 3. Infer Transaction Type from Endpoint URI
            String uri = httpRequest.getRequestURI();
            if (uri.endsWith("/earn")) {
                MDC.put(TXN_TYPE_KEY, TransactionType.EARNED.name());
            } else if (uri.endsWith("/redeem")) {
                MDC.put(TXN_TYPE_KEY, TransactionType.REDEEM.name());
            } else if (uri.endsWith("/refund")) {
                MDC.put(TXN_TYPE_KEY, TransactionType.REFUND.name());
            } else {
                MDC.put(TXN_TYPE_KEY, "NON-TXN");
            }

            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
            chain.doFilter(request, response);

        } finally {
            // Clear all injected keys to prevent thread reuse pollution
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove(TXN_TYPE_KEY);
            MDC.remove(SERVICE_NAME_KEY);
            MDC.remove(INSTANCE_ID_KEY);
        }
    }
}