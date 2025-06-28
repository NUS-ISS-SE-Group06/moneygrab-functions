package com.moola.fx.moneychanger.fxfileupload.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moola.fx.moneychanger.fxfileupload.config.DatabaseConfig;
import com.moola.fx.moneychanger.fxfileupload.dto.FxUploadDto;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FxFileUploadHandlerTest {

    private FxFileUploadHandler handler;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        handler = new FxFileUploadHandler();
        mockContext = Mockito.mock(Context.class);
    }

    @Test
    void testPost_HappyPath() throws Exception {
        // Arrange
        FxUploadDto dto = new FxUploadDto();
        dto.setCurrency_code("USD");
        dto.setBid(new BigDecimal("1.2"));
        dto.setAsk(new BigDecimal("1.3"));

        String json = new ObjectMapper().writeValueAsString(dto);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(json);

        // Mock DB
        try (MockedStatic<DatabaseConfig> dbConfigMock = Mockito.mockStatic(DatabaseConfig.class)) {
            Connection mockConn = Mockito.mock(Connection.class);
            PreparedStatement deleteStmt = Mockito.mock(PreparedStatement.class);
            PreparedStatement insertStmt = Mockito.mock(PreparedStatement.class);

            dbConfigMock.when(DatabaseConfig::getConnection).thenReturn(mockConn);
            Mockito.when(mockConn.prepareStatement(Mockito.anyString()))
                    .thenReturn(deleteStmt, insertStmt);

            Mockito.when(deleteStmt.executeUpdate()).thenReturn(1);
            Mockito.when(insertStmt.executeUpdate()).thenReturn(1);

            // Act
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

            // Assert
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("FX data saved successfully"));
        }
    }

    @Test
    void testGet_HappyPath() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET");

        try (MockedStatic<DatabaseConfig> dbConfigMock = Mockito.mockStatic(DatabaseConfig.class)) {
            Connection mockConn = Mockito.mock(Connection.class);
            PreparedStatement mockStmt = Mockito.mock(PreparedStatement.class);
            ResultSet mockRs = Mockito.mock(ResultSet.class);

            dbConfigMock.when(DatabaseConfig::getConnection).thenReturn(mockConn);
            Mockito.when(mockConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);
            Mockito.when(mockStmt.executeQuery()).thenReturn(mockRs);

            // Simulate two records
            Mockito.when(mockRs.next()).thenReturn(true, false);
            Mockito.when(mockRs.getString("currency_code")).thenReturn("USD");
            Mockito.when(mockRs.getBigDecimal("bid")).thenReturn(new BigDecimal("1.2"));
            Mockito.when(mockRs.getBigDecimal("ask")).thenReturn(new BigDecimal("1.3"));
            Mockito.when(mockRs.getBigDecimal("spread")).thenReturn(new BigDecimal("0.1"));
            Mockito.when(mockRs.getString("updated_at")).thenReturn("2024-06-28 12:34:56");

            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("USD"));
        }
    }

    @Test
    void testPost_MissingFields_BadRequest() throws Exception {
        // No bid/ask
        FxUploadDto dto = new FxUploadDto();
        dto.setCurrency_code("USD");
        String json = new ObjectMapper().writeValueAsString(dto);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(json);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing required fields"));
    }

    @Test
    void testUnsupportedMethod() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("PUT");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("Method Not Allowed"));
    }

    @Test
    void testDatabaseException_Returns500() throws Exception {
        FxUploadDto dto = new FxUploadDto();
        dto.setCurrency_code("USD");
        dto.setBid(new BigDecimal("1.2"));
        dto.setAsk(new BigDecimal("1.3"));
        String json = new ObjectMapper().writeValueAsString(dto);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withBody(json);

        try (MockedStatic<DatabaseConfig> dbConfigMock = Mockito.mockStatic(DatabaseConfig.class)) {
            dbConfigMock.when(DatabaseConfig::getConnection).thenThrow(new SQLException("DB Down"));

            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

            assertEquals(500, response.getStatusCode());
            assertTrue(response.getBody().contains("Server error"));
        }
    }
}