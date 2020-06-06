package hms.dbmi.ppm;

import hms.dbmi.ppm.JWTAuthenticationInterceptor;
import hms.dbmi.ppm.JWTAuthorizationInterceptor;
import hms.dbmi.ppm.MyLoggingInterceptor;

import ca.uhn.fhir.jpa.binstore.DatabaseBlobBinaryStorageSvcImpl;
import ca.uhn.fhir.jpa.binstore.IBinaryStorageSvc;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.dstu2.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thymeleaf.util.Validate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;

//#if hapi_fhir_version=="5.0.0"
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionDeliveryHandlerFactory;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.JavaMailEmailSender;
//#else
//$import ca.uhn.fhir.jpa.dao.DaoConfig;
//$import ca.uhn.fhir.jpa.subscription.module.channel.SubscriptionDeliveryHandlerFactory;
//$import ca.uhn.fhir.jpa.subscription.module.subscriber.email.IEmailSender;
//$import ca.uhn.fhir.jpa.subscription.module.subscriber.email.JavaMailEmailSender;
//#endif

/**
 * This is the primary configuration file for the example server
 */
@Configuration
@EnableTransactionManagement()
@PropertySource("classpath:hapi.properties")
public class FhirServerConfigCommon {

    @Autowired
    private ApplicationContext appContext;

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServerConfigCommon.class);

    private Boolean enableIndexMissingFields = HapiProperties.getEnableIndexMissingFields();
    private Boolean autoCreatePlaceholderReferenceTargets = HapiProperties.getAutoCreatePlaceholderReferenceTargets();
    private Boolean enforceReferentialIntegrityOnWrite = HapiProperties.getEnforceReferentialIntegrityOnWrite();
    private Boolean enforceReferentialIntegrityOnDelete = HapiProperties.getEnforceReferentialIntegrityOnDelete();
    private Boolean allowContainsSearches = HapiProperties.getAllowContainsSearches();
    private Boolean allowMultipleDelete = HapiProperties.getAllowMultipleDelete();
    private Boolean allowExternalReferences = HapiProperties.getAllowExternalReferences();
    private Boolean expungeEnabled = HapiProperties.getExpungeEnabled();
    private Boolean allowPlaceholderReferences = HapiProperties.getAllowPlaceholderReferences();
    private Boolean subscriptionRestHookEnabled = HapiProperties.getSubscriptionRestHookEnabled();
    private Boolean subscriptionEmailEnabled = HapiProperties.getSubscriptionEmailEnabled();
    private Boolean allowOverrideDefaultSearchParams = HapiProperties.getAllowOverrideDefaultSearchParams();
    private String emailFrom = HapiProperties.getEmailFrom();
    private Boolean emailEnabled = HapiProperties.getEmailEnabled();
    private String emailHost = HapiProperties.getEmailHost();
    private Integer emailPort = HapiProperties.getEmailPort();
    private String emailUsername = HapiProperties.getEmailUsername();
    private String emailPassword = HapiProperties.getEmailPassword();
    private Boolean emailAuth = HapiProperties.getEmailAuth();
    private Boolean emailStartTlsEnable = HapiProperties.getEmailStartTlsEnable();
    private Boolean emailStartTlsRequired = HapiProperties.getEmailStartTlsRequired();
    private Boolean emailQuitWait = HapiProperties.getEmailQuitWait();

    //#if hapi_fhir_version=="5.0.0"
    @Autowired
    private ApplicationContext myAppCtx;
    //#else
    @Autowired
    private SubscriptionDeliveryHandlerFactory subscriptionDeliveryHandlerFactory;
    //#endif

    public FhirServerConfigCommon() {
        ourLog.info("Server configured to " + (this.allowContainsSearches ? "allow" : "deny") + " contains searches");
        ourLog.info("Server configured to " + (this.allowMultipleDelete ? "allow" : "deny") + " multiple deletes");
        ourLog.info("Server configured to " + (this.allowExternalReferences ? "allow" : "deny") + " external references");
        ourLog.info("Server configured to " + (this.expungeEnabled ? "enable" : "disable") + " expunges");
        ourLog.info("Server configured to " + (this.allowPlaceholderReferences ? "allow" : "deny") + " placeholder references");
        ourLog.info("Server configured to " + (this.allowOverrideDefaultSearchParams ? "allow" : "deny") + " overriding default search params");

        if (this.emailEnabled) {
            ourLog.info("Server is configured to enable email with host '" + this.emailHost + "' and port " + this.emailPort.toString());
            ourLog.info("Server will use '" + this.emailFrom + "' as the from email address");

            if (this.emailUsername != null && this.emailUsername.length() > 0) {
                ourLog.info("Server is configured to use username '" + this.emailUsername + "' for email");
            }

            if (this.emailPassword != null && this.emailPassword.length() > 0) {
                ourLog.info("Server is configured to use a password for email");
            }
        }

        if (this.subscriptionRestHookEnabled) {
            ourLog.info("REST-hook subscriptions enabled");
        }

        if (this.subscriptionEmailEnabled) {
            ourLog.info("Email subscriptions enabled");
        }
    }

    /**
     * Configure FHIR properties around the the JPA server via this bean
     */
    @Bean()
    public DaoConfig daoConfig() {
        DaoConfig retVal = new DaoConfig();

        retVal.setIndexMissingFields(this.enableIndexMissingFields ? DaoConfig.IndexEnabledEnum.ENABLED : DaoConfig.IndexEnabledEnum.DISABLED);
        retVal.setAutoCreatePlaceholderReferenceTargets(this.autoCreatePlaceholderReferenceTargets);
        retVal.setEnforceReferentialIntegrityOnWrite(this.enforceReferentialIntegrityOnWrite);
        retVal.setEnforceReferentialIntegrityOnDelete(this.enforceReferentialIntegrityOnDelete);
        retVal.setAllowContainsSearches(this.allowContainsSearches);
        retVal.setAllowMultipleDelete(this.allowMultipleDelete);
        retVal.setAllowExternalReferences(this.allowExternalReferences);
        retVal.setExpungeEnabled(this.expungeEnabled);
        retVal.setAutoCreatePlaceholderReferenceTargets(this.allowPlaceholderReferences);
        retVal.setEmailFromAddress(this.emailFrom);

        Integer maxFetchSize = HapiProperties.getMaximumFetchSize();
        retVal.setFetchSizeDefaultMaximum(maxFetchSize);
        ourLog.info("Server configured to have a maximum fetch size of " + (maxFetchSize == Integer.MAX_VALUE ? "'unlimited'" : maxFetchSize));

        Long reuseCachedSearchResultsMillis = HapiProperties.getReuseCachedSearchResultsMillis();
        retVal.setReuseCachedSearchResultsForMillis(reuseCachedSearchResultsMillis);
        ourLog.info("Server configured to cache search results for {} milliseconds", reuseCachedSearchResultsMillis);

        Long retainCachedSearchesMinutes = HapiProperties.getExpireSearchResultsAfterMins();
        retVal.setExpireSearchResultsAfterMillis(retainCachedSearchesMinutes * 60 * 1000);

        // Subscriptions are enabled by channel type
        if (HapiProperties.getSubscriptionRestHookEnabled()) {
            ourLog.info("Enabling REST-hook subscriptions");
            retVal.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.RESTHOOK);
        }
        if (HapiProperties.getSubscriptionEmailEnabled()) {
            ourLog.info("Enabling email subscriptions");
            retVal.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.EMAIL);
        }
        if (HapiProperties.getSubscriptionWebsocketEnabled()) {
            ourLog.info("Enabling websocket subscriptions");
            retVal.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.WEBSOCKET);
        }

        retVal.setFilterParameterEnabled(HapiProperties.getFilterSearchEnabled());

        return retVal;
    }

    @Bean
    public ModelConfig modelConfig() {
        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setAllowContainsSearches(this.allowContainsSearches);
        modelConfig.setAllowExternalReferences(this.allowExternalReferences);
        modelConfig.setDefaultSearchParamsCanBeOverridden(this.allowOverrideDefaultSearchParams);
        modelConfig.setEmailFromAddress(this.emailFrom);

        // You can enable these if you want to support Subscriptions from your server
        if (this.subscriptionRestHookEnabled) {
            modelConfig.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.RESTHOOK);
        }

        if (this.subscriptionEmailEnabled) {
            modelConfig.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.EMAIL);
        }

        return modelConfig;
    }

    /**
     * The following bean configures the database connection. The 'url' property value of "jdbc:derby:directory:jpaserver_derby_files;create=true" indicates that the server should save resources in a
     * directory called "jpaserver_derby_files".
     * <p>
     * A URL to a remote database could also be placed here, along with login credentials and other properties supported by BasicDataSource.
     */
    @Bean(destroyMethod = "close")
    public BasicDataSource dataSource() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        BasicDataSource retVal = new BasicDataSource();
        Driver driver = (Driver) Class.forName(HapiProperties.getDataSourceDriver()).getConstructor().newInstance();
        retVal.setDriver(driver);
        retVal.setUrl(HapiProperties.getDataSourceUrl());
        retVal.setUsername(HapiProperties.getDataSourceUsername());
        retVal.setPassword(HapiProperties.getDataSourcePassword());
        retVal.setMaxTotal(HapiProperties.getDataSourceMaxPoolSize());
        return retVal;
    }

    @Lazy
    @Bean
    public IBinaryStorageSvc binaryStorageSvc() {
        DatabaseBlobBinaryStorageSvcImpl binaryStorageSvc = new DatabaseBlobBinaryStorageSvcImpl();

        if (HapiProperties.getMaxBinarySize() != null) {
            binaryStorageSvc.setMaximumBinarySize(HapiProperties.getMaxBinarySize());
        }

        return binaryStorageSvc;
    }

    //#if hapi_fhir_version=="5.0.0"
    @Bean
    public PartitionSettings partitionSettings() {
        return new PartitionSettings();
    }
    //#endif

    @Bean()
    public IEmailSender emailSender() {
        if (this.emailEnabled) {
            JavaMailEmailSender retVal = new JavaMailEmailSender();

            retVal.setSmtpServerHostname(this.emailHost);
            retVal.setSmtpServerPort(this.emailPort);
            retVal.setSmtpServerUsername(this.emailUsername);
            retVal.setSmtpServerPassword(this.emailPassword);
            // TODO KHS add these when HAPI 4.2.0 is released
            //retVal.setAuth(this.emailAuth);
            //retVal.setStartTlsEnable(this.emailStartTlsEnable);
            //retVal.setStartTlsRequired(this.emailStartTlsRequired);
            //retVal.setQuitWait(this.emailQuitWait);

            //#if hapi_fhir_version=="5.0.0"
            SubscriptionDeliveryHandlerFactory subscriptionDeliveryHandlerFactory = myAppCtx.getBean(SubscriptionDeliveryHandlerFactory.class);
            //#endif
            Validate.notNull(subscriptionDeliveryHandlerFactory, "No subscription delivery handler");
            subscriptionDeliveryHandlerFactory.setEmailSender(retVal);


            return retVal;
        }

        return null;
    }

    @Bean(autowire = Autowire.BY_TYPE)
    @ConditionalOnProperty(name="auth", havingValue="jwt", matchIfMissing=false)
    public JWTAuthenticationInterceptor authenticationInterceptor() {
        System.out.println("------------------- JWT AuthN Enabled -------------------");
        JWTAuthenticationInterceptor retVal = new JWTAuthenticationInterceptor();
        return retVal;
    }

    @Bean(autowire = Autowire.BY_TYPE)
    @ConditionalOnProperty(name="auth", havingValue="jwt", matchIfMissing=false)
    public JWTAuthorizationInterceptor authorizationInterceptor() {
        System.out.println("------------------- JWT AuthZ Enabled -------------------");
        JWTAuthorizationInterceptor retVal = new JWTAuthorizationInterceptor(appContext);
        return retVal;
    }

    @Bean
    public HandlerExceptionResolver sentryExceptionResolver() {
        return new io.sentry.spring.SentryExceptionResolver();
    }

    @Bean
    public ServletContextInitializer sentryServletContextInitializer() {
        return new io.sentry.spring.SentryServletContextInitializer();
    }
}
