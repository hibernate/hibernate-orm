/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.orm.test.mapping.basic.bitset.BitSetType;
import org.hibernate.orm.test.mapping.basic.bitset.BitSetUserType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Vlad Mihalcea
 */
public class BootstrapTest {

    @Test
    public void test_bootstrap_bootstrap_native_registry_BootstrapServiceRegistry_example() {

        ClassLoader customClassLoader = Thread.currentThread().getContextClassLoader();
        Integrator customIntegrator = new BeanValidationIntegrator();

        //tag::example-bootstrap-native-BootstrapServiceRegistry[]
        BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
            new BootstrapServiceRegistryBuilder();
        // add a custom ClassLoader
        bootstrapRegistryBuilder.applyClassLoader(customClassLoader);
        // manually add an Integrator
        bootstrapRegistryBuilder.applyIntegrator(customIntegrator);

        BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();
        //end::example-bootstrap-native-BootstrapServiceRegistry[]
    }

    @Test
    public void test_bootstrap_bootstrap_native_registry_StandardServiceRegistryBuilder_example_1() {
        //tag::example-bootstrap-native-StandardServiceRegistryBuilder[]
        // An example using an implicitly built BootstrapServiceRegistry
        StandardServiceRegistryBuilder standardRegistryBuilder =
            new StandardServiceRegistryBuilder();
        //end::example-bootstrap-native-StandardServiceRegistryBuilder[]
    }

    @Test
    public void test_bootstrap_bootstrap_native_registry_StandardServiceRegistryBuilder_example_2() {
        //tag::example-bootstrap-native-StandardServiceRegistryBuilder[]

        // An example using an explicitly built BootstrapServiceRegistry
        BootstrapServiceRegistry bootstrapRegistry =
            new BootstrapServiceRegistryBuilder().build();

        StandardServiceRegistryBuilder standardRegistryBuilder =
            new StandardServiceRegistryBuilder(bootstrapRegistry);
        //end::example-bootstrap-native-StandardServiceRegistryBuilder[]
    }

    @Test
    @Disabled
    public void testMetadataSources() {
        //tag::example-bootstrap-native-MetadataSources[]
        ServiceRegistry standardRegistry =
                new StandardServiceRegistryBuilder().build();

        MetadataSources sources = new MetadataSources(standardRegistry);

        // add a class using JPA/Hibernate annotations for mapping
        sources.addAnnotatedClass(MyEntity.class);

        // add the name of a class using JPA/Hibernate annotations for mapping.
        // differs from above in that accessing the Class is deferred which is
        // important if using runtime bytecode-enhancement
        sources.addAnnotatedClassName("org.hibernate.example.Customer");

        // Read package-level metadata.
        sources.addPackage("hibernate.example");

        // Adds the named JPA orm.xml resource as a source: which performs the
        // classpath lookup and parses the XML
        sources.addResource("org/hibernate/example/Product.orm.xml");
        //end::example-bootstrap-native-MetadataSources[]
    }

    @Test
    @Disabled
    public void testMetadataSourcesChaining() {
        //tag::example-bootstrap-native-MetadataSources-chained[]
        ServiceRegistry standardRegistry =
                new StandardServiceRegistryBuilder().build();

        MetadataSources sources = new MetadataSources(standardRegistry)
                .addAnnotatedClass(MyEntity.class)
                .addAnnotatedClassName("org.hibernate.example.Customer")
                .addPackage("hibernate.example")
                .addResource("org/hibernate/example/Product.orm.xml");
        //end::example-bootstrap-native-MetadataSources-chained[]
    }

    @Test
    public void testBuildMetadataNoBuilder() {
        ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder().build();
        MetadataSources sources = new MetadataSources(standardRegistry).addAnnotatedClass(MyEntity.class);

        //tag::example-bootstrap-native-Metadata-no-builder[]
        Metadata metadata = sources.buildMetadata();
        //end::example-bootstrap-native-Metadata-no-builder[]
    }

    @Test
    public void testBuildMetadataUsingBuilder() {
        ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder().build();
        MetadataSources sources = new MetadataSources(standardRegistry).addAnnotatedClass(MyEntity.class);

        //tag::example-bootstrap-native-Metadata-using-builder[]
        Metadata metadata = sources.getMetadataBuilder()
                // configure second-level caching
                .applyAccessType( AccessType.READ_WRITE )
                // default catalog
                .applyImplicitCatalogName( "my_catalog" )
                // default schema
                .applyImplicitSchemaName( "my_schema" )
                .build();
        //end::example-bootstrap-native-Metadata-using-builder[]
    }

    @Test
    public void testBuildSessionFactoryNoBuilder() {
        ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder().build();
        Metadata metadata = new MetadataSources(standardRegistry)
                .addAnnotatedClass(MyEntity.class)
                .buildMetadata();

        //tag::example-bootstrap-native-SessionFactory-no-builder[]
        final SessionFactory sessionFactory = metadata.buildSessionFactory();
        //end::example-bootstrap-native-SessionFactory-no-builder[]

        sessionFactory.close();
    }

    @Test
    public void testBuildSessionFactoryUsingBuilder() {
        ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder().build();
        Metadata metadata = new MetadataSources(standardRegistry)
                .addAnnotatedClass(MyEntity.class)
                .buildMetadata();

        //tag::example-bootstrap-native-SessionFactory-using-builder[]
        final SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
                .applyStatisticsSupport( true )
                .build();
        //end::example-bootstrap-native-SessionFactory-using-builder[]

        sessionFactory.close();
    }

    @Test
    public void testNativeBuilders() {
        //tag::example-bootstrap-native-MetadataBuilder[]
        ServiceRegistry standardRegistry =
                new StandardServiceRegistryBuilder().build();

        MetadataSources sources = new MetadataSources(standardRegistry);
        // ...
        //end::example-bootstrap-native-MetadataBuilder[]

        sources.addAnnotatedClass(MyEntity.class);

        //tag::example-bootstrap-native-MetadataBuilder[]
        final MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

        // configure second-level caching
        metadataBuilder.applyAccessType( AccessType.READ_WRITE );

        // default catalog
        metadataBuilder.applyImplicitCatalogName( "my_catalog" );

        // default schema
        metadataBuilder.applyImplicitSchemaName( "my_schema" );
        //end::example-bootstrap-native-MetadataBuilder[]

        //tag::example-bootstrap-native-Metadata[]
        Metadata metadata = metadataBuilder.build();
        // or Metadata metadata = sources.buildMetadata()
        //end::example-bootstrap-native-Metadata[]

        //tag::example-bootstrap-native-SessionFactoryBuilder[]
        SessionFactoryBuilder sessionFactoryBuilder
                = metadata.getSessionFactoryBuilder();

        // collect statistics
        sessionFactoryBuilder.applyStatisticsSupport( true );
        //end::example-bootstrap-native-SessionFactoryBuilder[]
    }

    @Test
    public void test_bootstrap_bootstrap_native_registry_MetadataSources_example() {

        try {
            //tag::bootstrap-bootstrap-native-registry-MetadataSources-example[]
            ServiceRegistry standardRegistry =
                    new StandardServiceRegistryBuilder().build();

            MetadataSources sources = new MetadataSources(standardRegistry);

            // alternatively, we can build the MetadataSources without passing
            // a service registry, in which case it will build a default
            // BootstrapServiceRegistry to use.  But the approach shown
            // above is preferred
            // MetadataSources sources = new MetadataSources();

            // add a class using JPA/Hibernate annotations for mapping
            sources.addAnnotatedClass(MyEntity.class);

            // add the name of a class using JPA/Hibernate annotations for mapping.
            // differs from above in that accessing the Class is deferred which is
            // important if using runtime bytecode-enhancement
            sources.addAnnotatedClassName("org.hibernate.example.Customer");

            // Read package-level metadata.
            sources.addPackage("hibernate.example");

            // Read package-level metadata.
            sources.addPackage(MyEntity.class.getPackage());

            // Adds the named hbm.xml resource as a source: which performs the
            // classpath lookup and parses the XML
            sources.addResource("org/hibernate/example/Order.hbm.xml");

            // Adds the named JPA orm.xml resource as a source: which performs the
            // classpath lookup and parses the XML
            sources.addResource("org/hibernate/example/Product.orm.xml");

            // Read all mapping documents from a directory tree.
            // Assumes that any file named *.hbm.xml is a mapping document.
            sources.addDirectory(new File("."));

            // Read mappings from a particular XML file
            sources.addFile(new File("./mapping.xml"));

            // Read all mappings from a jar file.
            // Assumes that any file named *.hbm.xml is a mapping document.
            sources.addJar(new File("./entities.jar"));
            //end::bootstrap-bootstrap-native-registry-MetadataSources-example[]
        }
        catch (Exception ignore) {

        }
    }


    @Test
    public void test_bootstrap_bootstrap_native_metadata_source_example() {
        try {
            {
                //tag::bootstrap-native-metadata-source-example[]
                ServiceRegistry standardRegistry =
                        new StandardServiceRegistryBuilder().build();

                MetadataSources sources = new MetadataSources(standardRegistry)
                    .addAnnotatedClass(MyEntity.class)
                    .addAnnotatedClassName("org.hibernate.example.Customer")
                    .addResource("org/hibernate/example/Order.hbm.xml")
                    .addResource("org/hibernate/example/Product.orm.xml");
                //end::bootstrap-native-metadata-source-example[]
            }

            {
                AttributeConverter myAttributeConverter = new AttributeConverter() {
                    @Override
                    public Object convertToDatabaseColumn(Object attribute) {
                        return null;
                    }

                    @Override
                    public Object convertToEntityAttribute(Object dbData) {
                        return null;
                    }
                } ;
                //tag::bootstrap-native-metadata-builder-example[]
                ServiceRegistry standardRegistry =
                    new StandardServiceRegistryBuilder().build();

                MetadataSources sources = new MetadataSources(standardRegistry);

                MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

                // Use the JPA-compliant implicit naming strategy
                metadataBuilder.applyImplicitNamingStrategy(
                    ImplicitNamingStrategyJpaCompliantImpl.INSTANCE);

                // specify the schema name to use for tables, etc when none is explicitly specified
                metadataBuilder.applyImplicitSchemaName("my_default_schema");

                // specify a custom Attribute Converter
                metadataBuilder.applyAttributeConverter(myAttributeConverter);

                Metadata metadata = metadataBuilder.build();
                //end::bootstrap-native-metadata-builder-example[]
            }
        }
        catch (Exception ignore) {

        }
    }

    @Test
    public void test_bootstrap_bootstrap_native_SessionFactory_example() {
        try {
            {
                //tag::bootstrap-native-SessionFactory-example[]
                StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                    .configure("org/hibernate/example/hibernate.cfg.xml")
                    .build();

                Metadata metadata = new MetadataSources(standardRegistry)
                    .addAnnotatedClass(MyEntity.class)
                    .addAnnotatedClassName("org.hibernate.example.Customer")
                    .addResource("org/hibernate/example/Order.hbm.xml")
                    .addResource("org/hibernate/example/Product.orm.xml")
                    .getMetadataBuilder()
                    .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
                    .build();

                SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
                    .applyBeanManager(getBeanManager())
                    .build();
                //end::bootstrap-native-SessionFactory-example[]
            }
            {
                //tag::bootstrap-native-SessionFactoryBuilder-example[]
                StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                        .configure("org/hibernate/example/hibernate.cfg.xml")
                        .build();

                Metadata metadata = new MetadataSources(standardRegistry)
                    .addAnnotatedClass(MyEntity.class)
                    .addAnnotatedClassName("org.hibernate.example.Customer")
                    .addResource("org/hibernate/example/Order.hbm.xml")
                    .addResource("org/hibernate/example/Product.orm.xml")
                    .getMetadataBuilder()
                    .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
                    .build();

                SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

                // Supply a SessionFactory-level Interceptor
                sessionFactoryBuilder.applyInterceptor(new CustomSessionFactoryInterceptor());

                // Add a custom observer
                sessionFactoryBuilder.addSessionFactoryObservers(new CustomSessionFactoryObserver());

                // Apply a CDI BeanManager (for JPA event listeners)
                sessionFactoryBuilder.applyBeanManager(getBeanManager());

                SessionFactory sessionFactory = sessionFactoryBuilder.build();
                //end::bootstrap-native-SessionFactoryBuilder-example[]
            }
        }
        catch (Exception ignore) {

        }
    }

    @Test
    public void test_bootstrap_bootstrap_jpa_compliant_EntityManagerFactory_example() {
        try {
            //tag::bootstrap-jpa-compliant-EntityManagerFactory-example[]
            // Create an EMF for our CRM persistence-unit.
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("CRM");
            //end::bootstrap-jpa-compliant-EntityManagerFactory-example[]
        } catch (Exception ignore) {}
    }

    @Test
    public void test_bootstrap_bootstrap_native_EntityManagerFactory_example() {

        try {
            //tag::bootstrap-native-EntityManagerFactory-example[]
            String persistenceUnitName = "CRM";
            List<String> entityClassNames = new ArrayList<>();
            Properties properties = new Properties();

            PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(
                persistenceUnitName,
                entityClassNames,
                properties
           );

            Map<String, Object> integrationSettings = ServiceRegistryUtil.createBaseSettings();
            integrationSettings.put(
                AvailableSettings.INTERCEPTOR,
                new CustomSessionFactoryInterceptor()
           );

            EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder =
                new EntityManagerFactoryBuilderImpl(
                    new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
                    integrationSettings
               );

            EntityManagerFactory emf = entityManagerFactoryBuilder.build();
            //end::bootstrap-native-EntityManagerFactory-example[]
        }
        catch (Exception ignore) {
        }
    }

    @Test
    @JiraKey("HHH-17154")
    public void build_EntityManagerFactory_with_NewTempClassLoader() {
        new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(
                        new PersistenceUnitInfoImpl( "", new ArrayList<>(), new Properties() ) {
                            @Override
                            public ClassLoader getNewTempClassLoader() {
                                return Thread.currentThread().getContextClassLoader();
                            }
                        }
                ),
				ServiceRegistryUtil.createBaseSettings()
        ).cancel();
    }

    public Object getBeanManager() {
        return null;
    }

    @Entity
    public static class MyEntity {
        @Id
        private Long id;
    }

    //tag::bootstrap-event-listener-registration-example[]
    public class MyIntegrator implements Integrator {

        @Override
        public void integrate(
                Metadata metadata,
                BootstrapContext bootstrapContext,
                SessionFactoryImplementor sessionFactory) {

            // As you might expect, an EventListenerRegistry is the thing with which event
            // listeners are registered
            // It is a service so we look it up using the service registry
            final EventListenerRegistry eventListenerRegistry =
                bootstrapContext.getServiceRegistry().getService(EventListenerRegistry.class);

            // If you wish to have custom determination and handling of "duplicate" listeners,
            // you would have to add an implementation of the
            // org.hibernate.event.service.spi.DuplicationStrategy contract like this
            eventListenerRegistry.addDuplicationStrategy(new CustomDuplicationStrategy());

            // EventListenerRegistry defines 3 ways to register listeners:

            // 1) This form overrides any existing registrations with
            eventListenerRegistry.setListeners(EventType.AUTO_FLUSH,
                                                DefaultAutoFlushEventListener.class);

            // 2) This form adds the specified listener(s) to the beginning of the listener chain
            eventListenerRegistry.prependListeners(EventType.PERSIST,
                                                    DefaultPersistEventListener.class);

            // 3) This form adds the specified listener(s) to the end of the listener chain
            eventListenerRegistry.appendListeners(EventType.MERGE,
                                                   DefaultMergeEventListener.class);
        }

        @Override
        public void disintegrate(
                SessionFactoryImplementor sessionFactory,
                SessionFactoryServiceRegistry serviceRegistry) {

        }
    }
    //end::bootstrap-event-listener-registration-example[]

    public class CustomDuplicationStrategy implements DuplicationStrategy {

        @Override
        public boolean areMatch(Object listener, Object original) {
            return false;
        }

        @Override
        public Action getAction() {
            return null;
        }
    }

    public class CustomSessionFactoryInterceptor implements Interceptor {}

    public class CustomSessionFactoryObserver implements SessionFactoryObserver {

        @Override
        public void sessionFactoryCreated(SessionFactory factory) {

        }

        @Override
        public void sessionFactoryClosed(SessionFactory factory) {

        }
    }

    //tag::bootstrap-jpa-compliant-PersistenceUnit-example[]
    @PersistenceUnit
    private EntityManagerFactory emf;
    //end::bootstrap-jpa-compliant-PersistenceUnit-example[]

    //tag::bootstrap-jpa-compliant-PersistenceUnit-configurable-example[]
    @PersistenceUnit(unitName="CRM")
    private EntityManagerFactory entityManagerFactory;
    //end::bootstrap-jpa-compliant-PersistenceUnit-configurable-example[]

    //tag::bootstrap-jpa-compliant-PersistenceContext-example[]
    @PersistenceContext
    private EntityManager em;
    //end::bootstrap-jpa-compliant-PersistenceContext-example[]

    //tag::bootstrap-jpa-compliant-PersistenceContext-configurable-example[]
    @PersistenceContext(
        unitName = "CRM",
        properties = {
            @PersistenceProperty(
                name="org.hibernate.flushMode",
                value= "MANUAL"
           )
        }
   )
    private EntityManager entityManager;
    //end::bootstrap-jpa-compliant-PersistenceContext-configurable-example[]

    //tag::bootstrap-native-PersistenceUnitInfoImpl-example[]
    public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

        private final String persistenceUnitName;

        private PersistenceUnitTransactionType transactionType =
                PersistenceUnitTransactionType.RESOURCE_LOCAL;

        private final List<String> managedClassNames;

        private final Properties properties;

        private DataSource jtaDataSource;

        private DataSource nonJtaDataSource;

        public PersistenceUnitInfoImpl(
                String persistenceUnitName,
                List<String> managedClassNames,
                Properties properties) {
            this.persistenceUnitName = persistenceUnitName;
            this.managedClassNames = managedClassNames;
            this.properties = properties;
        }

        @Override
        public String getPersistenceUnitName() {
            return persistenceUnitName;
        }

        @Override
        public String getPersistenceProviderClassName() {
            return HibernatePersistenceProvider.class.getName();
        }

        @Override
        public String getScopeAnnotationName() {
            return null;
        }

        @Override
        public List<String> getQualifierAnnotationNames() {
            return List.of();
        }

        @Override
        public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
            return transactionType;
        }

        @Override
        public DataSource getJtaDataSource() {
            return jtaDataSource;
        }

        public PersistenceUnitInfoImpl setJtaDataSource(DataSource jtaDataSource) {
            this.jtaDataSource = jtaDataSource;
            this.nonJtaDataSource = null;
            transactionType = PersistenceUnitTransactionType.JTA;
            return this;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return nonJtaDataSource;
        }

        public PersistenceUnitInfoImpl setNonJtaDataSource(DataSource nonJtaDataSource) {
            this.nonJtaDataSource = nonJtaDataSource;
            this.jtaDataSource = null;
            transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
            return this;
        }

        @Override
        public List<String> getMappingFileNames() {
            return null;
        }

        @Override
        public List<URL> getJarFileUrls() {
            return Collections.emptyList();
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return managedClassNames;
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return false;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return SharedCacheMode.UNSPECIFIED;
        }

        @Override
        public ValidationMode getValidationMode() {
            return ValidationMode.AUTO;
        }

        public Properties getProperties() {
            return properties;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return "2.1";
        }

        @Override
        public ClassLoader getClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {

        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return null;
        }
    }
    //end::bootstrap-native-PersistenceUnitInfoImpl-example[]

    @Test
    public void test_basic_custom_type_register_BasicType_example() {
        try {
            //tag::basic-custom-type-register-BasicType-example[]
            ServiceRegistry standardRegistry =
                    new StandardServiceRegistryBuilder().build();

            MetadataSources sources = new MetadataSources( standardRegistry );

            MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

            metadataBuilder.applyBasicType( BitSetType.INSTANCE );
            //end::basic-custom-type-register-BasicType-example[]
        }
        catch (Exception ignore) {

        }
    }

    @Test
    public void test_basic_custom_type_register_UserType_example() {
        try {
            //tag::basic-custom-type-register-UserType-example[]
            ServiceRegistry standardRegistry =
                new StandardServiceRegistryBuilder().build();

            MetadataSources sources = new MetadataSources(standardRegistry);

            MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

            metadataBuilder.applyBasicType(new BitSetUserType(), "bitset");
            //end::basic-custom-type-register-UserType-example[]
        }
        catch (Exception ignore) {

        }
    }
}
