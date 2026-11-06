package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.jdbc.JdbcProperties;
import org.hibernate.testing.orm.junit.DialectContext;

import static java.sql.Connection.*;
import static org.hibernate.cfg.JdbcSettings.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Independently selected database configurations for concurrency conformance.
/// Factory connections share one configuration; no actor changes its isolation.
/// SQL Server database-wide options are confined to a disposable database.
///
/// @author Steve Ebersole
public record ConcurrencyConfiguration(int isolation, Boolean rcsi, Boolean mariaSnapshot) {

	public static Stream<ConcurrencyConfiguration> configurations() {
		return configurations( DialectContext.getDialect() );
	}

	public static Stream<ConcurrencyConfiguration> configurations(Dialect dialect) {
		// Spanner PostgreSQL inherits PostgreSQLDialect, but not its isolation
		// matrix or locking guarantees. Classify it before the generic family.
		if ( isSpanner( dialect ) || dialect instanceof CockroachDialect && dialect.getVersion().isBefore( 23, 2 ) ) {
			return Stream.of( new ConcurrencyConfiguration( TRANSACTION_SERIALIZABLE ) );
		}
		if ( dialect instanceof SQLServerDialect ) {
			return Stream.of( new ConcurrencyConfiguration( 2, false, null ),
					new ConcurrencyConfiguration( 2, true, null ), new ConcurrencyConfiguration( 4096, true, null ) );
		}
		if ( dialect instanceof MariaDBDialect ) {
			return Stream.of( new ConcurrencyConfiguration( 2, null, null ),
					new ConcurrencyConfiguration( 4, null, false ), new ConcurrencyConfiguration( 4, null, true ) );
		}
		if ( dialect instanceof H2Dialect ) {
			return Stream.of( 2, 4, 6, 8 ).map( ConcurrencyConfiguration::new );
		}
		if ( dialect instanceof PostgreSQLDialect || dialect instanceof MySQLDialect ) {
			return Stream.of( 2, 4, 8 ).map( ConcurrencyConfiguration::new );
		}
		if ( dialect instanceof OracleDialect ) {
			return Stream.of( 2, 8 ).map( ConcurrencyConfiguration::new );
		}
		return Stream.of( new ConcurrencyConfiguration( 2 ) );
	}

	private static boolean isSpanner(Dialect dialect) {
		return dialect instanceof SpannerDialect || dialect instanceof SpannerPostgreSQLDialect;
	}

	private ConcurrencyConfiguration(int isolation) {
		this( isolation, null, null );
	}

	public boolean knownReadProfile(Dialect dialect) {
		return !isSpanner( dialect ) && (dialect instanceof H2Dialect || dialect instanceof PostgreSQLDialect
				|| dialect instanceof MySQLDialect || dialect instanceof OracleDialect || dialect instanceof SQLServerDialect
				|| dialect instanceof DB2Dialect || dialect instanceof SybaseDialect);
	}

	public boolean ordinaryProtection(Dialect dialect) {
		return dialect instanceof SQLServerDialect && (isolation == 4 || isolation == 8)
				|| dialect instanceof MySQLDialect && isolation == 8;
	}

	public Boolean ordinaryCurrentRead(Dialect dialect) {
		if ( dialect instanceof DB2Dialect || isSpanner( dialect ) ) return null; // configuration not classified here
		return dialect instanceof SybaseDialect || ordinaryProtection( dialect )
				|| dialect instanceof SQLServerDialect && isolation == 2 && !Boolean.TRUE.equals( rcsi );
	}

	public boolean mayConflict(Dialect dialect, org.hibernate.dialect.lock.spi.Operation operation) {
		return currentReadConflicts( dialect ) || dialect instanceof SQLServerDialect && isolation == 4096
				&& operation != org.hibernate.dialect.lock.spi.Operation.CURRENT_READ;
	}

	public boolean stableSnapshot(Dialect dialect) {
		return !ordinaryProtection( dialect ) && isolation != TRANSACTION_READ_COMMITTED;
	}

	public boolean currentReadConflicts(Dialect dialect) {
		return !isSpanner( dialect ) && (dialect instanceof H2Dialect && isolation != 2
				|| dialect instanceof PostgreSQLDialect && isolation != 2
				|| dialect instanceof OracleDialect && isolation == 8
				|| dialect instanceof MariaDBDialect && Boolean.TRUE.equals( mariaSnapshot ));
	}

	public boolean sharedRead(Dialect dialect) {
		return !isSpanner( dialect ) && (dialect instanceof PostgreSQLDialect || dialect instanceof MySQLDialect || dialect instanceof SQLServerDialect
				|| dialect instanceof DB2Dialect || dialect instanceof SybaseDialect);
	}

	public boolean shortCurrentRead(Dialect dialect) {
		return dialect instanceof SQLServerDialect || dialect instanceof DB2Dialect
				&& !(dialect instanceof DB2iDialect || dialect instanceof DB2zDialect);
	}

	public boolean conditionalWriteConflicts(Dialect dialect) {
		return currentReadConflicts( dialect ) || isolation == TRANSACTION_SERIALIZABLE
				|| dialect instanceof SQLServerDialect && isolation == 4096;
	}

	public void assertConflict(Throwable failure, Dialect dialect) {
		final var sql = ConcurrencyTestSupport.sqlException( failure );
		final Class<?> expectedClassification;
		if ( dialect instanceof SQLServerDialect ) {
			assertEquals( 3960, sql.getErrorCode(), "Expected SQL Server snapshot update conflict" );
			expectedClassification = org.hibernate.JDBCException.class;
		}
		else if ( dialect instanceof MariaDBDialect ) {
			assertEquals( 1020, sql.getErrorCode(), "Expected MariaDB snapshot conflict" );
			expectedClassification = org.hibernate.exception.SnapshotIsolationException.class;
		}
		else if ( dialect instanceof OracleDialect ) {
			assertEquals( 8177, sql.getErrorCode(), "Expected Oracle serialization conflict" );
			expectedClassification = org.hibernate.exception.TransactionSerializationException.class;
		}
		else {
			assertEquals( "40001", sql.getSQLState(), "Expected serialization conflict" );
			expectedClassification = dialect instanceof H2Dialect || dialect instanceof PostgreSQLDialect
					? org.hibernate.exception.LockAcquisitionException.class : org.hibernate.JDBCException.class;
		}
		for ( var cause = failure; cause != null; cause = cause.getCause() ) {
			if ( expectedClassification.isInstance( cause ) ) return;
		}
		throw new AssertionError( "Expected Hibernate classification " + expectedClassification.getSimpleName(), failure );
	}

	public Fixture open() throws Exception {
		return new Fixture( this, null, null );
	}

	public Fixture open(org.hibernate.dialect.lock.spi.TransactionConcurrency declaration) throws Exception {
		return new Fixture( this, declaration, null );
	}

	public Fixture openWithQueue(String queue) throws Exception {
		return new Fixture( this, null, queue );
	}

	/// Owns the pool, schema, and (on SQL Server) isolated database, in that order.
	/// @author Steve Ebersole
	public static final class Fixture implements AutoCloseable {
		private StandardServiceRegistry registry;
		public SessionFactoryImplementor factory;
		private String database;
		private final ConcurrencyConfiguration configuration;

		private Fixture(ConcurrencyConfiguration configuration, org.hibernate.dialect.lock.spi.TransactionConcurrency declaration, String queue) throws Exception {
			this.configuration = configuration;
			final var jdbc = JdbcProperties.INSTANCE;
			final var dialect = DialectContext.getDialect();
			String url = jdbc.getUrl();
			try {
				if ( dialect instanceof SQLServerDialect ) {
					database = "locking_contract_" + UUID.randomUUID().toString().replace( "-", "" );
					try ( var admin = admin(); var statement = admin.createStatement() ) {
						statement.execute( "create database " + database );
						statement.execute( "alter database " + database + " set read_committed_snapshot "
								+ (Boolean.TRUE.equals( configuration.rcsi ) ? "on" : "off") );
						statement.execute( "alter database " + database + " set allow_snapshot_isolation on" );
					}
					url = url.replaceAll( "(?i);databaseName=[^;]*", "" ) + ";databaseName=" + database;
				}
				else {
					try ( var connection = DriverManager.getConnection( url, jdbc.getUser(), jdbc.getPassword() ) ) {
						final boolean supported = connection.getMetaData().supportsTransactionIsolationLevel( configuration.isolation );
						if ( configuration.knownReadProfile( dialect ) ) {
							assertTrue( supported, "Required isolation missing: " + configuration );
						}
						else {
							assumeTrue( supported, "JDBC driver does not support " + configuration );
						}
						if ( configuration.mariaSnapshot != null ) {
							try ( var statement = connection.createStatement() ) {
								statement.executeQuery( "select @@session.innodb_snapshot_isolation" ).close();
							}
							catch (SQLException failure) {
								if ( failure.getErrorCode() != 1193 ) throw failure;
								assumeTrue( !configuration.mariaSnapshot, "MariaDB version has no snapshot-isolation setting" );
							}
						}
					}
				}
				final var builder = new StandardServiceRegistryBuilder()
						.applySetting( CONNECTION_PROVIDER, DriverManagerConnectionProvider.class )
						.applySetting( URL, url ).applySetting( USER, jdbc.getUser() ).applySetting( PASS, jdbc.getPassword() )
						.applySetting( CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, false )
						.applySetting( "hibernate.connection.pool_size", 5 )
						.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
						.applySetting( "hibernate.cache.use_second_level_cache", true )
						.applySetting( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory" )
						.applySetting( "hibernate.show_sql", false );
				if ( declaration != null ) builder.applySetting( "hibernate.transaction.concurrency", declaration );
				if ( queue != null ) {
					builder.applySetting( "hibernate.flush.queue.type", queue )
							.applySetting( CONNECTION_HANDLING, "DELAYED_ACQUISITION_AND_HOLD" );
				}
				PessimisticRefreshSnapshotTest.configureIsolation( builder, configuration.isolation );
				if ( configuration.mariaSnapshot != null ) {
					// Older versions use the OFF behavior but do not accept the command.
					try ( var connection = DriverManager.getConnection( url, jdbc.getUser(), jdbc.getPassword() );
							var statement = connection.createStatement() ) {
						statement.executeQuery( "select @@session.innodb_snapshot_isolation" ).close();
						builder.applySetting( DriverManagerConnectionProvider.INIT_SQL,
								"set session innodb_snapshot_isolation=" + (configuration.mariaSnapshot ? "on" : "off") );
					}
					catch (SQLException failure) {
						if ( failure.getErrorCode() != 1193 ) throw failure;
					}
				}
				registry = builder.build();
				final var metadata = new MetadataSources( registry ).addAnnotatedClass( Row.class ).buildMetadata();
				if ( dialect instanceof OracleDialect && configuration.isolation == 8 ) {
					metadata.getEntityBinding( Row.class.getName() ).getTable().setOptions( "segment creation immediate" );
				}
				factory = (SessionFactoryImplementor) metadata.buildSessionFactory();
				assertEquals( configuration.isolation, factory.getJdbcServices().getJdbcEnvironment()
						.getJdbcMetadata().getTransactionIsolation(), "Bootstrap isolation: " + configuration );
				try ( var session = factory.openSession() ) {
					session.beginTransaction();
					session.doWork( this::assertConfiguration );
					session.persist( new Row( 1 ) );
					session.persist( new Row( 2 ) );
					session.getTransaction().commit();
				}
			}
			catch (Throwable failure) {
				try { close(); } catch (Throwable cleanup) { failure.addSuppressed( cleanup ); }
				throw failure;
			}
		}

		public void assertConfiguration(Connection connection) throws SQLException {
			assertEquals( configuration.isolation, connection.getTransactionIsolation(), configuration.toString() );
			if ( configuration.rcsi != null ) {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select is_read_committed_snapshot_on, snapshot_isolation_state from sys.databases where database_id=db_id()" ) ) {
					assertTrue( rows.next() );
					assertEquals( configuration.rcsi, rows.getBoolean( 1 ) );
					assertEquals( 1, rows.getInt( 2 ), "Native snapshot isolation must be enabled" );
				}
			}
			if ( configuration.mariaSnapshot != null ) {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select @@session.innodb_snapshot_isolation" ) ) {
					assertTrue( rows.next() );
					assertEquals( configuration.mariaSnapshot, rows.getBoolean( 1 ) );
				}
				catch (SQLException failure) {
					if ( failure.getErrorCode() != 1193 || configuration.mariaSnapshot ) throw failure;
				}
			}
		}

		@Override
		public String toString() {
			return configuration.toString();
		}

		private static Connection admin() throws SQLException {
			final var jdbc = JdbcProperties.INSTANCE;
			final var connection = DriverManager.getConnection( jdbc.getUrl(), jdbc.getUser(), jdbc.getPassword() );
			connection.setCatalog( "master" );
			return connection;
		}

		@Override
		public void close() throws Exception {
			try {
				if ( factory != null ) factory.close();
			}
			finally {
				try {
					if ( registry != null ) registry.close();
				}
				finally {
					if ( database != null ) {
						try ( var admin = admin(); var statement = admin.createStatement() ) {
							statement.execute( "drop database " + database );
						}
					}
				}
			}
		}
	}

	/// Existing-row fixture; schema and seed writes finish before test transactions.
	/// @author Steve Ebersole
	@Entity(name = "ConcurrencyRow")
	@Table(name = "concurrency_row")
	@jakarta.persistence.Cacheable
	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	public static class Row {
		@Id public int id;
		@Version public int revision;
		public Row() { }
		public Row(int id) { this.id = id; }
	}
}
