package com.moola.fx.moneychanger.auth.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthConstant {

    // Cognito settings
    public static final String ISSUER    =
            "https://cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_XI7LG9T1f";

    public static final String JWKS_URL = "/.well-known/jwks.json";

    public static final String CLIENT_ID_VALUE = "4lcfgcumiu5derbjmd2fm1io34";

    public static final String AUTHORIZATION_HEADER = "authorization";

    public static final String BEARER_REGEX = "(?i)^Bearer\\s+";

    public static final String IS_AUTHORIZED_KEY = "isAuthorized";

    public static final String COGNITO_GROUPS = "cognito:groups";

    public static final String CLAIM_TOKEN_USE = "token_use";

    public static final String CLAIM_CLIENT_ID = "client_id";

    public static final String CLAIM_TOKEN_USE_VALUE = "access";

    public static final Map<String,List<String>> ACCESS_MATRIX;
    static {
        Map<String,List<String>> m = new HashMap<>();
        // MONEY CHANGER OPERATIONS
        m.put("GET#/api/v1/money-changers", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("POST#/api/v1/money-changers", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("PUT#/api/v1/money-changers/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("DELETE#/api/v1/money-changers/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));

        // SCHEMES
        m.put("GET#/api/v1/schemes", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("POST#/api/v1/schemes", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("PUT#/api/v1/schemes/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("DELETE#/api/v1/schemes/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));

        // COMMISSION RATES
        m.put("GET#/api/v1/commission-rates", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("POST#/api/v1/commission-rates", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("PUT#/api/v1/commission-rates/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("DELETE#/api/v1/commission-rates/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));

        // COMPANY COMMISSION SCHEMES
        m.put("GET#/api/v1/company-commission-schemes", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("POST#/api/v1/company-commission-schemes", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("PUT#/api/v1/company-commission-schemes/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));
        m.put("DELETE#/api/v1/company-commission-schemes/{id}", List.of("MoneyGrabAdmin", "MoneyGrabStaff"));

        // CURRENCIES
        m.put("GET#/api/v1/currencies", List.of("MoneyChangerAdmin", "MoneyChangerStaff"));

        // HEALTH CHECK
        m.put("GET#/api/actuator/health", List.of("MoneyGrabAdmin", "MoneyGrabStaff", "MoneyChangerAdmin", "MoneyChangerStaff"));

        ACCESS_MATRIX = Collections.unmodifiableMap(m);
    }

}
