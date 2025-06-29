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
            String methodName = input.getHttpMethod() != null ? input.getHttpMethod() : "POST";

            if ("GET".equalsIgnoreCase(methodName)) {
                List<FxUploadDto> latestRates = getLatestFxRates();
                String responseJson = objectMapper.writeValueAsString(latestRates);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(responseJson);
            } else if ("POST".equalsIgnoreCase(methodName)) {
                List<FxUploadDto> fxDataList = objectMapper.readValue(input.getBody(), new TypeReference<List<FxUploadDto>>(){});
                for (FxUploadDto fxData : fxDataList) {
                    validate(fxData);
                }
                saveToDatabase(fxDataList);
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

    private void validate(FxUploadDto dto) {
        if (dto == null
                || dto.getCurrencyCode() == null || dto.getCurrencyCode().isEmpty()
                || dto.getBid() == null
                || dto.getAsk() == null) {
            throw new ValidationException("Missing required fields: currencyCode, bid, ask");
        }
        if (dto.getBid().compareTo(BigDecimal.ZERO) < 0 || dto.getAsk().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Bid/Ask cannot be negative");
        }
    }

    private void saveToDatabase(List<FxUploadDto> dataList) {
        String deleteSql = "DELETE FROM fx_upload WHERE currency_code = ?";
        String insertSql = "INSERT INTO fx_upload (currency_code, bid, ask, spread) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;

        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // Remove old records
            deleteStmt = conn.prepareStatement(deleteSql);
            for (FxUploadDto data : dataList) {
                deleteStmt.setString(1, data.getCurrencyCode());
                deleteStmt.addBatch();
            }
            deleteStmt.executeBatch();

            // Insert new records
            insertStmt = conn.prepareStatement(insertSql);
            for (FxUploadDto data : dataList) {
                insertStmt.setString(1, data.getCurrencyCode());
                insertStmt.setBigDecimal(2, data.getBid());
                insertStmt.setBigDecimal(3, data.getAsk());
                insertStmt.setBigDecimal(4, data.getAsk().subtract(data.getBid()));
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException("Database rollback failed", ex);
                }
            }
            throw new RuntimeException("Database operation failed", e);
        } finally {
            closeQuietly(insertStmt);
            closeQuietly(deleteStmt);
            closeQuietly(conn);
        }
    }

    private List<FxUploadDto> getLatestFxRates() {
        String sql = "SELECT currency_code, bid, ask, spread, updated_at " +
                "FROM fx_upload ";
        List<FxUploadDto> rates = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                FxUploadDto dto = new FxUploadDto();
                dto.setCurrencyCode(rs.getString("currency_code"));
                dto.setBid(rs.getBigDecimal("bid"));
                dto.setAsk(rs.getBigDecimal("ask"));
                dto.setSpread(rs.getBigDecimal("spread"));
                dto.setUpdatedAt(rs.getString("updated_at"));
                rates.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
        return rates;
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }


}