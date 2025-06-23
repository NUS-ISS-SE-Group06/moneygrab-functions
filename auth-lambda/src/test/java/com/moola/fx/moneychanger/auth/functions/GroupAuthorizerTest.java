package com.moola.fx.moneychanger.auth.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.moola.fx.moneychanger.auth.constants.AuthConstant;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupAuthorizerTest {
    private ConfigurableJWTProcessor<SecurityContext> mockProcessor;
    private GroupAuthorizer handler;
    private Context ctx;  // ← our NoOpLambdaContext

    @BeforeEach
    void setup() {
        // 1) create the mock processor
        mockProcessor = Mockito.mock(ConfigurableJWTProcessor.class);
        // 2) inject into the handler
        handler = new GroupAuthorizer(mockProcessor);
        // 3) use the no-op Lambda Context
        ctx = new NoOpLambdaContext();
    }

    private APIGatewayV2CustomAuthorizerEvent makeEvent(String token, String routeKey) {
        APIGatewayV2CustomAuthorizerEvent evt = new APIGatewayV2CustomAuthorizerEvent();
        evt.setHeaders(Map.of("authorization", "Bearer " + token));
        // HTTP API routeKey comes in the form "GET /api/v1/xyz"
        evt.setRouteKey(routeKey);
        return evt;
    }

    @Test
    void allowWhenGroupMatches() throws Exception {
        // Fake an “Admin” token for the /currencies route
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AuthConstant.ISSUER)
                .claim(AuthConstant.CLAIM_CLIENT_ID, AuthConstant.CLIENT_ID_VALUE)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .claim(AuthConstant.COGNITO_GROUPS, List.of("MoneyChangerStaff"))
                .subject("test-user")
                .claim(AuthConstant.CLAIM_TOKEN_USE, AuthConstant.CLAIM_TOKEN_USE_VALUE)
                .build();

        Mockito.when(mockProcessor.process(Mockito.anyString(), Mockito.isNull()))
                .thenReturn(claims);

        var resp = handler.handleRequest(
                makeEvent("fake-token", "GET /api/v1/currencies"),
                ctx                                   // ← pass ctx, not null
        );

        System.out.println(resp);
        assertTrue((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
    }

    @Test
    void denyWhenNoGroupMatches() throws Exception {
        // Fake a “Guest” token for an Admin-only route
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AuthConstant.ISSUER)
                .claim(AuthConstant.CLAIM_CLIENT_ID, AuthConstant.CLIENT_ID_VALUE)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .claim(AuthConstant.COGNITO_GROUPS, List.of("Guest"))
                .subject("another-user")
                .claim(AuthConstant.CLAIM_TOKEN_USE, AuthConstant.CLAIM_TOKEN_USE_VALUE)
                .build();

        Mockito.when(mockProcessor.process(Mockito.anyString(), Mockito.isNull()))
                .thenReturn(claims);

        var resp = handler.handleRequest(
                makeEvent("fake-token", "POST /api/v1/schemes"),
                ctx                                   // ← pass ctx here too
        );

        assertFalse((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
        assertFalse(resp.containsKey("context"));
    }
}
