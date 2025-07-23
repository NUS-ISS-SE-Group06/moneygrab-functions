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
        mockContext = null; // Context not used
    }

    @Test
    void testHandleRequest_validPost_standardMethod() throws Exception {
        // Given
        BasicRate basicRate = new BasicRate();
        basicRate.setMoneyChangerId(1L);
        basicRate.setCurrencyCode("USD");
        basicRate.setUnit("1");
        basicRate.setRawBid(BigDecimal.valueOf(1.00));
        basicRate.setRawAsk(BigDecimal.valueOf(1.20));
        basicRate.setSpread(BigDecimal.valueOf(0.20));
        basicRate.setSkew(BigDecimal.ZERO);
        basicRate.setRefBid(BigDecimal.ZERO);
        basicRate.setRefAsk(BigDecimal.ZERO);
        basicRate.setDpBid(BigDecimal.valueOf(4));
        basicRate.setDpAsk(BigDecimal.valueOf(4));
        basicRate.setMarBid(BigDecimal.valueOf(0.5));
        basicRate.setMarAsk(BigDecimal.valueOf(0.5));
        basicRate.setCfBid(BigDecimal.valueOf(1.05));
        basicRate.setCfAsk(BigDecimal.valueOf(1.10));

        String body = objectMapper.writeValueAsString(List.of(basicRate));
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withQueryStringParameters(Map.of("method", "standard"))
                .withBody(body);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode(), "Expected status 200 for valid request");

        List<ComputedRate> computedRates = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(1, computedRates.size(), "Expected one computed rate");

        ComputedRate result = computedRates.getFirst();
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(1L, result.getMoneyChangerId());

        // Check formula outputs
        assertEquals(BigDecimal.valueOf(0.20), result.getSpread());
        assertEquals(BigDecimal.ZERO, result.getSkew());

        assertNotNull(result.getWsBid(), "wsBid should be calculated");
        assertNotNull(result.getWsAsk(), "wsAsk should be calculated");
        assertNotNull(result.getRtBid(), "rtBid should be calculated");
        assertNotNull(result.getRtAsk(), "rtAsk should be calculated");

        // Optionally, log values for debugging
        System.out.printf("wsBid: %s, wsAsk: %s, rtBid: %s, rtAsk: %s%n",
                result.getWsBid(), result.getWsAsk(), result.getRtBid(), result.getRtAsk());
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
