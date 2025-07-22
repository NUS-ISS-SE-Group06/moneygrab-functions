package com.moola.fx.moneychanger.rate.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moola.fx.moneychanger.rate.dto.BasicRate;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RateHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RateHandler handler;
    private Context mockContext;

    @BeforeEach
    void setup() {
        handler = new RateHandler();
        mockContext = null; // Not used in logic
    }

    @Test
    void testHandleRequest_validPost_standardMethod() throws Exception {
        // Given
        BasicRate basicRate = new BasicRate();
        basicRate.setCurrencyCode("USD");
        basicRate.setRawBid(BigDecimal.valueOf(1.00));
        basicRate.setRawAsk(BigDecimal.valueOf(1.20));
        basicRate.setUnit("1");

        String body = objectMapper.writeValueAsString(List.of(basicRate));

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withQueryStringParameters(Map.of("method", "standard"))
                .withBody(body);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        List<ComputedRate> computedRates = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(1, computedRates.size());

        ComputedRate result = computedRates.getFirst();
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(BigDecimal.valueOf(0.20), result.getSpread());
        assertNotNull(result.getSkew());
        assertNotNull(result.getWsBid());
        assertNotNull(result.getWsAsk());
    }

    @Test
    void testHandleRequest_invalidMethod() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("Only POST method is supported"));
    }

    @Test
    void testHandleRequest_unknownMethodStrategy() throws Exception {
        // Given
        String body = objectMapper.writeValueAsString(List.of(new BasicRate()));
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withQueryStringParameters(Map.of("method", "unknown"))
                .withBody(body);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unknown method"));
    }

    @Test
    void testHandleRequest_malformedJson() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withQueryStringParameters(Map.of("method", "standard"))
                .withBody("invalid-json");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Server error"));
    }
}
