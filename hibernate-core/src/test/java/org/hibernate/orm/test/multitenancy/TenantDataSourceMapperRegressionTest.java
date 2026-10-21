package org.hibernate.orm.test.multitenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantDataSourceMapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.util.connections.BaseDataSource;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.USER;
import static org.hibernate.cfg.AvailableSettings.JNDI_CLASS;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_DATASOURCE_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Regression coverage for credential handling and explicit-provider precedence
/// when tenant datasource mapping is enabled.
///
/// @author Steve Ebersole
@BaseUnitTest
class TenantDataSourceMapperRegressionTest {
	private static final String BOOTSTRAP = "java:comp/env/jdbc/bootstrap";
	private static final String TENANT = "java:comp/env/jdbc/tenant";
	private final List<Connection> connections = new ArrayList<>();

	@AfterEach
	void closeConnections() throws SQLException {
		for ( var connection : connections ) {
			if ( !connection.isClosed() ) {
				connection.close();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void configuredCredentialsArePreserved(boolean conventionalNaming) throws Exception {
		final var source = new RecordingDataSource();
		final var credentials = databaseCredentials();

		// Establish that the same datasource and credentials work with ordinary access.
		try ( var registry = registry( source, source )
				.applySetting( USER, credentials.user() ).applySetting( PASS, credentials.password() ).build() ) {
			final var provider = registry.requireService( ConnectionProvider.class );
			final var connection = provider.getConnection();
			provider.closeConnection( connection );
		}
		assertEquals( List.of( credentials ), source.credentials );
		assertEquals( 0, source.defaultConnections );
		source.credentials.clear();

		try ( var registry = registry( source, source )
				.applySetting( USER, credentials.user() ).applySetting( PASS, credentials.password() )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
						conventionalNaming ? "default" : (TenantDataSourceMapper<String>) tenant -> TENANT ).build() ) {
			final var provider = registry.requireService( MultiTenantConnectionProvider.class );
			final var connection = provider.getAnyConnection();
			try {
				assertFalse( connection.isClosed() );
			}
			finally {
				provider.releaseAnyConnection( connection );
			}
			assertTrue( connection.isClosed() );
			final var tenantConnection = provider.getConnection( "tenant" );
			try {
				assertFalse( tenantConnection.isClosed() );
			}
			finally {
				provider.releaseConnection( "tenant", tenantConnection );
			}
			assertTrue( tenantConnection.isClosed() );
			assertEquals( List.of( credentials, credentials ), source.credentials );
			assertEquals( 0, source.defaultConnections );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@RequiresDialect(H2Dialect.class)
	void tenantCredentialsAreUsedForEveryAcquisition(boolean conventionalNaming) throws Exception {
		final var bootstrap = new RecordingDataSource();
		final var tenantSource = new RecordingDataSource();
		final var credentials = new CountingCredentialsMapper();
		final var suffix = UUID.randomUUID().toString().replace( "-", "" ).toUpperCase( Locale.ROOT );
		final var first = new Credentials( "TENANT_FIRST_" + suffix, "first-password" );
		final var second = new Credentials( "TENANT_SECOND_" + suffix, "second-password" );
		try ( var admin = new BaseDataSource( Environment.getProperties() ).getConnection();
				var statement = admin.createStatement() ) {
			try {
				statement.execute( "create user " + first.user() + " password 'first-password' admin" );
				statement.execute( "create user " + second.user() + " password 'second-password' admin" );
				try ( var registry = registry( bootstrap, tenantSource )
						.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
								conventionalNaming ? "default" : (TenantDataSourceMapper<String>) tenant -> TENANT )
						.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, credentials ).build();
						var factory = new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
					assertEquals( 0, credentials.userCalls );
					assertEquals( 0, credentials.passwordCalls );
					assertTrue( tenantSource.credentials.isEmpty() );
					for ( var expected : List.of( first, second ) ) {
						credentials.current = expected;
						try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
							session.doWork( connection -> assertEquals( expected.user(), connection.getMetaData().getUserName() ) );
						}
					}
					assertEquals( 2, credentials.userCalls );
					assertEquals( 2, credentials.passwordCalls );
					assertEquals( List.of( first, second ), tenantSource.credentials );
					assertEquals( 0, tenantSource.defaultConnections );
				}
			}
			finally {
				statement.execute( "drop user if exists " + first.user() );
				statement.execute( "drop user if exists " + second.user() );
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void datasourceDefaultsAreUsedWithoutCredentials(boolean conventionalNaming) throws Exception {
		final var source = new RecordingDataSource();
		final var builder = registry( source, source )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
						conventionalNaming ? "default" : (TenantDataSourceMapper<String>) tenant -> TENANT );
		builder.getSettings().remove( USER );
		builder.getSettings().remove( PASS );
		try ( var registry = builder.build() ) {
			final var provider = registry.requireService( MultiTenantConnectionProvider.class );
			final var bootstrap = provider.getAnyConnection();
			provider.releaseAnyConnection( bootstrap );
			final var tenant = provider.getConnection( "tenant" );
			provider.releaseConnection( "tenant", tenant );
			assertTrue( bootstrap.isClosed() );
			assertTrue( tenant.isClosed() );
			assertEquals( 2, source.defaultConnections );
			assertTrue( source.credentials.isEmpty() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void factoryBuilderCredentialsAreUsed(boolean conventionalNaming) throws Exception {
		final var source = new RecordingDataSource();
		final var credentials = new CountingCredentialsMapper();
		try ( var registry = registry( source, source )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
						conventionalNaming ? "default" : (TenantDataSourceMapper<String>) tenant -> TENANT ).build();
				var factory = new MetadataSources( registry ).buildMetadata().getSessionFactoryBuilder()
						.applyTenantCredentialsMapper( credentials ).build() ) {
			assertEquals( 0, credentials.userCalls );
			assertEquals( 0, credentials.passwordCalls );
			try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
				session.doWork( connection -> assertFalse( connection.isClosed() ) );
			}
			assertEquals( 1, credentials.userCalls );
			assertEquals( 1, credentials.passwordCalls );
			assertEquals( databaseCredentials(), source.credentials.get( source.credentials.size() - 1 ) );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void explicitlySuppliedConcreteProviderIsRetained(boolean suppliedAsService) throws Exception {
		final var explicit = new DataSourceBasedMultiTenantConnectionProviderImpl<String>( tenant -> TENANT );
		final var builder = registry( new RecordingDataSource(), new RecordingDataSource() )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, (TenantDataSourceMapper<String>) tenant -> BOOTSTRAP );
		if ( suppliedAsService ) {
			builder.addService( MultiTenantConnectionProvider.class, explicit );
		}
		else {
			builder.applySetting( MULTI_TENANT_CONNECTION_PROVIDER, explicit );
		}
		try ( var registry = builder.build() ) {
			// Provided service instances are already initialized by their supplier.
			if ( suppliedAsService ) {
				explicit.injectServices( (ServiceRegistryImplementor) registry );
			}
			assertSame( explicit, registry.requireService( MultiTenantConnectionProvider.class ) );
			try ( var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
				assertSame( explicit, factory.getServiceRegistry().requireService( MultiTenantConnectionProvider.class ) );
			}
		}
	}

	private static Credentials databaseCredentials() {
		final var settings = Environment.getProperties();
		return new Credentials( settings.getProperty( USER ), settings.getProperty( PASS ) );
	}

	private record Credentials(String user, String password) {
	}

	private static class CountingCredentialsMapper implements TenantCredentialsMapper<String> {
		private int userCalls;
		private int passwordCalls;
		private Credentials current = databaseCredentials();

		@Override
		public String user(String tenant) {
			assertEquals( "tenant", tenant );
			userCalls++;
			return current.user();
		}

		@Override
		public String password(String tenant) {
			assertEquals( "tenant", tenant );
			passwordCalls++;
			return current.password();
		}
	}

	private class RecordingDataSource extends BaseDataSource {
		private final List<Credentials> credentials = new ArrayList<>();
		private int defaultConnections;

		RecordingDataSource() {
			super( Environment.getProperties() );
		}

		@Override
		public Connection getConnection() throws SQLException {
			defaultConnections++;
			final var connection = super.getConnection();
			connections.add( connection );
			return connection;
		}

		@Override
		public Connection getConnection(String user, String password) throws SQLException {
			credentials.add( new Credentials( user, password ) );
			final var connection = super.getConnection( user, password );
			connections.add( connection );
			return connection;
		}
	}

	public static class ContextFactory implements InitialContextFactory {
		@Override
		public Context getInitialContext(Hashtable<?, ?> environment) {
			return (Context) environment.get( "testContext" );
		}
	}

	private StandardServiceRegistryBuilder registry(DataSource bootstrap, DataSource tenant) throws NamingException {
		final var context = new TestingJndiContext();
		final var jdbcContext = context.createSubcontext( "java:comp" )
				.createSubcontext( "env" ).createSubcontext( "jdbc" );
		jdbcContext.bind( "bootstrap", bootstrap );
		jdbcContext.bind( "tenant", tenant );
		final var builder = new StandardServiceRegistryBuilder()
				.applySetting( DATASOURCE, BOOTSTRAP )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.applySetting( JNDI_CLASS, ContextFactory.class.getName() )
				.applySetting( "hibernate.jndi.testContext", context );
		builder.getSettings().remove( CONNECTION_PROVIDER );
		return builder;
	}
}
