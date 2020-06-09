package hms.dbmi.ppm;

//#define lte_3_1_0 hapi_fhir_version_major<=2 || ( hapi_fhir_version_major==3 && hapi_fhir_version_minor<=1 )

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

public class HapiProperties {
    static final String AUTH = "auth";
    static final String ENABLE_INDEX_MISSING_FIELDS = "enable_index_missing_fields";
    static final String AUTO_CREATE_PLACEHOLDER_REFERENCE_TARGETS = "auto_create_placeholder_reference_targets";
    static final String ENFORCE_REFERENTIAL_INTEGRITY_ON_WRITE = "enforce_referential_integrity_on_write";
    static final String ENFORCE_REFERENTIAL_INTEGRITY_ON_DELETE = "enforce_referential_integrity_on_delete";
    static final String BINARY_STORAGE_ENABLED = "binary_storage.enabled";
    static final String PERSISTENCE_UNIT_NAME = "persistence_unit_name";
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
    static final String ALLOW_CASCADING_DELETES = "allow_cascading_deletes";
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
    static final String ALLOWED_BUNDLE_TYPES = "allowed_bundle_types";
    static final String TEST_PORT = "test.port";
    static final String TESTER_CONFIG_REFUSE_TO_FETCH_THIRD_PARTY_URLS = "tester.config.refuse_to_fetch_third_party_urls";
    static final String CORS_ENABLED = "cors.enabled";
    static final String CORS_ALLOWED_ORIGIN = "cors.allowed_origin";
    static final String CORS_ALLOW_CREDENTIALS = "cors.allowCredentials";
    static final String ALLOW_CONTAINS_SEARCHES = "allow_contains_searches";
    static final String ALLOW_OVERRIDE_DEFAULT_SEARCH_PARAMS = "allow_override_default_search_params";
    static final String EMAIL_FROM = "email.from";
    static final String EMAIL_ENABLED = "email.enabled";
    static final String EMAIL_HOST = "email.host";
    static final String EMAIL_PORT = "email.port";
    static final String EMAIL_USERNAME = "email.username";
    static final String EMAIL_PASSWORD = "email.password";
    static final String EMAIL_QUITWAIT = "email.quitwait";
    static final String EMAIL_STARTTLS_REQUIRED = "email.starttls.required";
    static final String EMAIL_STARTTLS_ENABLE = "email.starttls.enable";
    static final String EMAIL_AUTH = "email.auth";
    static final String VALIDATE_REQUESTS_ENABLED = "validation.requests.enabled";
    static final String VALIDATE_RESPONSES_ENABLED = "validation.responses.enabled";
    static final String FILTER_SEARCH_ENABLED = "filter_search.enabled";
    static final String GRAPHQL_ENABLED = "graphql.enabled";
    static final String BULK_EXPORT_ENABLED = "bulk.export.enabled";
    static final String EXPIRE_SEARCH_RESULTS_AFTER_MINS = "retain_cached_searches_mins";
    static final String MAX_BINARY_SIZE = "max_binary_size";
    private static Properties ourProperties;

    public static boolean isElasticSearchEnabled() {
        return HapiProperties.getPropertyBoolean("elasticsearch.enabled", false);
    }

    /*
     * Force the configuration to be reloaded
     */
    public static void forceReload() {
        ourProperties = null;
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

    public static Properties getJpaProperties() {
        Properties retVal = loadProperties();

        return retVal;
    }

    private static Properties getProperties() {
        if (ourProperties == null) {
            Properties properties = loadProperties();
            HapiProperties.ourProperties = properties;
        }

        return ourProperties;
    }

    @NotNull
    private static Properties loadProperties() {
        // Load the configurable properties file
        Properties properties;
        try (InputStream in = HapiProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)) {
            properties = new Properties();
            properties.load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load HAPI properties", e);
        }

        Properties overrideProps = loadOverrideProperties();
        if (overrideProps != null) {
            properties.putAll(overrideProps);
        }

        // If we're on HAPI-FHIR <= 3.1.0, remove hibernate.search.model_mapping property
        //#if lte_3_1_0
        //$properties.remove("hibernate.search.model_mapping");
        //#endif
        return properties;
    }

    /**
     * If a configuration file path is explicitly specified via -Dhapi.properties=<path>, the properties there will
     * be used to override the entries in the default hapi.properties file (currently under WEB-INF/classes)
     *
     * @return properties loaded from the explicitly specified configuraiton file if there is one, or null otherwise.
     */
    private static Properties loadOverrideProperties() {
        String confFile = System.getProperty(HAPI_PROPERTIES);
        if (confFile != null) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(confFile));
                return props;
            } catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties file: " + confFile, e);
            }
        }

        return null;
    }

    private static String getProperty(String propertyName) {
        String env = "HAPI_" + propertyName.toUpperCase(Locale.US);
        env = env.replace(".", "_");
        env = env.replace("-", "_");

        String propertyValue = System.getenv(env);
        if (propertyValue != null) {
            return propertyValue;
        }

        Properties properties = HapiProperties.getProperties();
        if (properties != null) {
            propertyValue = properties.getProperty(propertyName);
        }

        return propertyValue;
    }

    private static String getProperty(String propertyName, String defaultValue) {
        String value = getProperty(propertyName);

        if (value != null && value.length() > 0) {
            return value;
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

    private static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        return getBooleanProperty(propertyName, Boolean.valueOf(defaultValue));
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

    public static boolean isBinaryStorageEnabled() {
        return HapiProperties.getBooleanProperty(BINARY_STORAGE_ENABLED, true);
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
        return HapiProperties.getProperty(SERVER_ADDRESS);
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
        return HapiProperties.getProperty(DATASOURCE_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    }

    public static Integer getDataSourceMaxPoolSize() {
        return HapiProperties.getIntegerProperty(DATASOURCE_MAX_POOL_SIZE, 10);
    }

    public static String getDataSourceUrl() {
        return HapiProperties.getProperty(DATASOURCE_URL, "jdbc:derby:directory:target/jpaserver_derby_files;create=true");
    }

    public static String getDataSourceUsername() {
        return HapiProperties.getProperty(DATASOURCE_USERNAME);
    }

    public static String getDataSourcePassword() {
        return HapiProperties.getProperty(DATASOURCE_PASSWORD);
    }

    public static Boolean getAllowMultipleDelete() {
        return HapiProperties.getBooleanProperty(ALLOW_MULTIPLE_DELETE, false);
    }

    public static Boolean getAllowCascadingDeletes() {
        return HapiProperties.getBooleanProperty(ALLOW_CASCADING_DELETES, false);
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

    public static String getAllowedBundleTypes() {
        return HapiProperties.getProperty(ALLOWED_BUNDLE_TYPES, "");
    }

    @Nonnull
    public static Set<String> getSupportedResourceTypes() {
        String[] types = defaultString(getProperty("supported_resource_types")).split(",");
        return Arrays.stream(types)
                .map(t -> trim(t))
                .filter(t -> isNotBlank(t))
                .collect(Collectors.toSet());
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
        return HapiProperties.getBooleanProperty(EMAIL_ENABLED, false);
    }

    public static String getEmailHost() {
        return HapiProperties.getProperty(EMAIL_HOST);
    }

    public static Integer getEmailPort() {
        return HapiProperties.getIntegerProperty(EMAIL_PORT, 0);
    }

    public static String getEmailUsername() {
        return HapiProperties.getProperty(EMAIL_USERNAME);
    }

    public static String getEmailPassword() {
        return HapiProperties.getProperty(EMAIL_PASSWORD);
    }

    // Defaults from https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
    public static Boolean getEmailAuth() {
        return HapiProperties.getBooleanProperty(EMAIL_AUTH, false);
    }

    public static Boolean getEmailStartTlsEnable() {
        return HapiProperties.getBooleanProperty(EMAIL_STARTTLS_ENABLE, false);
    }

    public static Boolean getEmailStartTlsRequired() {
        return HapiProperties.getBooleanProperty(EMAIL_STARTTLS_REQUIRED, false);
    }

    public static Boolean getEmailQuitWait() {
        return HapiProperties.getBooleanProperty(EMAIL_QUITWAIT, true);
    }

    public static Long getReuseCachedSearchResultsMillis() {
        String value = HapiProperties.getProperty(REUSE_CACHED_SEARCH_RESULTS_MILLIS, "-1");
        return Long.valueOf(value);
    }

    public static Long getExpireSearchResultsAfterMins() {
        String value = HapiProperties.getProperty(EXPIRE_SEARCH_RESULTS_AFTER_MINS, "60");
        return Long.valueOf(value);
    }

    public static Boolean getCorsAllowedCredentials() {
        return HapiProperties.getBooleanProperty(CORS_ALLOW_CREDENTIALS, false);
    }

    public static boolean getValidateRequestsEnabled() {
        return HapiProperties.getBooleanProperty(VALIDATE_REQUESTS_ENABLED, false);
    }

    public static boolean getValidateResponsesEnabled() {
        return HapiProperties.getBooleanProperty(VALIDATE_RESPONSES_ENABLED, false);
    }

    public static boolean getFilterSearchEnabled() {
        return HapiProperties.getBooleanProperty(FILTER_SEARCH_ENABLED, true);
    }

    public static boolean getGraphqlEnabled() {
        return HapiProperties.getBooleanProperty(GRAPHQL_ENABLED, true);
    }

    public static boolean getEnforceReferentialIntegrityOnDelete() {
        return HapiProperties.getBooleanProperty(ENFORCE_REFERENTIAL_INTEGRITY_ON_DELETE, true);
    }

    public static boolean getEnforceReferentialIntegrityOnWrite() {
        return HapiProperties.getBooleanProperty(ENFORCE_REFERENTIAL_INTEGRITY_ON_WRITE, true);
    }

    public static boolean getAutoCreatePlaceholderReferenceTargets() {
        return HapiProperties.getBooleanProperty(AUTO_CREATE_PLACEHOLDER_REFERENCE_TARGETS, true);
    }

    public static boolean getEnableIndexMissingFields() {
        return HapiProperties.getBooleanProperty(ENABLE_INDEX_MISSING_FIELDS, false);
    }

    public static Integer getMaxBinarySize() {
        return getIntegerProperty(MAX_BINARY_SIZE, null);
    }

    private static boolean getPropertyBoolean(String thePropertyName, boolean theDefaultValue) {
        String value = getProperty(thePropertyName, Boolean.toString(theDefaultValue));
        return Boolean.parseBoolean(value);
    }

    private static <T extends Enum> T getPropertyEnum(String thePropertyName, Class<T> theEnumType, T theDefaultValue) {
        String value = getProperty(thePropertyName, theDefaultValue.name());
        return (T) Enum.valueOf(theEnumType, value);
    }

    public static boolean getBulkExportEnabled() {
        return HapiProperties.getBooleanProperty(BULK_EXPORT_ENABLED, true);
    }

    public static String getPersistenceUnitName() { return getProperty(PERSISTENCE_UNIT_NAME, "HAPI_PU"); }

    public static Boolean getJwtAuthEnabled() {
        String auth = HapiProperties.getProperty("auth", "open");

        // If set to JWT, then it's enabled
        return auth.toLowerCase().contains("jwt");
    }

    public static Boolean getTokenAuthEnabled() {
        String auth = HapiProperties.getProperty("auth", "open");

        // If set to JWT, then it's enabled
        return auth.toLowerCase().contains("token");
    }
}