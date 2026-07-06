/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.persistence.FetchType;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
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
import jakarta.persistence.PersistenceUnitTransactionType;

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
	public void testHibernatePersistenceConfigurationSources() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-sources[]
		HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" );

		// add a class using JPA/Hibernate annotations for mapping
		configuration.managedClass(MyEntity.class);

		// add the name of a class using JPA/Hibernate annotations for mapping.
		// differs from above in that accessing the Class is deferred which is
		// important if using runtime bytecode-enhancement
		configuration.managedClassName("org.hibernate.example.Customer");

		// Read package-level metadata.
		configuration.packageName("hibernate.example");

		// Adds the named JPA orm.xml resource as a source: which performs the
		// classpath lookup and parses the XML
		configuration.mappingResource("org/hibernate/example/Product.orm.xml");
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-sources[]
	}

	@Test
	@Disabled
	public void testHibernatePersistenceConfigurationSourcesChaining() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-sources-chained[]
		HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" )
				.managedClass(MyEntity.class)
				.managedClassName("org.hibernate.example.Customer")
				.packageName("hibernate.example")
				.mappingResource("org/hibernate/example/Product.orm.xml");
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-sources-chained[]
	}

	@Test
	public void testBuildMetadataNoBuilder() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-createEntityManagerFactory[]
		SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
				.managedClass(MyEntity.class)
				.createEntityManagerFactory();
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-createEntityManagerFactory[]
		sessionFactory.close();
	}

	@Test
	public void testBuildMetadataUsingBuilder() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-with-settings[]
		SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
				.managedClass(MyEntity.class)
				// configure second-level caching
				.property( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" )
				// default catalog
				.defaultCatalog( "my_catalog" )
				// default schema
				.defaultSchema( "my_schema" )
				.createEntityManagerFactory();
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-with-settings[]
		sessionFactory.close();
	}

	@Test
	public void testBuildSessionFactoryNoBuilder() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-sessionFactory[]
		final SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
				.managedClass(MyEntity.class)
				.createEntityManagerFactory();
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-sessionFactory[]

		sessionFactory.close();
	}

	@Test
	public void testBuildSessionFactoryUsingSettings() {
	//tag::example-bootstrap-native-SessionFactory-with-settings[]
		final SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
				.managedClass(MyEntity.class)
				.property( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.createEntityManagerFactory();
	//end::example-bootstrap-native-SessionFactory-with-settings[]

		sessionFactory.close();
	}

	@Test
	public void testNativeBuilders() {
		//tag::example-bootstrap-native-HibernatePersistenceConfiguration[]
		HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" );
		// ...
		//end::example-bootstrap-native-HibernatePersistenceConfiguration[]

		configuration.managedClass(MyEntity.class);

		//tag::example-bootstrap-native-HibernatePersistenceConfiguration[]
		// configure second-level caching
		configuration.property( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );

		// default catalog
		configuration.defaultCatalog( "my_catalog" );

		// default schema
		configuration.defaultSchema( "my_schema" );
		//end::example-bootstrap-native-HibernatePersistenceConfiguration[]

		//tag::example-bootstrap-native-HibernatePersistenceConfiguration-build[]
		SessionFactory sessionFactory = configuration.createEntityManagerFactory();
		//end::example-bootstrap-native-HibernatePersistenceConfiguration-build[]

		sessionFactory.close();
	}


	@Test
	public void test_bootstrap_bootstrap_native_metadata_source_example() {
		try {
			{
				//tag::bootstrap-native-metadata-source-example[]
				HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" )
					.managedClass(MyEntity.class)
					.managedClassName("org.hibernate.example.Customer")
					.mappingResource("org/hibernate/example/Product.orm.xml");
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
				HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" );

				// Use the JPA-compliant implicit naming strategy
				configuration.property(
					AvailableSettings.IMPLICIT_NAMING_STRATEGY,
					"jpa");

				// specify the schema name to use for tables, etc when none is explicitly specified
				configuration.defaultSchema("my_default_schema");

				// specify a custom Attribute Converter
				configuration.managedClass(myAttributeConverter.getClass());

				SessionFactory sessionFactory = configuration.createEntityManagerFactory();
				sessionFactory.close();
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
				SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
					.property( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
					.managedClass(MyEntity.class)
					.managedClassName("org.hibernate.example.Customer")
					.mappingResource("org/hibernate/example/Product.orm.xml")
					.property(AvailableSettings.IMPLICIT_NAMING_STRATEGY, "jpa")
					.createEntityManagerFactory();

				sessionFactory.close();
				//end::bootstrap-native-SessionFactory-example[]
			}
			{
				//tag::bootstrap-native-SessionFactory-settings-example[]
				SessionFactory sessionFactory = new HibernatePersistenceConfiguration( "CRM" )
						.property( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
						.property( AvailableSettings.INTERCEPTOR, new CustomSessionFactoryInterceptor() )
						.property( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, getBeanManager() )
						.managedClass(MyEntity.class)
						.managedClassName("org.hibernate.example.Customer")
						.mappingResource("org/hibernate/example/Product.orm.xml")
						.property(AvailableSettings.IMPLICIT_NAMING_STRATEGY, "jpa")
						.createEntityManagerFactory();

				//end::bootstrap-native-SessionFactory-settings-example[]
				sessionFactory.close();
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

			EntityManagerFactory emf = BootstrapPipeline.build(
				new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
				integrationSettings
			);
			//end::bootstrap-native-EntityManagerFactory-example[]
			emf.close();
		}
		catch (Exception ignore) {
		}
	}

	@Test
	@JiraKey("HHH-17154")
	public void build_EntityManagerFactory_with_NewTempClassLoader() {
		try (var ignored = BootstrapPipeline.resolveMetadata(
				new PersistenceUnitInfoDescriptor(
						new PersistenceUnitInfoImpl( "", new ArrayList<>(), new Properties() ) {
							@Override
							public ClassLoader getNewTempClassLoader() {
								return Thread.currentThread().getContextClassLoader();
							}
						}
				),
				ServiceRegistryUtil.createBaseSettings()
		)) {
		}
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
			// It is a service, so we look it up using the service registry
			final EventListenerRegistry eventListenerRegistry = sessionFactory.getEventListenerRegistry();

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

	public static class CustomDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(@Nonnull Object listener, @Nonnull Object original) {
			return false;
		}

		@Override
		@Nonnull
		public Action getAction() {
			throw new UnsupportedOperationException();
		}
	}

	public static class CustomSessionFactoryInterceptor implements Interceptor {}

	public static class CustomSessionFactoryObserver implements SessionFactoryObserver {
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
		public jakarta.persistence.PersistenceUnitTransactionType getTransactionType() {
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
			return Collections.emptyList();
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
		public List<String> getAllClassNames() {
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

		@Override
		public FetchType getDefaultToOneFetchType() {
			return FetchType.EAGER;
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
}
