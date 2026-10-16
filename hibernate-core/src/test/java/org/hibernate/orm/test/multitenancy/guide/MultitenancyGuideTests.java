package org.hibernate.orm.test.multitenancy.guide;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.Session;
import org.hibernate.SessionCreationOption;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.TenantId;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.context.TenantIdentifierMismatchException;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.orm.test.multitenancy.ConfigurableMultiTenantConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.jpa.HibernateHints.HINT_TENANT_ID;

/// Isolated H2 examples for the Multitenancy chapter.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
public class MultitenancyGuideTests {
	@Test
	void tenantSelectionAndPartitionedData() {
		try ( var factory = configuration( Document.class ).createEntityManagerFactory() ) {
			inTenant( factory, "alpha", session -> session.persist( new Document( 1, "alpha document" ) ) );
			inTenant( factory, "beta", session -> session.persist( new Document( 2, "beta document" ) ) );
			// tag::multitenancy-explicit-session[]
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				assertThat( session.find( Document.class, 1 ) ).isNotNull();
				assertThat( session.find( Document.class, 2 ) ).isNull();
			}
			// end::multitenancy-explicit-session[]
			// tag::multitenancy-entity-manager[]
			try ( var entityManager = factory.createEntityManager( new SessionCreationOption.TenantId( "alpha" ) ) ) {
				assertThat( entityManager.find( Document.class, 2 ) ).isNull();
			}
			try ( var entityManager = factory.createEntityManager( Map.of( HINT_TENANT_ID, "beta" ) ) ) {
				assertThat( entityManager.find( Document.class, 1 ) ).isNull();
			}
			// end::multitenancy-entity-manager[]
			// tag::multitenancy-stateless[]
			try ( var session = factory.withStatelessOptions().tenantIdentifier( "alpha" ).openStatelessSession() ) {
				assertThat( session.createSelectionQuery( "from TenantDocument", Document.class ).getResultList() )
						.extracting( document -> document.title ).containsExactly( "alpha document" );
			}
			// end::multitenancy-stateless[]
			inTenant( factory, "alpha", session -> assertThat( session.find( Document.class, 1 ).tenant ).isEqualTo( "alpha" ) );
		}
	}

	@Test
	void resolverLifetimeAndCurrentSessionValidation() {
		var resolver = new TenantResolver();
		try ( var factory = configuration( Document.class )
				.property( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver )
				.property( "hibernate.current_session_context_class", "managed" )
				.createEntityManagerFactory() ) {
			// tag::multitenancy-resolver-lifetime[]
			resolver.currentTenant = "alpha";
			try ( var session = factory.openSession() ) {
				resolver.currentTenant = "beta";
				session.clear();
				assertThat( session.getTenantIdentifierValue() ).isEqualTo( "alpha" );
				try ( var next = factory.openSession() ) {
					assertThat( next.getTenantIdentifierValue() ).isEqualTo( "beta" );
				}
			}
			// end::multitenancy-resolver-lifetime[]
			resolver.currentTenant = "alpha";
			try ( var explicit = factory.withOptions().tenantIdentifier( "beta" ).openSession() ) {
				assertThat( explicit.getTenantIdentifierValue() ).isEqualTo( "beta" );
			}
			try ( var session = factory.openSession() ) {
				ManagedSessionContext.bind( session );
				try {
					resolver.currentTenant = "beta";
					assertThatThrownBy( factory::getCurrentSession ).isInstanceOf( TenantIdentifierMismatchException.class );
				}
				finally {
					ManagedSessionContext.unbind( factory );
				}
			}
		}
	}

	@Test
	void rootAndAssignmentValidation() {
		var resolver = new TenantResolver();
		try ( var factory = configuration( Document.class )
				.property( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver ).createEntityManagerFactory() ) {
			inTenant( factory, "alpha", session -> session.persist( new Document( 1, "alpha document" ) ) );
			inTenant( factory, "beta", session -> session.persist( new Document( 2, "beta document" ) ) );
			inTenant( factory, "alpha", session -> {
				var document = new Document( 3, "mismatched" );
				document.tenant = "beta";
				assertThatThrownBy( () -> {
					session.persist( document );
					session.flush();
				} ).hasMessageContaining( "assigned tenant id differs" );
				session.getTransaction().rollback();
			} );
			// tag::multitenancy-root[]
			try ( var session = factory.withOptions().tenantIdentifier( "root" ).openSession() ) {
				assertThat( session.createSelectionQuery( "from TenantDocument", Document.class ).getResultList() )
						.hasSize( 2 );
				assertThat( session.find( Document.class, 2 ) ).isNotNull();
			}
			// end::multitenancy-root[]
		}
	}

	@Test
	void tenantInclusiveIdentifiers() {
		try ( var factory = configuration( LocalDocument.class ).createEntityManagerFactory() ) {
			inTenant( factory, "alpha", session -> session.persist( new LocalDocument( 1, "alpha" ) ) );
			inTenant( factory, "beta", session -> session.persist( new LocalDocument( 1, "beta" ) ) );
			inTenant( factory, "alpha", session -> {
				assertThat( session.find( LocalDocument.class, new LocalKey( 1, "alpha" ) ).title ).isEqualTo( "alpha" );
				assertThat( session.createSelectionQuery( "from LocalDocument", LocalDocument.class ).getResultList() ).hasSize( 1 );
			} );
		}
	}

	@Test
	void schemaMappingAndProvisioning() throws Exception {
		try ( var connection = DriverManager.getConnection( url(), "sa", "" ) ) {
			var provider = new BorrowedConnectionProvider( connection );
			String originalSchema = connection.getSchema();
			try ( var statement = connection.createStatement() ) {
				statement.executeUpdate( "create schema ALPHA" );
				statement.executeUpdate( "create schema BETA" );
			}
			// tag::multitenancy-schema-configuration[]
			TenantSchemaMapper<String> schemas = tenant -> switch ( tenant ) {
				case "alpha" -> "ALPHA";
				case "beta" -> "BETA";
				default -> throw new IllegalArgumentException( "Unknown tenant: " + tenant );
			};
			var schemaConfiguration = new HibernatePersistenceConfiguration( "tenant-schemas" )
					.managedClasses( SchemaDocument.class )
					.property( "hibernate.connection.provider_class", provider )
					.property( "hibernate.multi_tenant.schema_mapper", schemas )
					.property( "hibernate.hbm2ddl.auto", "none" );
			// end::multitenancy-schema-configuration[]
			try ( var factory = schemaConfiguration.createEntityManagerFactory() ) {
				// tag::multitenancy-schema-provisioning[]
				// The application has already created ALPHA and BETA in this H2 database.
				for ( String schema : List.of( "ALPHA", "BETA" ) ) {
					factory.getSchemaManager().forSchema( schema ).create( false );
				}
				// end::multitenancy-schema-provisioning[]
				try {
					// tag::multitenancy-schema-use[]
					inTenant( factory, "alpha", session -> {
						session.persist( new SchemaDocument( 1, "alpha" ) );
						session.doWork( c -> assertThat( c.getSchema() ).isEqualTo( "ALPHA" ) );
					} );
					assertThat( connection.getSchema() ).isEqualTo( originalSchema );
					inTenant( factory, "beta", session -> {
						assertThat( session.find( SchemaDocument.class, 1 ) ).isNull();
						session.persist( new SchemaDocument( 1, "beta" ) );
					} );
					assertThat( connection.getSchema() ).isEqualTo( originalSchema );
					// end::multitenancy-schema-use[]
					inTenant( factory, "alpha", session ->
							assertThat( session.find( SchemaDocument.class, 1 ).title ).isEqualTo( "alpha" ) );
					assertThat( connection.getSchema() ).isEqualTo( originalSchema );
				}
				finally {
					for ( String schema : List.of( "ALPHA", "BETA" ) ) {
						factory.getSchemaManager().forSchema( schema ).drop( false );
					}
				}
			}
			assertThat( provider.stops ).isEqualTo( 1 );
			assertThat( connection.isClosed() ).isFalse();
		}
	}

	@Test
	void credentialRouting() throws Exception {
		try ( var connection = DriverManager.getConnection( url(), "sa", "" ) ) {
			var provider = new BorrowedConnectionProvider( connection );
			// tag::multitenancy-credentials[]
			TenantCredentialsMapper<String> credentials = new TenantCredentialsMapper<>() {
				@Override
				public String user(String tenant) {
					return switch ( tenant ) {
						case "alpha" -> "ALPHA_USER";
						case "beta" -> "BETA_USER";
						default -> throw new IllegalArgumentException( "Unknown tenant: " + tenant );
					};
				}

				@Override
				public String password(String tenant) {
					return lookupPassword( user( tenant ) );
				}
			};
			// end::multitenancy-credentials[]
			try ( var factory = configuration( SchemaDocument.class )
					.property( JdbcSettings.CONNECTION_PROVIDER, provider )
					.property( MULTI_TENANT_CREDENTIALS_MAPPER, credentials ).createEntityManagerFactory() ) {
				inTenant( factory, "alpha", session -> session.doWork( c -> {} ) );
				assertThat( provider.user ).isEqualTo( "ALPHA_USER" );
				assertThat( provider.password ).isEqualTo( "test-password-ALPHA_USER" );
				inTenant( factory, "beta", session -> session.doWork( c -> {} ) );
				assertThat( provider.user ).isEqualTo( "BETA_USER" );
			}
		}
	}

	@Test
	void customProviderRoutingAndOwnership() throws Exception {
		try ( var alpha = DriverManager.getConnection( url(), "sa", "" );
				var beta = DriverManager.getConnection( url(), "sa", "" ) ) {
			var alphaProvider = new BorrowedConnectionProvider( alpha );
			var betaProvider = new BorrowedConnectionProvider( beta );
			// tag::multitenancy-provider-configuration[]
			var tenants = new ConfigurableMultiTenantConnectionProvider(
					Map.of( "alpha", alphaProvider, "beta", betaProvider ), "alpha" );
			var configuration = new HibernatePersistenceConfiguration( "tenant-databases" )
					.property( "hibernate.multi_tenant_connection_provider", tenants )
					.property( "hibernate.hbm2ddl.auto", "none" );
			// end::multitenancy-provider-configuration[]
			try ( var factory = configuration.createEntityManagerFactory() ) {
				inTenant( factory, "alpha", session -> session.doWork( c -> assertThat( c ).isSameAs( alpha ) ) );
				inTenant( factory, "beta", session -> session.doWork( c -> assertThat( c ).isSameAs( beta ) ) );
				assertThatThrownBy( () -> tenants.getConnection( "unknown" ) )
						.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "unknown" );
				assertThat( tenants.getAnyConnection() ).isSameAs( alpha );
			}
			assertThat( alphaProvider.stops ).isZero();
			assertThat( betaProvider.stops ).isZero();
			alphaProvider.stop();
			betaProvider.stop();
			assertThat( alphaProvider.stops ).isEqualTo( 1 );
			assertThat( betaProvider.stops ).isEqualTo( 1 );
			assertThat( alpha.isClosed() ).isFalse();
		}
	}

	@Test
	void tenantJdbcTimeZone() {
		// tag::multitenancy-timezone-options[]
		var configuration = configuration( ClockRecord.class )
				.property( "hibernate.type.java_time_use_direct_jdbc", false );
		var zones = Map.of( "alpha", TimeZone.getTimeZone( "UTC" ),
				"beta", TimeZone.getTimeZone( "America/Chicago" ) );
		// end::multitenancy-timezone-options[]
		try ( var factory = configuration.createEntityManagerFactory() ) {
			// tag::multitenancy-timezone-session[]
			LocalDateTime value = LocalDateTime.of( 2020, 7, 15, 12, 0 );
			try ( var session = factory.withOptions().tenantIdentifier( "beta" )
					.jdbcTimeZone( zones.get( "beta" ) ).openSession() ) {
				var transaction = session.beginTransaction();
				session.persist( new ClockRecord( 1, value ) );
				transaction.commit();
			}
			// end::multitenancy-timezone-session[]
			try ( var session = factory.withOptions().tenantIdentifier( "beta" )
					.jdbcTimeZone( zones.get( "beta" ) ).openSession() ) {
				// tag::multitenancy-timezone-roundtrip[]
				assertThat( session.find( ClockRecord.class, 1 ).createdOn ).isEqualTo( value );
				// end::multitenancy-timezone-roundtrip[]
				// tag::multitenancy-timezone-extraction[]
				Timestamp timestamp = session.doReturningWork( connection -> {
					try ( var statement = connection.createStatement();
							var results = statement.executeQuery( "select createdOn from ClockRecord" ) ) {
						results.next();
						return results.getTimestamp( 1, java.util.Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
					}
				} );
				// end::multitenancy-timezone-extraction[]
				Timestamp chicago = session.doReturningWork( connection -> {
					try ( var statement = connection.createStatement();
							var results = statement.executeQuery( "select createdOn from ClockRecord" ) ) {
						results.next();
						return results.getTimestamp( 1, java.util.Calendar.getInstance( zones.get( "beta" ) ) );
					}
				} );
				int offset = ZoneId.of( "America/Chicago" ).getRules().getOffset( timestamp.toInstant() ).getTotalSeconds();
				assertThat( chicago.toInstant() ).isEqualTo( timestamp.toInstant().minusSeconds( offset ) );
			}
		}
	}

	private static String lookupPassword(String user) {
		return "test-password-" + user;
	}

	private static HibernatePersistenceConfiguration configuration(Class<?>... entities) {
		return new HibernatePersistenceConfiguration( "tenants" )
				.managedClasses( entities )
				.property( JdbcSettings.CONNECTION_PROVIDER, "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider" )
				.property( JdbcSettings.URL, url() )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.property( "hibernate.hbm2ddl.auto", "create-drop" )
				.property( "hibernate.multi_tenant.rls_enabled", false );
	}

	private static String url() {
		return "jdbc:h2:mem:tenant_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
	}

	private static void inTenant(SessionFactory factory, String tenant, Consumer<Session> work) {
		try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
			var transaction = session.beginTransaction();
			try {
				work.accept( session );
				if ( transaction.isActive() ) {
					transaction.commit();
				}
			}
			catch (RuntimeException exception) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw exception;
			}
		}
	}

	// tag::multitenancy-mapping[]
	@Entity(name = "TenantDocument")
	public static class Document {
		@Id
		int id;
		@TenantId
		String tenant;
		String title;

		public Document() {}

		Document(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	// end::multitenancy-mapping[]

	// tag::multitenancy-composite-key[]
	@Entity(name = "LocalDocument")
	@IdClass(LocalKey.class)
	public static class LocalDocument {
		@Id
		int id;
		@Id @TenantId
		String tenant;
		String title;

		public LocalDocument() {}

		LocalDocument(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	public static class LocalKey implements Serializable {
		int id;
		String tenant;

		public LocalKey() {}

		LocalKey(int id, String tenant) {
			this.id = id;
			this.tenant = tenant;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof LocalKey key && id == key.id && Objects.equals( tenant, key.tenant );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, tenant );
		}
	}

	// end::multitenancy-composite-key[]

	@Entity(name = "SchemaDocument")
	public static class SchemaDocument {
		@Id
		int id;
		String title;

		public SchemaDocument() {}

		SchemaDocument(int id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	@Entity(name = "ClockRecord")
	public static class ClockRecord {
		@Id
		int id;
		LocalDateTime createdOn;

		public ClockRecord() {}

		ClockRecord(int id, LocalDateTime createdOn) {
			this.id = id;
			this.createdOn = createdOn;
		}
	}

	// tag::multitenancy-resolver[]
	public static class TenantResolver implements CurrentTenantIdentifierResolver<String> {
		String currentTenant = "alpha";

		@Override
		public String resolveCurrentTenantIdentifier() {
			return currentTenant;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return true;
		}

		@Override
		public boolean isRoot(String tenant) {
			return "root".equals( tenant );
		}
	}
	// end::multitenancy-resolver[]

	private static class BorrowedConnectionProvider extends UserSuppliedConnectionProviderImpl implements Stoppable {
		final Connection connection;
		int stops;
		String user;
		String password;

		BorrowedConnectionProvider(Connection connection) {
			this.connection = connection;
		}

		@Override
		public Connection getConnection() {
			return connection;
		}

		@Override
		public Connection getConnection(String user, String password) {
			this.user = user;
			this.password = password;
			return connection;
		}

		@Override
		public void closeConnection(Connection connection) {}

		@Override
		public void stop() {
			stops++;
		}
	}
}
