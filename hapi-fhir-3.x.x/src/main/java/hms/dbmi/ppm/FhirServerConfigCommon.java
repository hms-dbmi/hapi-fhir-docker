package hms.dbmi.ppm;

import hms.dbmi.ppm.JWTAuthenticationInterceptor;
import hms.dbmi.ppm.JWTAuthorizationInterceptor;
import hms.dbmi.ppm.MyLoggingInterceptor;

import javax.persistence.EntityManagerFactory;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.instance.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.thymeleaf.util.Validate;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu3;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;

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

        retVal.setAllowContainsSearches(this.allowContainsSearches);
        retVal.setAllowMultipleDelete(this.allowMultipleDelete);
        retVal.setAllowExternalReferences(this.allowExternalReferences);
        retVal.setExpungeEnabled(this.expungeEnabled);
        retVal.setAutoCreatePlaceholderReferenceTargets(this.allowPlaceholderReferences);

        Integer maxFetchSize = HapiProperties.getMaximumFetchSize();
        retVal.setFetchSizeDefaultMaximum(maxFetchSize);
        ourLog.info("Server configured to have a maximum fetch size of " + (maxFetchSize == Integer.MAX_VALUE ? "'unlimited'" : maxFetchSize));

        Long reuseCachedSearchResultsMillis = HapiProperties.getReuseCachedSearchResultsMillis();
        retVal.setReuseCachedSearchResultsForMillis(reuseCachedSearchResultsMillis);
        ourLog.info("Server configured to cache search results for {} milliseconds", reuseCachedSearchResultsMillis);

        return retVal;
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

    /**
     * Do some fancy logging to create a nice access log that has details about each incoming request.
     */
    public IServerInterceptor loggingInterceptor() {
        MyLoggingInterceptor retVal = new MyLoggingInterceptor();
        retVal.setLoggerName("fhirtest.access");
        retVal.setMessageFormat(
                "Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
        retVal.setLogExceptions(true);
        retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
        return retVal;
    }

    /**
     * This interceptor adds some pretty syntax highlighting in responses when a browser is detected
     */
    @Bean(autowire = Autowire.BY_TYPE)
    public IServerInterceptor responseHighlighterInterceptor() {
        ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
        return retVal;
    }

    @Bean()
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager retVal = new JpaTransactionManager();
        retVal.setEntityManagerFactory(entityManagerFactory);
        return retVal;
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
