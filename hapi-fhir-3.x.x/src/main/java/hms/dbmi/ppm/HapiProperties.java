package hms.dbmi.ppm;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import com.google.common.annotations.VisibleForTesting;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;

public class HapiProperties {
    static final String ALLOW_EXTERNAL_REFERENCES = "allow_external_references";
    static final String ALLOW_MULTIPLE_DELETE = "allow_multiple_delete";
    static final String ALLOW_PLACEHOLDER_REFERENCES = "allow_placeholder_references";
    static final String REUSE_CACHED_SEARCH_RESULTS_MILLIS = "reuse_cached_search_results_millis";
    static final String DATASOURCE_DRIVER = "datasource.driver";
    static final String DATASOURCE_MAX_POOL_SIZE = "datasource.max_pool_size";
    static final String DATASOURCE_PASSWORD = "datasource.password";
    static final String DATASOURCE_URL = "datasource.url";
    static final String DATASOURCE_USERNAME = "datasource.username";
    static final String DEFAULT_ENCODING = "default_encoding";
    static final String DEFAULT_PAGE_SIZE = "default_page_size";
    static final String DEFAULT_PRETTY_PRINT = "default_pretty_print";
    static final String ETAG_SUPPORT = "etag_support";
    static final String FHIR_VERSION = "fhir_version";
    static final String HAPI_PROPERTIES = "hapi.properties";
    static final String LOGGER_ERROR_FORMAT = "logger.error_format";
    static final String LOGGER_FORMAT = "logger.format";
    static final String LOGGER_LOG_EXCEPTIONS = "logger.log_exceptions";
    static final String LOGGER_NAME = "logger.name";
    static final String MAX_FETCH_SIZE = "max_fetch_size";
    static final String MAX_PAGE_SIZE = "max_page_size";
    static final String SERVER_ADDRESS = "server_address";
    static final String SERVER_ID = "server.id";
    static final String SERVER_NAME = "server.name";
    static final String SUBSCRIPTION_EMAIL_ENABLED = "subscription.email.enabled";
    static final String SUBSCRIPTION_RESTHOOK_ENABLED = "subscription.resthook.enabled";
    static final String SUBSCRIPTION_WEBSOCKET_ENABLED = "subscription.websocket.enabled";
    static final String TEST_PORT = "test.port";
    static final String TESTER_CONFIG_REFUSE_TO_FETCH_THIRD_PARTY_URLS = "tester.config.refuse_to_fetch_third_party_urls";
    static final String CORS_ENABLED = "cors.enabled";
    static final String CORS_ALLOWED_ORIGIN = "cors.allowed_origin";
    static final String ALLOW_CONTAINS_SEARCHES = "allow_contains_searches";
    static final String ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS = "allow_override_default_search_params";
    static final String EMAIL_FROM = "email.from";

    private static Properties properties;

    /*
     * Force the configuration to be reloaded
     */
    public static void forceReload() {
        properties = null;
        getProperties();
    }

    /**
     * This is mostly here for unit tests. Use the actual properties file
     * to set values
     */
    @VisibleForTesting
    public static void setProperty(String theKey, String theValue) {
        getProperties().setProperty(theKey, theValue);
    }

    public static Properties getProperties() {
        if (properties == null) {
            // Load the configurable properties file
            try (InputStream in = HapiProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)){
                HapiProperties.properties = new Properties();
                HapiProperties.properties.load(in);
            } catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties", e);
            }

            Properties overrideProps = loadOverrideProperties();
            if(overrideProps != null) {
              properties.putAll(overrideProps);
            }
        }

        return properties;
    }

    /**
     * If a configuration file path is explicitly specified via -Dhapi.properties=<path>, the properties there will
     * be used to override the entries in the default hapi.properties file (currently under WEB-INF/classes)
     * @return properties loaded from the explicitly specified configuraiton file if there is one, or null otherwise.
     */
    private static Properties loadOverrideProperties() {
        String confFile = System.getProperty(HAPI_PROPERTIES);
        if(confFile != null) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(confFile));
                return props;
            }
            catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties file: " + confFile, e);
            }
        }

        return null;
    }

    private static String getProperty(String propertyName) {
        Properties properties = HapiProperties.getProperties();

        if (properties != null) {
            return properties.getProperty(propertyName);
        }

        return null;
    }

    private static String getProperty(String propertyName, String defaultValue) {
        Properties properties = HapiProperties.getProperties();

        if (properties != null) {
            String value = properties.getProperty(propertyName);

            if (value != null && value.length() > 0) {
                return value;
            }
        }

        return defaultValue;
    }

    private static Boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
        String value = HapiProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static Integer getIntegerProperty(String propertyName, Integer defaultValue) {
        String value = HapiProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    public static FhirVersionEnum getFhirVersion() {
        String fhirVersionString = HapiProperties.getProperty(FHIR_VERSION);

        if (fhirVersionString != null && fhirVersionString.length() > 0) {
            return FhirVersionEnum.valueOf(fhirVersionString);
        }

        return FhirVersionEnum.DSTU3;
    }

    public static ETagSupportEnum getEtagSupport() {
        String etagSupportString = HapiProperties.getProperty(ETAG_SUPPORT);

        if (etagSupportString != null && etagSupportString.length() > 0) {
            return ETagSupportEnum.valueOf(etagSupportString);
        }

        return ETagSupportEnum.ENABLED;
    }

    public static EncodingEnum getDefaultEncoding() {
        String defaultEncodingString = HapiProperties.getProperty(DEFAULT_ENCODING);

        if (defaultEncodingString != null && defaultEncodingString.length() > 0) {
            return EncodingEnum.valueOf(defaultEncodingString);
        }

        return EncodingEnum.JSON;
    }

    public static Boolean getDefaultPrettyPrint() {
        return HapiProperties.getBooleanProperty(DEFAULT_PRETTY_PRINT, true);
    }

    public static String getServerAddress() {
        return System.getenv("FHIR_SERVER_URL");
    }

    public static Integer getDefaultPageSize() {
        return HapiProperties.getIntegerProperty(DEFAULT_PAGE_SIZE, 20);
    }

    public static Integer getMaximumPageSize() {
        return HapiProperties.getIntegerProperty(MAX_PAGE_SIZE, 200);
    }

    public static Integer getMaximumFetchSize() {
        return HapiProperties.getIntegerProperty(MAX_FETCH_SIZE, Integer.MAX_VALUE);
    }

    public static String getLoggerName() {
        return HapiProperties.getProperty(LOGGER_NAME, "fhirtest.access");
    }

    public static String getLoggerFormat() {
        return HapiProperties.getProperty(LOGGER_FORMAT, "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
    }

    public static String getLoggerErrorFormat() {
        return HapiProperties.getProperty(LOGGER_ERROR_FORMAT, "ERROR - ${requestVerb} ${requestUrl}");
    }

    public static Boolean getLoggerLogExceptions() {
        return HapiProperties.getBooleanProperty(LOGGER_LOG_EXCEPTIONS, true);
    }

    public static String getDataSourceDriver() {
        return "com.mysql.jdbc.Driver";
    }

    public static Integer getDataSourceMaxPoolSize() {
        return HapiProperties.getIntegerProperty(DATASOURCE_MAX_POOL_SIZE, 10);
    }

    public static String getDataSourceUrl() {
        return System.getenv("FHIR_MYSQL_URL");
    }

    public static String getDataSourceUsername() {
        return System.getenv("FHIR_MYSQL_USERNAME");
    }

    public static String getDataSourcePassword() {
        return System.getenv("FHIR_MYSQL_PASSWORD");
    }

    public static Boolean getAllowMultipleDelete() {
        return HapiProperties.getBooleanProperty(ALLOW_MULTIPLE_DELETE, false);
    }

    public static Boolean getAllowExternalReferences() {
        return HapiProperties.getBooleanProperty(ALLOW_EXTERNAL_REFERENCES, false);
    }

    public static Boolean getExpungeEnabled() {
        return HapiProperties.getBooleanProperty("expunge_enabled", true);
    }

    public static Integer getTestPort() {
        return HapiProperties.getIntegerProperty(TEST_PORT, 0);
    }

    public static Boolean getTesterConfigRefustToFetchThirdPartyUrls() {
        return HapiProperties.getBooleanProperty(TESTER_CONFIG_REFUSE_TO_FETCH_THIRD_PARTY_URLS, false);
    }

    public static Boolean getCorsEnabled() {
        return HapiProperties.getBooleanProperty(CORS_ENABLED, true);
    }

    public static String getCorsAllowedOrigin() {
        return HapiProperties.getProperty(CORS_ALLOWED_ORIGIN, "*");
    }

    public static String getServerName() {
        return HapiProperties.getProperty(SERVER_NAME, "Local Tester");
    }

    public static String getServerId() {
        return HapiProperties.getProperty(SERVER_ID, "home");
    }

    public static Boolean getAllowPlaceholderReferences() {
        return HapiProperties.getBooleanProperty(ALLOW_PLACEHOLDER_REFERENCES, true);
    }

    public static Boolean getSubscriptionEmailEnabled() {
        return HapiProperties.getBooleanProperty(SUBSCRIPTION_EMAIL_ENABLED, false);
    }

    public static Boolean getSubscriptionRestHookEnabled() {
        return HapiProperties.getBooleanProperty(SUBSCRIPTION_RESTHOOK_ENABLED, false);
    }

    public static Boolean getSubscriptionWebsocketEnabled() {
        return HapiProperties.getBooleanProperty(SUBSCRIPTION_WEBSOCKET_ENABLED, false);
    }

    public static Boolean getAllowContainsSearches() {
        return HapiProperties.getBooleanProperty(ALLOW_CONTAINS_SEARCHES, true);
    }

    public static Boolean getAllowOverrideDefaultSearchParams() {
        return HapiProperties.getBooleanProperty(ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS, true);
    }

    public static String getEmailFrom() {
        return HapiProperties.getProperty(EMAIL_FROM, "some@test.com");
    }

    public static Boolean getEmailEnabled() {
        return HapiProperties.getBooleanProperty("email.enabled", false);
    }

    public static String getEmailHost() {
        return HapiProperties.getProperty("email.host");
    }

    public static Integer getEmailPort() {
        return HapiProperties.getIntegerProperty("email.port", 0);
    }

    public static String getEmailUsername() {
        return HapiProperties.getProperty("email.username");
    }

    public static String getEmailPassword() {
        return HapiProperties.getProperty("email.password");
    }

    public static Long getReuseCachedSearchResultsMillis() {
        String value = HapiProperties.getProperty(REUSE_CACHED_SEARCH_RESULTS_MILLIS, "-1");
        return Long.valueOf(value);
    }

    public static Boolean getJwtAuthenticationEnabled() {
        String auth = HapiProperties.getProperty("auth", "open");

        // If set to JWT, then it's enabled
        return auth.toLowerCase().equals("jwt");
    }
}
