package hms.dbmi.ppm;

//#define gte_3_4_0 hapi_fhir_version_major>=4 || ( hapi_fhir_version_major==3 && hapi_fhir_version_minor>=4 )
//#define lt_3_4_0 !gte_3_4_0

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu2;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

//#if lt_3_4_0
//$import org.hibernate.jpa.HibernatePersistenceProvider;
//#endif

@Configuration
public class FhirServerConfigDstu2 extends BaseJavaConfigDstu2 {

    @Autowired
    private DataSource myDataSource;

    /**
     * We override the paging provider definition so that we can customize
     * the default/max page sizes for search results. You can set these however
     * you want, although very large page sizes will require a lot of RAM.
     */
    @Override
    public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
        DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
        pagingProvider.setDefaultPageSize(HapiProperties.getDefaultPageSize());
        pagingProvider.setMaximumPageSize(HapiProperties.getMaximumPageSize());
        return pagingProvider;
    }

    //#if gte_3_4_0
    @Override
    //#endif
    @Bean()
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        //#if gte_3_4_0
        LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
        //#else
        //$LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
        //$retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
        //$retVal.setPersistenceProvider(new HibernatePersistenceProvider());
        //#endif
        retVal.setPersistenceUnitName(HapiProperties.getPersistenceUnitName());

        try {
            retVal.setDataSource(myDataSource);
        } catch (Exception e) {
            throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
        }

        retVal.setJpaProperties(HapiProperties.getJpaProperties());
        return retVal;
    }

    @Bean()
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager retVal = new JpaTransactionManager();
        retVal.setEntityManagerFactory(entityManagerFactory);
        return retVal;
    }

}