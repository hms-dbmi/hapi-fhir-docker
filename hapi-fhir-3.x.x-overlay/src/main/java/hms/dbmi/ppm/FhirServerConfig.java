package hms.dbmi.ppm;

import hms.dbmi.ppm.JWTAuthenticationInterceptor;
import hms.dbmi.ppm.JWTAuthorizationInterceptor;
import hms.dbmi.ppm.MyLoggingInterceptor;

import java.sql.SQLException;
import java.util.Properties;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;
import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu3;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;


@Configuration
@EnableTransactionManagement()
public class FhirServerConfig extends BaseJavaConfigDstu3 {

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
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
	    try {
            LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
            retVal.setPersistenceUnitName("HAPI_PU");
            retVal.setDataSource(dataSource());
            retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
            retVal.setPersistenceProvider(new HibernatePersistenceProvider());
            retVal.setJpaProperties(jpaProperties());
            return retVal;
		} catch(SQLException e) {
		    return null;
		}
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
		extraProperties.put("hibernate.search.model_mapping", LuceneSearchMappingFactory.class.getName());
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
	public IServerInterceptor authenticationInterceptor() {
		JWTAuthenticationInterceptor retVal = new JWTAuthenticationInterceptor();
		return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor patientAndAdminAuthorizationInterceptor() {
		JWTAuthorizationInterceptor retVal = new JWTAuthorizationInterceptor();
		return retVal;
	}
}
