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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;

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

    // helper to build a “good” token, which you then tweak:
    private JWTClaimsSet.Builder baseBuilder() {
        return new JWTClaimsSet.Builder()
                .issuer(AuthConstant.ISSUER)
                .claim(AuthConstant.CLAIM_CLIENT_ID, AuthConstant.CLIENT_ID_VALUE)
                .claim("token_use", AuthConstant.CLAIM_TOKEN_USE_VALUE)
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .claim(AuthConstant.COGNITO_GROUPS, List.of("MoneyChangerAdmin"))
                .subject("test-user");
    }

    // helper to create a minimal event:
    private APIGatewayV2CustomAuthorizerEvent makeEvent() {
        var e = new APIGatewayV2CustomAuthorizerEvent();
        e.setHeaders(Map.of("authorization", "Bearer dummy"));
        e.setRouteKey("GET /api/v1/currencies");
        return e;
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
                .claim(AuthConstant.COGNITO_GROUPS, List.of(AuthConstant.GROUP_MONEY_CHANGER_STAFF))
                .subject("test-user")
                .claim(AuthConstant.CLAIM_TOKEN_USE, AuthConstant.CLAIM_TOKEN_USE_VALUE)
                .build();

        Mockito.when(mockProcessor.process(anyString(), isNull()))
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
                .claim(AuthConstant.COGNITO_GROUPS, List.of(AuthConstant.GROUP_CUSTOMER))
                .subject("another-user")
                .claim(AuthConstant.CLAIM_TOKEN_USE, AuthConstant.CLAIM_TOKEN_USE_VALUE)
                .build();

        Mockito.when(mockProcessor.process(anyString(), isNull()))
                .thenReturn(claims);

        var resp = handler.handleRequest(
                makeEvent("fake-token", "POST /api/v1/schemes"),
                ctx                                   // ← pass ctx here too
        );

        assertFalse((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
        assertFalse(resp.containsKey("context"));
    }

    @Test
    void buildDefaultProcessor_isNotNull() {
        // simply call it and assert it returns something
        ConfigurableJWTProcessor<SecurityContext> proc =
                GroupAuthorizer.buildDefaultProcessor();
        assertNotNull(proc, "buildDefaultProcessor() should never return null");
    }

    @Test
    void rejectWhenIssuerMismatch() throws Exception {
        // 1) Use a wrong issuer
        JWTClaimsSet badIssuer = baseBuilder()
                .issuer("https://evil.example.com")        // ← not AuthConstant.ISSUER
                .build();

        Mockito.when(mockProcessor.process(anyString(), isNull()))
                .thenReturn(badIssuer);

        @SuppressWarnings("unchecked")
        Map<String,Object> resp = handler.handleRequest(makeEvent(), ctx);
        assertFalse((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
        // context should not be present
        assertFalse(resp.containsKey("context"));
    }

    @Test
    void rejectWhenTokenUseNotAccess() throws Exception {
        // 2) Use a token_use other than "access"
        JWTClaimsSet badUse = baseBuilder()
                .claim("token_use", "id")                  // ← not AuthConstant.CLAIM_TOKEN_USE_VALUE
                .build();

        Mockito.when(mockProcessor.process(anyString(), isNull()))
                .thenReturn(badUse);

        @SuppressWarnings("unchecked")
        Map<String,Object> resp = handler.handleRequest(makeEvent(), ctx);
        assertFalse((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
    }

    @Test
    void rejectWhenClientIdMismatch() throws Exception {
        // 3) Use a wrong client_id
        JWTClaimsSet badClient = baseBuilder()
                .claim(AuthConstant.CLAIM_CLIENT_ID, "wrong-client") // ← not AUTH_CONSTANT.CLIENT_ID_VALUE
                .build();

        Mockito.when(mockProcessor.process(anyString(), isNull()))
                .thenReturn(badClient);

        @SuppressWarnings("unchecked")
        Map<String,Object> resp = handler.handleRequest(makeEvent(), ctx);
        assertFalse((Boolean) resp.get(AuthConstant.IS_AUTHORIZED_KEY));
    }
}

