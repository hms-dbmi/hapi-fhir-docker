package hms.dbmi.ppm;

import hms.dbmi.ppm.TokenVerifier;
import hms.dbmi.ppm.DBMITokenAuthInterceptor;

import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Flag;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import ca.uhn.fhir.jpa.rp.dstu3.PatientResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;

@SuppressWarnings("ConstantConditions")
public class JWTAuthorizationInterceptor extends AuthorizationInterceptor {

    private ApplicationContext appContext;

    public JWTAuthorizationInterceptor(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) throws AuthenticationException {

        // Check for authorization
        String authHeader = theRequestDetails.getHeader("Authorization");
        if( authHeader != null ) {
            try {
                // Get the token
                String token = TokenVerifier.getTokenFromHeader(authHeader);

                // Verify it
                TokenVerifier verifier = new TokenVerifier();

                // Determine if the user is an admin
                if(verifier.isAdmin(token)) {
                    System.out.println("Admin: " + verifier.getEmail(token));

                    // Allow anything
                    return new RuleBuilder()
                            .allowAll().build();

                } else {

                    // Get their email and determine permissions for a user
                    String email = verifier.getEmail(token);
                    System.out.println("User: " + email);

                    // Get the Patient DAO
                    PatientResourceProvider patientResourceProvider = (PatientResourceProvider) appContext.getBean("myPatientRpDstu3");

                    // Query for a patient with that email.
                    SearchParameterMap paramMap = new SearchParameterMap();
                    TokenParam param = new TokenParam();
                    param.setSystem("http://schema.org/email");
                    param.setValue(email);
                    paramMap.add("identifier", param);
                    IBundleProvider bundle = patientResourceProvider.getDao().search(paramMap);
                    if (bundle.size() > 0) {

                        // Collect rules for each Patient
                        ArrayList<IAuthRule> rules = new ArrayList<>();
                        for(int i = 0; i < bundle.size(); i++) {

                            // Get the patient and add their ID
                            Patient patient = (Patient) bundle.getResources(0, bundle.size()).get(i);
                            IdDt patientId = new IdDt(patient.getId());
                            System.out.println("Found Patient for '" + email + "': " + patientId.toString());

                            // Add the rules
                            rules.addAll(
                                new RuleBuilder()
                                .allow("Allow user read " + patientId.toString()).read()
                                        .allResources().inCompartment("Patient", patientId).andThen()
                                .allow("Allow user write " + patientId.toString()).write()
                                        .allResources().inCompartment("Patient", patientId).andThen()
                                .build()
                            );
                        }

                        // Deny everything else but metadata
                        rules.addAll(new RuleBuilder().allow("Allow user metadata").metadata().andThen()
                                .denyAll("Deny user access").build());

                        // Allow them to update themselves and any attached component
                        return rules;
                    }

                    else {

                        // Patient does not exist, allow creation of Patient and Flag
                        return new RuleBuilder()
                                .allow("Allow user create Patient").write().resourcesOfType(Patient.class).withAnyId().andThen()
                                .allow("Allow user create Flag").write().resourcesOfType(Flag.class).withAnyId().andThen()
                                .allow("Allow user metadata").metadata().andThen()
                                .denyAll("Deny user access")
                                .build();
                    }
                }

            } catch(Exception e) {
                System.out.println( "JWT Exception: " + e );
            }
        }

        // User has not tried to authenticate, set rules
        return new RuleBuilder()
                .allow("Only allow anonymous metadata").metadata().build();
    }
}