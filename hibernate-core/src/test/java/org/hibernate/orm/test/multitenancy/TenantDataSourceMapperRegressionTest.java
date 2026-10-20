package org.hibernate.orm.test.multitenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantDataSourceMapper;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.util.connections.BaseDataSource;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.USER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_DATASOURCE_MAPPER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Regression coverage for credential handling and explicit-provider precedence
/// when tenant datasource mapping is enabled.
///
/// @author Steve Ebersole
@BaseUnitTest
class TenantDataSourceMapperRegressionTest {
	private static final String BOOTSTRAP = "jdbc/bootstrap";
	private static final String TENANT = "jdbc/tenant";
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
		final var source = source();
		doThrow( new SQLException( "Explicit credentials required" ) ).when( source ).getConnection();
		when( source.getConnection( "bootstrap-user", "bootstrap-password" ) )
				.thenAnswer( ignored -> connection() );

		// Establish that the same datasource and credentials work with ordinary access.
		try ( var registry = registry( source, source )
				.applySetting( USER, "bootstrap-user" ).applySetting( PASS, "bootstrap-password" ).build() ) {
			final var provider = registry.requireService( ConnectionProvider.class );
			final var connection = provider.getConnection();
			provider.closeConnection( connection );
		}
		clearInvocations( source );

		try ( var registry = registry( source, source )
				.applySetting( USER, "bootstrap-user" ).applySetting( PASS, "bootstrap-password" )
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
			final var tenantConnection = provider.getConnection( "tenant" );
			try {
				assertFalse( tenantConnection.isClosed() );
			}
			finally {
				provider.releaseConnection( "tenant", tenantConnection );
			}
			verify( source, times( 2 ) ).getConnection( "bootstrap-user", "bootstrap-password" );
			verify( source, never() ).getConnection();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void tenantCredentialsAreUsedForEveryAcquisition(boolean conventionalNaming) throws Exception {
		final var bootstrap = source();
		final var tenantSource = source();
		when( tenantSource.getConnection( "first-user", "first-password" ) ).thenAnswer( ignored -> connection() );
		when( tenantSource.getConnection( "second-user", "second-password" ) ).thenAnswer( ignored -> connection() );
		final var credentials = mock( TenantCredentialsMapper.class );
		when( credentials.user( "tenant" ) ).thenReturn( "first-user", "second-user" );
		when( credentials.password( "tenant" ) ).thenReturn( "first-password", "second-password" );
		try ( var registry = registry( bootstrap, tenantSource )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
						conventionalNaming ? "default" : (TenantDataSourceMapper<String>) tenant -> TENANT )
				.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, credentials ).build();
				var factory = new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
			verify( credentials, never() ).user( "tenant" );
			verify( credentials, never() ).password( "tenant" );
			clearInvocations( tenantSource );
			for ( int i = 0; i < 2; i++ ) {
				try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
					session.doWork( connection -> assertFalse( connection.isClosed() ) );
				}
			}
			verify( tenantSource ).getConnection( "first-user", "first-password" );
			verify( tenantSource ).getConnection( "second-user", "second-password" );
			verify( tenantSource, never() ).getConnection();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void explicitlySuppliedConcreteProviderIsRetained(boolean suppliedAsService) throws Exception {
		final var explicit = new DataSourceBasedMultiTenantConnectionProviderImpl<String>( tenant -> TENANT );
		final var builder = registry( source(), source() )
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

	private DataSource source() throws SQLException {
		final var source = mock( DataSource.class );
		when( source.getConnection() ).thenAnswer( ignored -> connection() );
		return source;
	}

	private Connection connection() throws SQLException {
		final var connection = new BaseDataSource( Environment.getProperties() ).getConnection();
		connections.add( connection );
		return connection;
	}

	private StandardServiceRegistryBuilder registry(DataSource bootstrap, DataSource tenant) {
		final var jndi = mock( JndiService.class );
		when( jndi.locate( BOOTSTRAP ) ).thenReturn( bootstrap );
		when( jndi.locate( TENANT ) ).thenReturn( tenant );
		final var builder = new StandardServiceRegistryBuilder()
				.applySetting( DATASOURCE, BOOTSTRAP )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.addService( JndiService.class, jndi );
		builder.getSettings().remove( CONNECTION_PROVIDER );
		return builder;
	}
}
