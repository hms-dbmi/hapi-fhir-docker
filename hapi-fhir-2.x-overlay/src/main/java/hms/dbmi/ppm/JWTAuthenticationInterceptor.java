package hms.dbmi.ppm;

import hms.dbmi.ppm.TokenVerifier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.ToStringBuilder;

import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.method.RequestDetails;

public class JWTAuthenticationInterceptor extends InterceptorAdapter {

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
                TokenVerifier verifier = new TokenVerifier();
                String token = TokenVerifier.getTokenFromHeader(authHeader);
                return verifier.verify(token);

           } catch(Exception e) {
               System.out.println( "JWT Exception: " + e );
               throw new AuthenticationException("Invalid JWT");
           }
       }

       throw new AuthenticationException("Missing JWT");
    }
}