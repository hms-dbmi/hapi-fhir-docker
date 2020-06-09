package hms.dbmi.ppm;

//#define gte_3_0_0 hapi_fhir_version_major>=3

import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;

//#if gte_3_0_0
import ca.uhn.fhir.rest.api.server.RequestDetails;
//#else
//$import ca.uhn.fhir.rest.method.RequestDetails;
//#endif

public class MyLoggingInterceptor extends LoggingInterceptor {

    @Override
	public void processingCompletedNormally(ServletRequestDetails theRequestDetails) {

	    // Check for RESTful metadata operations and don't log it
        if (theRequestDetails.getRestOperationType() != null &&
            theRequestDetails.getRestOperationType() == RestOperationTypeEnum.METADATA) {
            return;
        }

        // Handle it normally
        super.processingCompletedNormally(theRequestDetails);
	}
}