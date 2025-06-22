package com.moola.fx.moneychanger.auth.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.moola.fx.moneychanger.auth.constants.AuthConstant;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URL;
import java.util.*;

public class GroupAuthorizer
        implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, Map<String,Object>> {

    // Build the JWT processor with JWKSourceBuilder
    private static final ConfigurableJWTProcessor<SecurityContext> JWT_PROC;
    static {
        try {
            JWT_PROC = new DefaultJWTProcessor<>();
            URL jwkSetUrl = new URL(AuthConstant.ISSUER + AuthConstant.JWKS_URL);
            JWKSource<SecurityContext> jwkSource =
                    JWKSourceBuilder.create(jwkSetUrl).build();
            JWT_PROC.setJWSKeySelector(
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT processor", e);
        }
    }

    @Override
    public Map<String,Object> handleRequest(
            APIGatewayV2CustomAuthorizerEvent event,
            Context ctx
    ) {
        Map<String,Object> response = new HashMap<>();
        try {
            // --- Extract Bearer token ---
            ctx.getLogger().log("Checking Tokens");
            String auth = event.getHeaders().getOrDefault(AuthConstant.AUTHORIZATION_HEADER, "");
            String token = auth.replaceFirst(AuthConstant.BEARER_REGEX, "");
            ctx.getLogger().log("Processing Tokens");

            // --- Verify signature, issuer, audience, expiry ---
            JWTClaimsSet claims = JWT_PROC.process(token, null);
            if (!AuthConstant.ISSUER.equals(claims.getIssuer())
                    || !claims.getAudience().contains(AuthConstant.CLIENT_ID)
                    || new Date().after(claims.getExpirationTime())) {
                ctx.getLogger().log("Token Validation failed");
                response.put(AuthConstant.IS_AUTHORIZED_KEY, false);
                return response;
            }
            ctx.getLogger().log("Token Validation successful");

            // --- Get user’s Cognito groups ---
            @SuppressWarnings("unchecked")
            List<String> userGroups = (List<String>)claims.getClaim(AuthConstant.COGNITO_GROUPS);
            ctx.getLogger().log("userGroups: "+userGroups);

            // --- Determine allowed groups for this route ---
            String key = event.getRouteKey().replace(" ", "#"); // e.g. "GET#/api/orders"
            List<String> ok = AuthConstant.ACCESS_MATRIX.getOrDefault(key, Collections.emptyList());

            // --- Authorize if there’s any overlap ---
            boolean allowed = userGroups.stream().anyMatch(ok::contains);
            ctx.getLogger().log("URL: "+key+"; Matrix: "+ok+"; Allowed: "+allowed);
            response.put(AuthConstant.IS_AUTHORIZED_KEY, allowed);

        } catch (Exception e) {
            ctx.getLogger().log("Auth failure: " + e);
            response.put(AuthConstant.IS_AUTHORIZED_KEY, false);
        }
        return response;
    }
}

