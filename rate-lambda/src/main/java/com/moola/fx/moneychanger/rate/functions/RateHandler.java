package com.moola.fx.moneychanger.rate.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moola.fx.moneychanger.rate.dto.BasicRate;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;
import com.moola.fx.moneychanger.rate.service.RateCalculationService;
import com.moola.fx.moneychanger.rate.strategy.RateCalculationStrategy;
import com.moola.fx.moneychanger.rate.strategy.StandardFormula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


public class RateHandler  implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RateCalculationService rateCalculationService;

    static {
        Map<String, RateCalculationStrategy> strategyMap = new HashMap<>();
        strategyMap.put("standard", new StandardFormula());
        rateCalculationService=new RateCalculationService(strategyMap);
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        try {
            String httpMethod = input.getHttpMethod() != null ? input.getHttpMethod() : "POST";

            if ("POST".equalsIgnoreCase(httpMethod)) {
                Map<String, String> queryParams = input.getQueryStringParameters();
                String method = queryParams != null ? queryParams.get("method") : "";

                String requestBody = input.getBody();
                List<BasicRate> basicRates = objectMapper.readValue(requestBody, new TypeReference<List<BasicRate>>() {});
                List<ComputedRate> results =rateCalculationService.processBatch(basicRates,method);

                String responseMessage=objectMapper.writeValueAsString(results);

                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withHeaders(Map.of("Content-type", "application/json"))
                        .withBody(responseMessage);
            } else {
                // ✅ return a 405 if not POST
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(405)
                        .withHeaders(Map.of("Content-type", "application/json"))
                        .withBody("{\"error\":\"Only POST method is supported\"}");
            }
        }  catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-type", "application/json"))
                    .withBody("{\"error\":\"Server error: " + e.getMessage() + "\"}");
        }


    }
}
