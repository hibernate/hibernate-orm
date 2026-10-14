package org.hibernate.orm.test.jdbc.guide;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.util.connections.BaseDataSource;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable Database Access examples using isolated H2 databases.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
public class DatabaseAccessGuideTests {
	@Test
	void externalDataSource() throws Exception {
		try ( var dataSource = new OwnedDataSource( url() ) ) {
			// tag::database-datasource[]
			try ( var factory = new HibernatePersistenceConfiguration( "application" )
					.managedClasses( Record.class )
					.property( "jakarta.persistence.nonJtaDataSource", dataSource )
					.property( "hibernate.hbm2ddl.auto", "create-drop" )
					.createEntityManagerFactory() ) {
				factory.runInTransaction( entityManager -> {
					entityManager.persist( new Record( 1, "datasource" ) );
				} );
				factory.runInTransaction( entityManager -> {
					assertThat( entityManager.find( Record.class, 1 ).text ).isEqualTo( "datasource" );
				} );
			}
			// The application still owns dataSource after the factory is closed.
			// end::database-datasource[]
			assertThat( dataSource.closed ).isFalse();
			try ( var connection = dataSource.getConnection() ) {
				assertThat( connection.isValid( 1 ) ).isTrue();
			}
		}
	}

	@Test
	void suppliedConnection() throws Exception {
		final String jdbcUrl = url();
		try ( var factory = factory( jdbcUrl ) ) {
			// tag::database-supplied[]
			try ( var connection = DriverManager.getConnection( jdbcUrl, "sa", "" ) ) {
				try ( var session = factory.withOptions().connection( connection ).openSession() ) {
					var transaction = session.beginTransaction();
					try {
						session.persist( new Record( 1, "supplied" ) );
						transaction.commit();
					}
					catch (RuntimeException exception) {
						if ( transaction.isActive() ) {
							transaction.rollback();
						}
						throw exception;
					}
				}
				// Closing the session does not close the supplied connection.
				assertThat( connection.isClosed() ).isFalse();
			}
			// end::database-supplied[]
			factory.inTransaction( session ->
					assertThat( session.find( Record.class, 1 ).text ).isEqualTo( "supplied" ) );
		}
	}

	@Test
	void jdbcCallbacks() {
		try ( var factory = factory( url() ) ) {
			// tag::database-work[]
			factory.inTransaction( session -> {
				session.doWork( connection -> {
					try ( var statement = connection.prepareStatement(
							"insert into AccessRecord (id, text) values (?, ?)" ) ) {
						statement.setInt( 1, 1 );
						statement.setString( 2, "JDBC" );
						statement.executeUpdate();
					}
				} );
				long count = session.doReturningWork( connection -> {
					try ( var statement = connection.prepareStatement( "select count(*) from AccessRecord" );
							var results = statement.executeQuery() ) {
						results.next();
						return results.getLong( 1 );
					}
				} );
				assertThat( count ).isEqualTo( 1 );
			} );
			// end::database-work[]
			factory.inTransaction( session ->
					assertThat( session.find( Record.class, 1 ).text ).isEqualTo( "JDBC" ) );
			try ( var session = factory.openSession() ) {
				var transaction = session.beginTransaction();
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.executeUpdate( "delete from AccessRecord" );
					}
				} );
				transaction.rollback();
			}
			factory.inTransaction( session -> assertThat( session.find( Record.class, 1 ) ).isNotNull() );
		}
	}

	@Test
	void metadataWithoutConnection() {
		var provider = new NoConnections();
		// tag::database-metadata[]
		var configuration = new HibernatePersistenceConfiguration( "metadata" )
				.property( "hibernate.boot.allow_jdbc_metadata_access", JdbcMetadataOnBoot.DISALLOW )
				.property( "jakarta.persistence.database-product-name", "H2" )
				.property( "jakarta.persistence.database-product-version", "2.3.232" )
				.property( "hibernate.hbm2ddl.auto", "none" );
		// end::database-metadata[]
		configuration.property( JdbcSettings.CONNECTION_PROVIDER, provider );
		try ( var factory = configuration.createEntityManagerFactory() ) {
			var dialect = factory.unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
			assertThat( dialect.getClass().getSimpleName() ).isEqualTo( "H2Dialect" );
			assertThat( dialect.getVersion().getMajor() ).isEqualTo( 2 );
			assertThat( provider.requests ).isZero();
		}
	}

	@Test
	void unsupportedAggressiveRelease() {
		var configuration = configuration( url() )
				.property( JdbcSettings.CONNECTION_HANDLING,
						PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT );
		try ( var factory = configuration.createEntityManagerFactory();
				var session = factory.openSession() ) {
			var implementation = (org.hibernate.engine.spi.SessionImplementor) session;
			assertThat( implementation.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode() )
					.isEqualTo( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION );
			var transaction = session.beginTransaction();
			session.doWork( connection -> assertThat( connection.isClosed() ).isFalse() );
			assertThat( implementation.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() ).isTrue();
			transaction.commit();
			assertThat( implementation.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() ).isFalse();
		}
	}

	private static SessionFactory factory(String url) {
		return configuration( url ).createEntityManagerFactory();
	}

	private static HibernatePersistenceConfiguration configuration(String url) {
		return new HibernatePersistenceConfiguration( "access" )
				.managedClasses( Record.class )
				.property( JdbcSettings.CONNECTION_PROVIDER, "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider" )
				.property( JdbcSettings.URL, url )
				.property( JdbcSettings.USER, "sa" )
				.property( JdbcSettings.PASS, "" )
				.property( "hibernate.hbm2ddl.auto", "create-drop" );
	}

	private static String url() {
		return "jdbc:h2:mem:access_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
	}

	@Entity(name = "AccessRecord")
	public static class Record {
		@Id
		int id;
		String text;

		public Record() {
		}

		Record(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	private static class OwnedDataSource extends BaseDataSource implements AutoCloseable {
		boolean closed;

		OwnedDataSource(String url) {
			super( properties( url ) );
		}

		@Override
		public Connection getConnection() throws SQLException {
			if ( closed ) {
				throw new SQLException( "Datasource is closed" );
			}
			return super.getConnection();
		}

		@Override
		public void close() {
			closed = true;
		}

		private static Properties properties(String url) {
			var properties = new Properties();
			properties.setProperty( JdbcSettings.URL, url );
			properties.setProperty( JdbcSettings.USER, "sa" );
			properties.setProperty( JdbcSettings.PASS, "" );
			return properties;
		}
	}

	private static class NoConnections extends UserSuppliedConnectionProviderImpl {
		int requests;

		@Override
		public Connection getConnection() {
			requests++;
			throw new AssertionError( "Bootstrap must not acquire a connection" );
		}
	}
}
