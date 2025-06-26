package com.moola.fx.moneychanger.auth.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.moola.fx.moneychanger.auth.constants.AuthConstant;
import com.moola.fx.moneychanger.auth.exception.JwtProcessorInitializationException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class MoneyGrabGroupAuthorizer
        implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, Map<String,Object>> {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public MoneyGrabGroupAuthorizer() {
        this.jwtProcessor = buildDefaultProcessor();
    }

    // package-private constructor for tests
    MoneyGrabGroupAuthorizer(ConfigurableJWTProcessor<SecurityContext> jwtProcessor) {
        this.jwtProcessor = jwtProcessor;
    }

    static ConfigurableJWTProcessor<SecurityContext> buildDefaultProcessor() {
        try {
            DefaultJWTProcessor<SecurityContext> proc = new DefaultJWTProcessor<>();
            URL jwkUrl = new URL(AuthConstant.ISSUER + AuthConstant.JWKS_URL);
            JWKSource<SecurityContext> src = JWKSourceBuilder.create(jwkUrl).build();
            proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, src));
            return proc;
        } catch (MalformedURLException | java.text.ParseException e) {
            throw new JwtProcessorInitializationException(
                    "Failed to initialize JWT processor: invalid JWKS URL or parse error", e
            );
        } catch (Exception e) {
            // fallback for other unexpected errors
            throw new JwtProcessorInitializationException(
                    "Unexpected error initializing JWT processor", e
            );
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
            JWTClaimsSet claims = jwtProcessor.process(token, null);
            String tokenIssuer = claims.getIssuer();
            String tokenUse = claims.getStringClaim(AuthConstant.CLAIM_TOKEN_USE);
            Date expiry = claims.getExpirationTime();

            // Cognito access tokens have no “aud”, but do have “client_id”
            String requestClientId = claims.getStringClaim(AuthConstant.CLAIM_CLIENT_ID);

            ctx.getLogger().log("claims: "+claims);
            ctx.getLogger().log("claims: tokenIssuer - "+tokenIssuer);
            ctx.getLogger().log("claims: tokenUse - "+tokenUse);
            ctx.getLogger().log("claims: expiry - "+expiry);

            // reject if issuer wrong, not an access token, wrong client_id, or expired
            if (!AuthConstant.ISSUER.equals(tokenIssuer)
                    || !AuthConstant.CLAIM_TOKEN_USE_VALUE.equals(tokenUse)
                    || !AuthConstant.CLIENT_ID_VALUE.equals(requestClientId)
                    || new Date().after(expiry)) {
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
        } catch (Exception exception) {
            ctx.getLogger().log("Auth failure: " + exception.getMessage());
            response.put(AuthConstant.IS_AUTHORIZED_KEY, false);
        }
        return response;
    }

}

