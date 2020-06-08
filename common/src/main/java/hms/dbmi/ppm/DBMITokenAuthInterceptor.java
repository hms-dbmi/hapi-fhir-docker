package hms.dbmi.ppm;

import org.apache.commons.lang3.Validate;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.*;
import ca.uhn.fhir.util.CoverageIgnore;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;


/**
 * Class that creates clients using DBMI JWT token authentication
 */
public class DBMITokenAuthInterceptor extends BearerTokenAuthInterceptor {

    private String tokenPrefix;
    private String myToken;

    /**
    * Constructor. If this constructor is used, a token must be supplied later
    */
    @CoverageIgnore
    public DBMITokenAuthInterceptor() {

        // Get the auth header prefix to use
        this.tokenPrefix = System.getenv("JWT_HEADER_PREFIX");
        Validate.notNull(this.tokenPrefix, "JWT_HEADER_PREFIX must be set in environment");
    }

    /**
    * Constructor
    *
    * @param theToken
    *           The bearer token to use (must not be null)
    */
    public DBMITokenAuthInterceptor(String theToken) {

        // Get the auth header prefix to use
        this.tokenPrefix = System.getenv("JWT_HEADER_PREFIX");
        Validate.notNull(this.tokenPrefix, "JWT_HEADER_PREFIX must be set in environment");

        Validate.notNull("theToken must not be null");
        this.myToken = theToken;
    }

    /**
    * Returns the bearer token to use
    */
    public String getToken() {
        return myToken;
    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        theRequest.addHeader(Constants.HEADER_AUTHORIZATION, (tokenPrefix + myToken));
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) {
        // nothing
    }

    /**
    * Sets the bearer token to use
    */
    public void setToken(String theToken) {
        myToken = theToken;
    }
}