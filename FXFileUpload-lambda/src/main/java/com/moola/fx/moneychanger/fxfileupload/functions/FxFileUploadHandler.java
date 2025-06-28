package com.moola.fx.moneychanger.fxfileupload.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moola.fx.moneychanger.fxfileupload.config.DatabaseConfig;
import com.moola.fx.moneychanger.fxfileupload.dto.FxUploadDto;
import com.moola.fx.moneychanger.fxfileupload.exception.ValidationException;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FxFileUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String method = input.getHttpMethod() != null ? input.getHttpMethod() : "POST";

            if ("GET".equalsIgnoreCase(method)) {
                List<FxUploadDto> latestRates = getLatestFxRates();
                String responseJson = objectMapper.writeValueAsString(latestRates);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(responseJson);
            } else if ("POST".equalsIgnoreCase(method)) {
                FxUploadDto fxData = parseBody(input.getBody());
                validate(fxData);
                saveToDatabase(fxData);

                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("{\"message\":\"FX data saved successfully\"}");
            } else {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(405)
                        .withBody("{\"error\":\"Method Not Allowed\"}");
            }
        } catch (ValidationException ve) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("{\"error\":\"" + ve.getMessage() + "\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"Server error: " + e.getMessage() + "\"}");
        }
    }

    private FxUploadDto parseBody(String body) throws Exception {
        return objectMapper.readValue(body, FxUploadDto.class);
    }

    private void validate(FxUploadDto dto) {
        if (dto == null
                || dto.getCurrency_code() == null || dto.getCurrency_code().isEmpty()
                || dto.getBid() == null
                || dto.getAsk() == null) {
            throw new ValidationException("Missing required fields: currency_code, bid, ask");
        }
        if (dto.getBid().compareTo(BigDecimal.ZERO) < 0 || dto.getAsk().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Bid/Ask cannot be negative");
        }
    }

    private void saveToDatabase(FxUploadDto data) {
        String deleteSql = "DELETE FROM fx_upload WHERE currency_code = ?";
        String insertSql = "INSERT INTO fx_upload (currency_code, bid, ask, spread) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;

        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Remove old record (if any)
            deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setString(1, data.getCurrency_code());
            deleteStmt.executeUpdate();

            // 2. Insert new record
            insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, data.getCurrency_code());
            insertStmt.setBigDecimal(2, data.getBid());
            insertStmt.setBigDecimal(3, data.getAsk());
            insertStmt.setBigDecimal(4, data.getAsk().subtract(data.getBid()));
            insertStmt.executeUpdate();

            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    // Optionally log rollback error
                }
            }
            throw new RuntimeException("Database operation failed", e);
        } finally {
            // Close resources in reverse order of allocation
            try { if (insertStmt != null) insertStmt.close(); } catch (SQLException ignore) {}
            try { if (deleteStmt != null) deleteStmt.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }

    // New: Query the latest FX rates for all currencies
    private List<FxUploadDto> getLatestFxRates() {
        String sql = "SELECT currency_code, bid, ask, spread, updated_at " +
                "FROM fx_upload " +
                "WHERE updated_at = (SELECT MAX(updated_at) FROM fx_upload u2 WHERE u2.currency_code = fx_upload.currency_code)";
        List<FxUploadDto> rates = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                FxUploadDto dto = new FxUploadDto();
                dto.setCurrency_code(rs.getString("currency_code"));
                dto.setBid(rs.getBigDecimal("bid"));
                dto.setAsk(rs.getBigDecimal("ask"));
                dto.setSpread(rs.getBigDecimal("spread"));
                dto.setUpdated_at(rs.getString("updated_at"));
                rates.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
        return rates;
    }
}