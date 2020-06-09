package hms.dbmi.ppm;

import java.util.*;
import org.apache.commons.lang3.Validate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Flag;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;


@SuppressWarnings("ConstantConditions")
public class TokenAuthorizationInterceptor extends AuthorizationInterceptor {

    private ApplicationContext appContext;

    public TokenAuthorizationInterceptor(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) throws AuthenticationException {

        // Check for authorization
        String authHeader = theRequestDetails.getHeader("Authorization");
        if( authHeader != null ) {
            try {
                // Get the token
                String token = getTokenFromHeader(authHeader);

                // Determine if the token is valid, ALL tokens are admin level
                if(validateToken(token)) {
                    System.out.println("Admin token: " + token.substring(0, 3) + new String(new char[token.length() - 3]).replace('\0', '*'));

                    // Allow anything
                    return new RuleBuilder().allowAll().build();
                }

            } catch(Exception e) {
                System.out.println( "Token Exception: " + e );
            }
        }

        // User has not tried to authenticate, set rules
        return new RuleBuilder()
                .allow("Only allow anonymous metadata").metadata().build();
    }

    public static Boolean validateToken(String token) {

        // Check env
        String tokenList = System.getenv().get("AUTHORIZED_TOKENS");
        List<String> tokens = Arrays.asList(tokenList.split(","));
        return tokens.contains(token);
    }

    public static String getTokenFromHeader(String header) {
        Validate.notNull(header, "Authorization header is null");

        // Get the auth header prefix to use
        String prefix = System.getenv("TOKEN_HEADER_PREFIX");
        Validate.notNull(prefix, "TOKEN_HEADER_PREFIX must be set in environment");

        // Ensure it ends with a space
        if (!prefix.endsWith(" ")) {
            prefix = prefix + " ";
        }

        // Ensure the token is in the string
        if (header.length() < prefix.length()) {
            throw new AuthenticationException("Token header is invalid");
        }

        // Get the token from the header.
        String token = header.substring(prefix.length(), header.length());

        // Strip all non-alphanumeric characters
        return token.replaceAll("[^a-zA-Z0-9]", "");
    }
}