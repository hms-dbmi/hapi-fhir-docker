package hms.dbmi.ppm;

import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;


public class MyLoggingInterceptor extends LoggingInterceptor {

    @Override
	public void processingCompletedNormally(ServletRequestDetails theRequestDetails) {

	    // Check for RESTful metadata operations and don't log it
        if (theRequestDetails.getRestOperationType() != null &&
            theRequestDetails.getRestOperationType().equals(RestOperationTypeEnum.METADATA)) {
            return;
        }

        // Handle it normally
        super.processingCompletedNormally(theRequestDetails);
	}
}