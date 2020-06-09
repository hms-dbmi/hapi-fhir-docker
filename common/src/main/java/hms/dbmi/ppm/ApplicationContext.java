package hms.dbmi.ppm;
//#define gte_5_0_0 hapi_fhir_version_major>=5
//#define lt_5_0_0 hapi_fhir_version_major<5
//#define gte_4_0_0 hapi_fhir_version_major>=4
//#define lt_4_0_0 hapi_fhir_version_major<4
//#define gte_3_0_0 hapi_fhir_version_major>=3
//#define lt_3_0_0 hapi_fhir_version_major<3
//#define gte_2_0_0 hapi_fhir_version_major>=2
//#define lt_2_0_0 hapi_fhir_version_major<2

//#define eq_5 hapi_fhir_version_major==5
//#define eq_4 hapi_fhir_version_major==4
//#define eq_3 hapi_fhir_version_major==3
//#define eq_2 hapi_fhir_version_major==2

//#define eq_3_8_0__4_0_0 hapi_fhir_version_major==3 && hapi_fhir_version_minor>=8
//#define gte_3_8_0 hapi_fhir_version_major>=4 || ( hapi_fhir_version_major==3 && hapi_fhir_version_minor>=8 )
//#define eq_3_7_0 hapi_fhir_version_major==3 && hapi_fhir_version_minor==7
//#define in_3_0_0__3_7_0 hapi_fhir_version_major==3 && hapi_fhir_version_minor<=6

import ca.uhn.fhir.context.FhirVersionEnum;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

//#if eq_5
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
//#endif

//#if eq_4
//$import ca.uhn.fhir.jpa.config.WebsocketDispatcherConfig;
//#endif

//#if eq_3
//$import ca.uhn.fhir.jpa.config.WebsocketDispatcherConfig;
//#endif

//#if eq_2
//$import ca.uhn.fhir.jpa.config.WebsocketDispatcherConfig;
//#endif

public class ApplicationContext extends AnnotationConfigWebApplicationContext {

    public ApplicationContext() {
        FhirVersionEnum fhirVersion = HapiProperties.getFhirVersion();
        if (fhirVersion == FhirVersionEnum.DSTU2) {
            register(FhirServerConfigDstu2.class, FhirServerConfigCommon.class);
        } else if (fhirVersion == FhirVersionEnum.DSTU3) {
            register(FhirServerConfigDstu3.class, FhirServerConfigCommon.class);
        } else if (fhirVersion == FhirVersionEnum.R4) {
            register(FhirServerConfigR4.class, FhirServerConfigCommon.class);
        //#if gte_4_0_0
        } else if (fhirVersion == FhirVersionEnum.R5) {
            register(FhirServerConfigR5.class, FhirServerConfigCommon.class);
        //#endif
        } else {
            throw new IllegalStateException();
        }

        if (HapiProperties.getSubscriptionWebsocketEnabled()) {
            register(WebsocketDispatcherConfig.class);
        }

    }

}