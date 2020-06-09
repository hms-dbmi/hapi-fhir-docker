package hms.dbmi.ppm;

//#define gte_3_0_0 hapi_fhir_version_major>=3

import java.util.*;
import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.ToStringBuilder;

import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;

//#if gte_3_0_0
import ca.uhn.fhir.rest.api.server.RequestDetails;
//#else
//$import ca.uhn.fhir.rest.method.RequestDetails;
//#endif

public class TokenAuthenticationInterceptor extends InterceptorAdapter {

   @Override
   public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest, HttpServletResponse theResponse) throws AuthenticationException {

        // Don't worry about metadata requests
        if( theRequestDetails.getRestOperationType().equals(RestOperationTypeEnum.METADATA) ) {
            return true;
        }

       // Create the auth
       String authHeader = theRequestDetails.getHeader("Authorization");
       if( authHeader != null ) {
           try {
                // Validate and ensure a payload comes back
                String token = getTokenFromHeader(authHeader);
                if(validateToken(token)) {

                    // Allow process to finish normally
                    return true;
                }

           } catch(Exception e) {
               System.out.println( "Token Exception: " + e );
               throw new AuthenticationException("Invalid Token");
           }
       }

       throw new AuthenticationException("Missing Token");
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
        return header.substring(prefix.length(), header.length());
    }
}