package hms.dbmi.ppm;

import hms.dbmi.ppm.DBMITokenAuthInterceptor;
import hms.dbmi.ppm.TokenVerifier;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;
import ca.uhn.fhir.context.FhirContext;

//#if gte_3_0_0
import ca.uhn.fhir.rest.server.util.ITestingUiClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
//#else
//$import ca.uhn.fhir.util.ITestingUiClientFactory;
//$import ca.uhn.fhir.rest.client.IGenericClient;
//#endif

//@formatter:off
/**
 * This spring config file configures the web testing module. It serves two
 * purposes:
 * 1. It imports FhirTesterMvcConfig, which is the spring config for the
 *    tester itself
 * 2. It tells the tester which server(s) to talk to, via the testerConfig()
 *    method below
 */
@Configuration
@Import(FhirTesterMvcConfig.class)
public class FhirTesterConfig {

	/**
	 * This bean tells the testing webpage which servers it should configure itself
	 * to communicate with. In this example we configure it to talk to the local
	 * server, as well as one public server. If you are creating a project to
	 * deploy somewhere else, you might choose to only put your own server's
	 * address here.
	 *
	 * Note the use of the ${serverBase} variable below. This will be replaced with
	 * the base URL as reported by the server itself. Often for a simple Tomcat
	 * (or other container) installation, this will end up being something
	 * like "http://localhost:8080/hapi-fhir-jpaserver-starter". If you are
	 * deploying your server to a place with a fully qualified domain name,
	 * you might want to use that instead of using the variable.
	 */
	@Bean
	public TesterConfig testerConfig() {
		TesterConfig retVal = new TesterConfig();
		retVal
				.addServer()
				.withId(HapiProperties.getServerId())
				.withFhirVersion(HapiProperties.getFhirVersion())
				.withBaseUrl(HapiProperties.getServerAddress())
				.withName(HapiProperties.getServerName());
		//#if gte_3_0_0
		retVal.setRefuseToFetchThirdPartyUrls(HapiProperties.getTesterConfigRefustToFetchThirdPartyUrls());
		//#endif
		// Check if JWT authn/authz are enabled
		if(HapiProperties.getJwtAuthEnabled()) {
			System.out.println("------------------- JWT AuthN Enabled -------------------");

			// Add a client to take the JWT cookie token and put it into the request headers
			ITestingUiClientFactory clientFactory = new ITestingUiClientFactory() {

				@Override
				public IGenericClient newClient(FhirContext theFhirContext, HttpServletRequest theRequest, String theServerBaseUrl) {

					// Create a client
					IGenericClient client = theFhirContext.newRestfulGenericClient(theServerBaseUrl);

					// Fetch the token
					String token = TokenVerifier.getTokenFromCookie(theRequest);

					// Ensure it's not null
					if( token != null) {

						// Register an interceptor which adds the token as credentials
						client.registerInterceptor(new DBMITokenAuthInterceptor(token));
					}

					return client;
				}
			};
			retVal.setClientFactory(clientFactory);
		}

		return retVal;
	}

}
//@formatter:on
