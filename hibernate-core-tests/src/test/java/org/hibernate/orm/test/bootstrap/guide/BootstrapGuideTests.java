package org.hibernate.orm.test.bootstrap.guide;

import java.net.URLClassLoader;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManagementAction;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.cfg.StatisticsSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.tool.schema.Action;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Executable application bootstrap examples; injection fields are illustrative.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
public class BootstrapGuideTests {
	@Test
	void hibernateProgrammaticBootstrap() {
		//tag::bootstrap-guide-hibernate-configuration[]
		HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( "CRM" )
				.managedClasses( Document.class )
				.transactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL )
				.jdbcUrl( "jdbc:h2:mem:bootstrap_hibernate;DB_CLOSE_DELAY=-1" )
				.jdbcUsername( "sa" )
				.jdbcPassword( "" )
				.schemaToolingAction( Action.CREATE_DROP )
				.collectStatistics( true );

		try (SessionFactory factory = configuration.createEntityManagerFactory()) {
			persistDocument( factory );
			//tag::bootstrap-guide-assertions[]
			verifyDocument( factory );
			//end::bootstrap-guide-assertions[]
		}
		//end::bootstrap-guide-hibernate-configuration[]
	}

	@Test
	void standardProgrammaticBootstrap() {
		//tag::bootstrap-guide-standard-configuration[]
		PersistenceConfiguration configuration = new PersistenceConfiguration( "CRM" )
				.provider( HibernatePersistenceProvider.class.getName() )
				.managedClass( Document.class )
				.transactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL )
				.property( PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:bootstrap_standard;DB_CLOSE_DELAY=-1" )
				.property( PersistenceConfiguration.JDBC_USER, "sa" )
				.property( PersistenceConfiguration.JDBC_PASSWORD, "" )
				.schemaManagementDatabaseAction( SchemaManagementAction.DROP_AND_CREATE );

		try (EntityManagerFactory factory = configuration.createEntityManagerFactory()) {
			persistDocument( factory );
			//tag::bootstrap-guide-assertions[]
			verifyDocument( factory );
			//end::bootstrap-guide-assertions[]
		}
		//end::bootstrap-guide-standard-configuration[]
	}

	@Test
	void xmlBootstrap() throws Exception {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		var root = getClass().getResource( "unit/" );
		assertThat( root ).isNotNull();
		try (var loader = new URLClassLoader( new java.net.URL[] { root }, original )) {
			Thread.currentThread().setContextClassLoader( loader );
			//tag::bootstrap-guide-xml-configuration[]
			Map<String, Object> overrides = Map.of(
					PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:bootstrap_xml;DB_CLOSE_DELAY=-1"
			);
			try (EntityManagerFactory factory = Persistence.createEntityManagerFactory( "CRM", overrides )) {
				persistDocument( factory );
				//tag::bootstrap-guide-assertions[]
				verifyDocument( factory );
				try (var entityManager = factory.createEntityManager()) {
					entityManager.unwrap( org.hibernate.Session.class ).doWork( connection ->
							assertThat( connection.getMetaData().getURL() ).contains( "jdbc:h2:mem:bootstrap_xml" ) );
				}
				//end::bootstrap-guide-assertions[]
			}
			//end::bootstrap-guide-xml-configuration[]
		}
		finally {
			Thread.currentThread().setContextClassLoader( original );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void unitSettingOverridesControlledBaseline(boolean statisticsEnabled) {
		var configuration = new PersistenceConfiguration( "precedence" )
				.managedClass( Document.class )
				.property( JdbcSettings.URL, "jdbc:h2:mem:bootstrap_precedence" )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.property( StatisticsSettings.GENERATE_STATISTICS, statisticsEnabled );
		// Seed a conflicting baseline through the test hook; Environment is a process-wide snapshot.
		var builder = new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration ), Map.of(),
				baseline -> baseline.getConfigurationValues().put(
						StatisticsSettings.GENERATE_STATISTICS, !statisticsEnabled ) );
		try {
			try (var factory = builder.build()) {
				assertThat( factory.unwrap( SessionFactory.class ).getStatistics().isStatisticsEnabled() )
						.isEqualTo( statisticsEnabled );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void programmaticPropertyCallsMergeInOrder(boolean hibernateConfiguration) {
		PersistenceConfiguration configuration = hibernateConfiguration
				? new HibernatePersistenceConfiguration( "property-order" )
				: new PersistenceConfiguration( "property-order" );
		configuration.managedClass( Document.class )
				.property( JdbcSettings.URL, "jdbc:h2:mem:bootstrap_property_order" )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.property( StatisticsSettings.GENERATE_STATISTICS, true )
				.properties( Map.of( StatisticsSettings.GENERATE_STATISTICS, false ) );
		try (var factory = configuration.createEntityManagerFactory()) {
			assertThat( factory.unwrap( SessionFactory.class ).getStatistics().isStatisticsEnabled() ).isFalse();
		}
		configuration.property( StatisticsSettings.GENERATE_STATISTICS, true );
		try (var factory = configuration.createEntityManagerFactory()) {
			assertThat( factory.unwrap( SessionFactory.class ).getStatistics().isStatisticsEnabled() ).isTrue();
		}
	}

	@Test
	void configurationBootstrap() {
		//tag::bootstrap-guide-configuration[]
		Configuration configuration = new Configuration()
				.addAnnotatedClass( Document.class )
				.setProperty( JdbcSettings.URL, "jdbc:h2:mem:bootstrap_configuration" )
				.setProperty( JdbcSettings.USER, "sa" )
				.setProperty( JdbcSettings.PASS, "" )
				.setProperty( SchemaToolingSettings.HBM2DDL_AUTO, "create-drop" );

		try (SessionFactory factory = configuration.buildSessionFactory()) {
			persistDocument( factory );
			//tag::bootstrap-guide-assertions[]
			verifyDocument( factory );
			//end::bootstrap-guide-assertions[]
		}
		//end::bootstrap-guide-configuration[]
	}

	@Test
	void builderBootstrap() {
		//tag::bootstrap-guide-pipeline[]
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( JdbcSettings.URL, "jdbc:h2:mem:bootstrap_pipeline" )
				.applySetting( JdbcSettings.USER, "sa" )
				.applySetting( JdbcSettings.PASS, "" )
				.applySetting( SchemaToolingSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			Metadata metadata = new MetadataSources( registry )
					.addAnnotatedClass( Document.class )
					.buildMetadata();
			try (SessionFactory factory = metadata.buildSessionFactory()) {
				persistDocument( factory );
				//tag::bootstrap-guide-assertions[]
				verifyDocument( factory );
				//end::bootstrap-guide-assertions[]
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
		//end::bootstrap-guide-pipeline[]
	}

	@Test
	void failedResourceLocalOperationRollsBack() {
		var configuration = new HibernatePersistenceConfiguration( "rollback" )
				.managedClasses( Document.class )
				.jdbcUrl( "jdbc:h2:mem:bootstrap_rollback" )
				.jdbcUsername( "sa" )
				.jdbcPassword( "" )
				.schemaToolingAction( Action.CREATE_DROP );
		try (SessionFactory factory = configuration.createEntityManagerFactory()) {
			persistDocument( factory );
			assertThatThrownBy( () -> persistDocument( factory ) ).isInstanceOf( jakarta.persistence.PersistenceException.class );
			verifyDocument( factory );
		}
	}

	private static void verifyDocument(EntityManagerFactory factory) {
		try (var entityManager = factory.createEntityManager()) {
			assertThat( entityManager.find( Document.class, 1L ).getTitle() ).isEqualTo( "Hibernate User Guide" );
			assertThat( entityManager.createQuery( "select count(d) from Document d", Long.class ).getSingleResult() ).isEqualTo( 1L );
		}
	}

	//tag::bootstrap-guide-persist[]
	private static void persistDocument(EntityManagerFactory factory) {
		try (EntityManager entityManager = factory.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			try {
				transaction.begin();
				entityManager.persist( new Document( 1L, "Hibernate User Guide" ) );
				transaction.commit();
			}
			catch (RuntimeException failure) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw failure;
			}
		}
	}
	//end::bootstrap-guide-persist[]

	//tag::bootstrap-guide-inject-factory[]
	@PersistenceUnit(unitName = "CRM")
	EntityManagerFactory factory;
	//end::bootstrap-guide-inject-factory[]

	//tag::bootstrap-guide-inject-context[]
	@PersistenceContext(unitName = "CRM")
	EntityManager entityManager;
	//end::bootstrap-guide-inject-context[]

	//tag::bootstrap-guide-inject-default[]
	@PersistenceUnit
	EntityManagerFactory defaultFactory;
	//end::bootstrap-guide-inject-default[]
}
