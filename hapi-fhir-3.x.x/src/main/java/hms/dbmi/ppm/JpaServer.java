package hms.dbmi.ppm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.model.Meta;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.provider.SubscriptionTriggeringProvider;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.provider.r4.TerminologyUploaderProviderR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import javax.servlet.ServletException;
import java.util.Arrays;

public class JpaServer extends RestfulServer {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		/*
		 * Create a FhirContext object that uses the version of FHIR
		 * specified in the properties file.
		 */
		ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

		/*
		 * ResourceProviders are fetched from the Spring context
		 */
		FhirVersionEnum fhirVersion = HapiProperties.getFhirVersion();
		List<IResourceProvider> resourceProviders;
		Object systemProvider;
		if (fhirVersion == FhirVersionEnum.DSTU2) {
			resourceProviders = appCtx.getBean("myResourceProvidersDstu2", List.class);
			systemProvider = appCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class);
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {
			resourceProviders = appCtx.getBean("myResourceProvidersDstu3", List.class);
			systemProvider = appCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
		} else if (fhirVersion == FhirVersionEnum.R4) {
			resourceProviders = appCtx.getBean("myResourceProvidersR4", List.class);
			systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
		} else {
			throw new IllegalStateException();
		}

		setFhirContext(appCtx.getBean(FhirContext.class));

		setResourceProviders(resourceProviders);
		setPlainProviders(systemProvider);

		/*
		 * The conformance provider exports the supported resources, search parameters, etc for
		 * this server. The JPA version adds resourceProviders counts to the exported statement, so it
		 * is a nice addition.
		 *
		 * You can also create your own subclass of the conformance provider if you need to
		 * provide further customization of your server's CapabilityStatement
		 */
		if (fhirVersion == FhirVersionEnum.DSTU2) {
			IFhirSystemDao<ca.uhn.fhir.model.dstu2.resource.Bundle, ca.uhn.fhir.model.dstu2.composite.MetaDt> systemDao = appCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
			JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao, appCtx.getBean(DaoConfig.class));
			confProvider.setImplementationDescription("HAPI FHIR DSTU2 Server");
			setServerConformanceProvider(confProvider);
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {
			IFhirSystemDao<org.hl7.fhir.dstu3.model.Bundle, org.hl7.fhir.dstu3.model.Meta> systemDao = appCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
			JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, appCtx.getBean(DaoConfig.class));
			confProvider.setImplementationDescription("HAPI FHIR DSTU3 Server");
			setServerConformanceProvider(confProvider);
		} else if (fhirVersion == FhirVersionEnum.R4) {
			IFhirSystemDao<org.hl7.fhir.r4.model.Bundle, org.hl7.fhir.r4.model.Meta> systemDao = appCtx.getBean("mySystemDaoR4", IFhirSystemDao.class);
			JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(this, systemDao, appCtx.getBean(DaoConfig.class));
			confProvider.setImplementationDescription("HAPI FHIR R4 Server");
			setServerConformanceProvider(confProvider);
		} else {
			throw new IllegalStateException();
		}

		/*
		 * ETag Support
		 */
		setETagSupport(HapiProperties.getEtagSupport());

		/*
		 * This server tries to dynamically generate narratives
		 */
		FhirContext ctx = getFhirContext();
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

		/*
		 * Default to JSON and pretty printing
		 */
		setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

		/*
		 * Default encoding
		 */
		setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());

		/*
		 * This configures the server to page search results to and from
		 * the database, instead of only paging them to memory. This may mean
		 * a performance hit when performing searches that return lots of results,
		 * but makes the server much more scalable.
		 */
		setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

		/*
		 * Load interceptors for the server from Spring (these are defined in FhirServerConfig.java)
		 */
		Collection<IServerInterceptor> interceptorBeans = appCtx.getBeansOfType(IServerInterceptor.class).values();
		for (IServerInterceptor interceptor : interceptorBeans) {
			this.registerInterceptor(interceptor);
		}

		/*
		 * Add some logging for each request
		 */
		LoggingInterceptor loggingInterceptor = new MyLoggingInterceptor();
		loggingInterceptor.setLoggerName(HapiProperties.getLoggerName());
		loggingInterceptor.setMessageFormat(HapiProperties.getLoggerFormat());
		loggingInterceptor.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
		loggingInterceptor.setLogExceptions(HapiProperties.getLoggerLogExceptions());
		this.registerInterceptor(loggingInterceptor);

		/*
		 * If you are hosting this server at a specific DNS name, the server will try to
		 * figure out the FHIR base URL based on what the web container tells it, but
		 * this doesn't always work. If you are setting links in your search bundles that
		 * just refer to "localhost", you might want to use a server address strategy:
		 */
		String serverAddress = HapiProperties.getServerAddress();
		if (serverAddress != null && serverAddress.length() > 0) {
			setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
		}

    }
}
