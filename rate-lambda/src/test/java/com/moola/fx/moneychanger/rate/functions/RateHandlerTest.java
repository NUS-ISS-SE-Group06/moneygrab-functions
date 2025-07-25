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
import java.math.RoundingMode;
import java.sql.Timestamp;
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
        basicRate.setCurrencyCode("JPY");
        basicRate.setUnit("1000");
        basicRate.setTradeType("BUY_SELL");
        basicRate.setTradeDeno("ALL");
        basicRate.setTradeRound(2);
        basicRate.setRawBid(BigDecimal.valueOf(0.0086709165));
        basicRate.setRawAsk(BigDecimal.valueOf(0.0086724642));
        basicRate.setSpread(BigDecimal.valueOf(0.1));
        basicRate.setSkew(BigDecimal.valueOf(0));
        basicRate.setWsBid(BigDecimal.ZERO);
        basicRate.setWsAsk(BigDecimal.ZERO);
        basicRate.setRefBid(BigDecimal.ZERO);
        basicRate.setRefAsk(BigDecimal.ONE);
        basicRate.setDpBid(BigDecimal.valueOf(3));
        basicRate.setDpAsk(BigDecimal.valueOf(4));
        basicRate.setMarBid(BigDecimal.valueOf(1));
        basicRate.setMarAsk(BigDecimal.valueOf(0.2));
        basicRate.setCfBid(BigDecimal.valueOf(8.67));
        basicRate.setCfAsk(BigDecimal.valueOf(0.115));
        basicRate.setProcessedAt(new Timestamp(1753253891205L));
        basicRate.setProcessedBy(1);

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
        assertEquals(1L, result.getMoneyChangerId());
        assertEquals("JPY", result.getCurrencyCode());
        assertEquals("1000", result.getUnit());
        assertEquals("BUY_SELL", result.getTradeType());
        assertEquals("ALL", result.getTradeDeno());
        assertEquals(2, result.getTradeRound());
        assertEquals(BigDecimal.valueOf(0.1), result.getSpread());
        assertEquals(BigDecimal.ZERO, result.getSkew());
        assertEquals(BigDecimal.ZERO, result.getRefBid());
        assertEquals(BigDecimal.ONE, result.getRefAsk());
        assertEquals(BigDecimal.valueOf(3), result.getDpBid());
        assertEquals(BigDecimal.valueOf(4), result.getDpAsk());
        assertEquals(BigDecimal.valueOf(1), result.getMarBid());
        assertEquals(BigDecimal.valueOf(0.2), result.getMarAsk());
        assertEquals(BigDecimal.valueOf(8.67), result.getCfBid());
        assertEquals(BigDecimal.valueOf(0.115), result.getCfAsk());
        assertNotNull(result.getProcessedAt());
        assertEquals(1, result.getProcessedBy());

        assertEquals(BigDecimal.valueOf(8.670917), result.getRawBid());
        assertEquals(BigDecimal.valueOf(8.672464), result.getRawAsk());

        assertEquals(BigDecimal.valueOf(8.62169), result.getWsBid());
        assertEquals(BigDecimal.valueOf(8.72169), result.getWsAsk());

        assertEquals(BigDecimal.valueOf(8.5350), result.getRtBid());
        assertEquals(BigDecimal.valueOf(0.1144), result.getRtAsk());

        System.out.printf("wsBid: %s, wsAsk: %s, rtBid: %s, rtAsk: %s%n",
                result.getWsBid(), result.getWsAsk(), result.getRtBid(), result.getRtAsk());
    }


    @Test
    void testHandleRequest_validPost_standardMethod2() throws Exception {
        // Given
        BasicRate basicRate = new BasicRate();
        basicRate.setMoneyChangerId(1L);
        basicRate.setCurrencyCode("JPY");
        basicRate.setUnit("1000");
        basicRate.setTradeType("BUY_SELL");
        basicRate.setTradeDeno("ALL");
        basicRate.setTradeRound(2);
        basicRate.setRawBid(BigDecimal.valueOf(0.0086709165));
        basicRate.setRawAsk(BigDecimal.valueOf(0.0086724642));
        basicRate.setSpread(BigDecimal.valueOf(0.1));
        basicRate.setSkew(BigDecimal.valueOf(0));
        basicRate.setWsBid(BigDecimal.ZERO);
        basicRate.setWsAsk(BigDecimal.ZERO);
        basicRate.setRefBid(BigDecimal.ONE);
        basicRate.setRefAsk(BigDecimal.ZERO);
        basicRate.setDpBid(BigDecimal.valueOf(3));
        basicRate.setDpAsk(BigDecimal.valueOf(4));
        basicRate.setMarBid(BigDecimal.valueOf(1));
        basicRate.setMarAsk(BigDecimal.valueOf(0.2));
        basicRate.setCfBid(BigDecimal.valueOf(8.67));
        basicRate.setCfAsk(BigDecimal.valueOf(0.115));
        basicRate.setProcessedAt(new Timestamp(1753253891205L));
        basicRate.setProcessedBy(1);

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
        assertEquals(1L, result.getMoneyChangerId());
        assertEquals("JPY", result.getCurrencyCode());
        assertEquals("1000", result.getUnit());
        assertEquals("BUY_SELL", result.getTradeType());
        assertEquals("ALL", result.getTradeDeno());
        assertEquals(2, result.getTradeRound());
        assertEquals(BigDecimal.valueOf(0.1), result.getSpread());
        assertEquals(BigDecimal.ZERO, result.getSkew());
        assertEquals(BigDecimal.ONE, result.getRefBid());
        assertEquals(BigDecimal.ZERO, result.getRefAsk());
        assertEquals(BigDecimal.valueOf(3), result.getDpBid());
        assertEquals(BigDecimal.valueOf(4), result.getDpAsk());
        assertEquals(BigDecimal.valueOf(1), result.getMarBid());
        assertEquals(BigDecimal.valueOf(0.2), result.getMarAsk());
        assertEquals(BigDecimal.valueOf(8.67), result.getCfBid());
        assertEquals(BigDecimal.valueOf(0.115), result.getCfAsk());
        assertNotNull(result.getProcessedAt()); 
        assertEquals(1, result.getProcessedBy());

        assertEquals(BigDecimal.valueOf(8.670917), result.getRawBid());
        assertEquals(BigDecimal.valueOf(8.672464), result.getRawAsk());

        assertEquals(BigDecimal.valueOf(8.62169), result.getWsBid());
        assertEquals(BigDecimal.valueOf(8.72169), result.getWsAsk());

        assertEquals(BigDecimal.valueOf(8.6700), result.getRtBid());
        assertEquals(BigDecimal.valueOf(8.7391), result.getRtAsk());

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
