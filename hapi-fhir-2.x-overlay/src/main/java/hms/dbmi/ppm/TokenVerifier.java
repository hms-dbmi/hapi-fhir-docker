package hms.dbmi.ppm;

import java.util.*;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import java.security.interfaces.RSAKey;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;

/**
 * Class that verifies the signature of Auth0 issued id tokens.
 */
@SuppressWarnings("WeakerAccess")
class TokenVerifier {

    private final Algorithm algorithm;
    private final JwkProvider jwkProvider;
    private final String audience;
    private final String issuer;
    private JWTVerifier verifier;

    /**
     * Creates a new instance using the RS256 algorithm and issuer and audience specified in environment
     *
     * @throws UnsupportedEncodingException if the current environment doesn't support UTF-8 encoding.
     */
    public TokenVerifier() throws UnsupportedEncodingException {

        // Get the JWT authority details
        String issuer = System.getenv("JWT_ISSUER");
        Validate.notNull(issuer, "JWT_ISSUER must be set in environment");
        String audience = System.getenv("JWT_AUDIENCE");
        Validate.notNull(audience, "JWT_AUDIENCE must be set in environment");

        this.algorithm = null;
        this.audience = audience;
        this.issuer = toUrl(issuer);

        // Set the JWK provider
        UrlJwkProvider urlJwkProvider = new UrlJwkProvider(this.issuer);
        this.jwkProvider = new GuavaCachedJwkProvider(urlJwkProvider);
    }

    /**
     * Creates a new instance using the HS256 algorithm and the clientSecret as secret.
     *
     * @param clientSecret the Auth0 client secret to validate the signature with.
     * @param clientId     the Auth0 client id that this token is issued for.
     * @param domain       the Auth0 domain that issued this token.
     * @throws UnsupportedEncodingException if the current environment doesn't support UTF-8 encoding.
     */
    public TokenVerifier(String clientSecret, String clientId, String domain) throws UnsupportedEncodingException {
        Validate.notNull(clientSecret);
        Validate.notNull(clientId);
        Validate.notNull(domain);

        this.algorithm = Algorithm.HMAC256(clientSecret);
        this.jwkProvider = null;
        this.audience = clientId;
        this.issuer = toUrl(domain);
    }

    /**
     * Creates a new instance using the RS256 algorithm and the RSA key as secret.
     *
     * @param jwkProvider the JwkProvider of the key to validate the signature with.
     * @param clientId    the Auth0 client id that this token is issued for.
     * @param domain      the Auth0 domain that issued this token.
     */
    public TokenVerifier(JwkProvider jwkProvider, String clientId, String domain) {
        Validate.notNull(jwkProvider);
        Validate.notNull(clientId);
        Validate.notNull(domain);

        this.algorithm = null;
        this.jwkProvider = jwkProvider;
        this.audience = clientId;
        this.issuer = toUrl(domain);
    }

    /**
     * Creates a new instance using the RS256 algorithm and the RSA key as secret.
     *
     * @param clientId    the Auth0 client id that this token is issued for.
     * @param domain      the Auth0 domain that issued this token.
     */
    public TokenVerifier(String clientId, String domain) {
        Validate.notNull(clientId);
        Validate.notNull(domain);

        this.algorithm = null;
        this.audience = clientId;
        this.issuer = toUrl(domain);

        // Assume the provider is Auth0
        this.jwkProvider = new UrlJwkProvider(this.issuer);
    }

    private DecodedJWT verifyToken(String idToken) throws JwkException {
        if (verifier != null) {
            return verifier.verify(idToken);
        }

        if (algorithm != null) {
            verifier = JWT.require(algorithm)
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build();
            return verifier.verify(idToken);
        }

        String kid = JWT.decode(idToken).getKeyId();
        PublicKey publicKey = jwkProvider.get(kid).getPublicKey();
        return JWT.require(Algorithm.RSA256((RSAKey) publicKey))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
                .verify(idToken);
    }

    /**
     * Verify the JWT token and return the user's email if valid
     *
     * @param idToken the id token to verify
     * @return whether the token is valid or not
     * @throws JwkException             if the Public Key Certificate couldn't be obtained
     * @throws JWTVerificationException if the Id Token signature was invalid
     */
    public boolean verify(String idToken) throws JwkException, JWTVerificationException {
        Validate.notNull(idToken);

        // Decode the JWT, and throw an exception if invalid
        DecodedJWT jwt = verifyToken(idToken);

        return true;
    }

    /**
     * Verify that the idToken contains a claim 'nonce' with the exact given value.
     * If verification passes, the User Id ('sub' claim) is returned.
     *
     * @param idToken the id token to verify
     * @param nonce   the expected nonce value
     * @return the User Id contained in the token
     * @throws JwkException             if the Public Key Certificate couldn't be obtained
     * @throws JWTVerificationException if the Id Token signature was invalid
     */
    public String verifyNonce(String idToken, String nonce) throws JwkException, JWTVerificationException {
        Validate.notNull(idToken);
        Validate.notNull(nonce);

        DecodedJWT jwt = verifyToken(idToken);
        return nonce.equals(jwt.getClaim("nonce").asString()) ? jwt.getSubject() : null;
    }

    /**
     * Verify the token and retrieve the 'email' claim from the payload
     *
     * @param idToken the id token
     * @return the Email contained in the token
     * @throws JwkException             if the Public Key Certificate couldn't be obtained
     * @throws JWTVerificationException if the Id Token signature was invalid
     */
    public String getEmail(String idToken) throws JwkException, JWTVerificationException {
        Validate.notNull(idToken);

        // Decode the JWT and return the email claim
        DecodedJWT jwt = verifyToken(idToken);

        return jwt.getClaim("email").asString();
    }

    /**
     * Verify the token and retrieve the authorizations claim, if any, from the JWT. The key for the
     * authorizations claim must be specified in environment as 'JWT_AUTHZ_CLAIM'
     *
     * @param idToken the id token
     * @return the authorizations map contained in the token
     * @throws JwkException             if the Public Key Certificate couldn't be obtained
     * @throws JWTVerificationException if the Id Token signature was invalid
     */
    public Map<String, Object> getAuthorizations(String idToken) throws JwkException, JWTVerificationException {
        Validate.notNull(idToken);

        // Decode the JWT
        DecodedJWT jwt = verifyToken(idToken);

        // Get the JWT authority details
        String authzClaim = System.getenv("JWT_AUTHZ_CLAIM");
        Validate.notNull(authzClaim, "JWT_AUTHZ_CLAIM must be set in environment");

        try {

            // Ensure authorizations are included in the JWT
            if(!jwt.getClaim(authzClaim).isNull()) {

                // Get authorizations
                Map<String, Object> authorizations = jwt.getClaim(authzClaim).asMap();

                // Get the JWT admin group
                return authorizations;
            }
        }

        catch(Exception e) {
            System.out.println("JWT Authorization error:" + e);
        }

        return null;
    }

    /**
     * Verify the token and retrieve the authorizations claim, if any, from the JWT. The key for the
     * authorizations claim must be specified in environment as 'JWT_AUTHZ_CLAIM'. Checks the authorizations
     * map for membership in the admin group specified in environment as 'JWT_ADMIN_GROUP'.
     *
     * @param idToken the id token
     * @return whether the token's authorizations includes membership in the admin group or not
     * @throws JwkException             if the Public Key Certificate couldn't be obtained
     * @throws JWTVerificationException if the Id Token signature was invalid
     */
    public boolean isAdmin(String idToken) throws JwkException, JWTVerificationException {
        Validate.notNull(idToken);

        String adminGroup = System.getenv("JWT_ADMIN_GROUP");
        Validate.notNull(adminGroup, "JWT_ADMIN_GROUP must be set in environment");

        // Get the authorizations claim
        Map<String, Object> authorizations = getAuthorizations(idToken);
        if(authorizations != null) {
            try {
                // Ensure groups are added
                if (authorizations.get("groups") != null) {

                    // Get groups and check for the admin group
                    ArrayList<String> groups = (ArrayList<String>) authorizations.get("groups");

                    return groups.contains(adminGroup);
                }

            } catch (Exception e) {
                System.out.println("JWT Authorization error:" + e);
            }
        }

        return false;
    }

    private static String toUrl(String domain) {
        String url = domain;
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            url = "https://" + domain;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    public static String getTokenFromCookie(HttpServletRequest request) {

        // Get the cookie name to use
        String cookieName = System.getenv("JWT_COOKIE_NAME");
        Validate.notNull(cookieName, "JWT_COOKIE_NAME must be set in environment");

        // Get all cookies
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (int i = 0; i < cookies.length; i++) {

                // Check for it
                Cookie cookie = cookies[i];
                if (cookieName.equals(cookie.getName())) {

                    // Return the value
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public static String getTokenFromHeader(String header) {
        Validate.notNull(header, "Authorization header is null");

        // Get the auth header prefix to use
        String prefix = System.getenv("JWT_HEADER_PREFIX");
        Validate.notNull(prefix, "JWT_HEADER_PREFIX must be set in environment");

        // Ensure it ends with a space
        if (!prefix.endsWith(" ")) {
            prefix = prefix + " ";
        }

        // Ensure the token is in the string
        if (header.length() < prefix.length()) {
            throw new JWTVerificationException("JWT header is invalid");
        }

        // Get the token from the header.
        return header.substring(prefix.length(), header.length());
    }
}