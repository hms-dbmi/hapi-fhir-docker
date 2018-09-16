package hms.dbmi.ppm;

import hms.dbmi.ppm.JWTAuthenticationInterceptor;
import hms.dbmi.ppm.JWTAuthorizationInterceptor;
import hms.dbmi.ppm.MyLoggingInterceptor;

import java.sql.SQLException;
import java.util.Properties;
import java.util.List;
import java.lang.reflect.Method;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;

import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu3;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;


/**
 * This class isn't used by default by the example, but
 * you can use it as a config if you want to support DSTU3
 * instead of DSTU2 in your server.
 *
 * See https://github.com/jamesagnew/hapi-fhir/issues/278
 */
@Configuration
@EnableTransactionManagement()
@PropertySource("classpath:application.properties")
@PropertySource(value="file:/usr/local/tomcat/dbmi.application.properties", ignoreResourceNotFound=true)
public class FhirServerConfig extends BaseJavaConfigDstu3 {

	@Autowired
	private ApplicationContext appContext;

	/**
	 * Configure FHIR properties around the the JPA server via this bean
	 */
	@Bean()
	public DaoConfig daoConfig() {
		DaoConfig retVal = new DaoConfig();
		retVal.setSubscriptionEnabled(true);
		retVal.setSubscriptionPollDelay(5000);
		retVal.setSubscriptionPurgeInactiveAfterMillis(DateUtils.MILLIS_PER_HOUR);
		retVal.setAllowMultipleDelete(true);

		// Do not reuse searches (2.5+)
		try {
			Method method = retVal.getClass().getMethod("setReuseCachedSearchResultsForMillis", Long.class);
			method.invoke(retVal, null);
		}
		catch (Exception e) {
			System.out.println("DaoConfig.setReuseCachedSearchResultsForMillis() missing, must be HAPI-FHIR 2.4");
		}

		return retVal;
	}

	/**
	 * The following bean configures the database connection. The 'url' property value of "jdbc:derby:directory:jpaserver_derby_files;create=true" indicates that the server should save resources in a
	 * directory called "jpaserver_derby_files".
	 *
	 * A URL to a remote database could also be placed here, along with login credentials and other properties supported by BasicDataSource.
	 */
	@Bean(destroyMethod = "close")
	public DataSource dataSource() throws SQLException {
		BasicDataSource retVal = new BasicDataSource();
		retVal.setDriver(new com.mysql.jdbc.Driver());
		retVal.setUrl(System.getenv("FHIR_MYSQL_URL"));
		retVal.setUsername(System.getenv("FHIR_MYSQL_USERNAME"));
		retVal.setPassword(System.getenv("FHIR_MYSQL_PASSWORD"));
		return retVal;
	}

	@Bean()
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws SQLException {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		retVal.setPersistenceUnitName("HAPI_PU");
		retVal.setDataSource(dataSource());
		retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
		retVal.setPersistenceProvider(new HibernatePersistenceProvider());
		retVal.setJpaProperties(jpaProperties());
		return retVal;
	}

	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		extraProperties.put("hibernate.dialect", org.hibernate.dialect.MySQL5InnoDBDialect.class.getName());
		extraProperties.put("hibernate.format_sql", "true");
		extraProperties.put("hibernate.show_sql", "false");
		extraProperties.put("hibernate.hbm2ddl.auto", "update");
		extraProperties.put("hibernate.jdbc.batch_size", "20");
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");
		extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
		extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
		extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
		extraProperties.put("hibernate.search.default.worker.execution", "async");
		return extraProperties;
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

	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor subscriptionSecurityInterceptor() {
		SubscriptionsRequireManualActivationInterceptorDstu3 retVal = new SubscriptionsRequireManualActivationInterceptorDstu3();
		return retVal;
	}

	@Bean()
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	@ConditionalOnProperty(name="dbmi.jwt_auth_enabled", havingValue="true")
	public IServerInterceptor authenticationInterceptor() {
        System.out.println("------------------- JWT AuthN Enabled -------------------");
		JWTAuthenticationInterceptor retVal = new JWTAuthenticationInterceptor();
		return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	@ConditionalOnProperty(name="dbmi.jwt_auth_enabled", havingValue="true")
	public IServerInterceptor authorizationInterceptor() {
        System.out.println("------------------- JWT AuthZ Enabled -------------------");
		JWTAuthorizationInterceptor retVal = new JWTAuthorizationInterceptor(appContext);
		return retVal;
	}
}
