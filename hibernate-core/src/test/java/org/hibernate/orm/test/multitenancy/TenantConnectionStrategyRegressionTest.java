package org.hibernate.orm.test.multitenancy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.SessionCreationOption;
import org.hibernate.SessionException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.creation.spi.SharedSessionBuilderImplementor;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.MultiTenantConnectionStrategy.SCHEMA;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.MULTI_TENANT_CONNECTION_STRATEGY;
import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Regression coverage for schema verification, shared-session validation,
/// immutable tenant connection options, and directly registered providers.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
class TenantConnectionStrategyRegressionTest {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void silentlyIgnoredSchemaSelectionPreventsApplicationSql(boolean explicitStrategy) throws Exception {
		try ( var fixture = new Fixture( explicitStrategy ) ) {
			if ( !explicitStrategy ) {
				fixture.builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) tenant -> "ALPHA" );
			}
			doNothing().when( fixture.pooled ).setSchema( anyString() );
			try ( var factory = fixture.buildFactory();
					var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				final var failure = assertThrows( HibernateException.class,
						() -> session.doWork( connection -> fail( "Application SQL must not execute on the default schema" ) ) );
				assertTrue( failure.getMessage().contains( "did not select schema 'ALPHA'" ) );
				assertEquals( "PUBLIC", fixture.physical.getSchema() );
			}
			verify( fixture.pooled, times( 1 ) ).close();
		}
	}

	@Test
	void silentlyIgnoredRestorationIsReportedAndReleased() throws Exception {
		try ( var fixture = new Fixture( true ) ) {
			doNothing().when( fixture.pooled ).setSchema( "PUBLIC" );
			try ( var factory = fixture.buildFactory() ) {
				final var session = factory.withOptions().tenantIdentifier( "alpha" )
						.connectionHandlingMode( DELAYED_ACQUISITION_AND_HOLD ).openSession();
				session.doWork( connection -> assertEquals( "ALPHA", connection.getSchema() ) );
				final var failure = assertThrows( HibernateException.class, session::close );
				assertTrue( failure.getMessage().contains( "did not select schema 'PUBLIC'" ) );
			}
			verify( fixture.pooled, times( 1 ) ).close();
		}
	}

	@Test
	void quotedSchemaNamesPassVerification() throws Exception {
		try ( var fixture = new Fixture( true ) ) {
			fixture.builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) tenant -> "\"MiXeD\"" );
			try ( var factory = fixture.buildFactory();
					var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
				session.doWork( connection -> assertEquals( "MiXeD", connection.getSchema() ) );
			}
			assertEquals( "PUBLIC", fixture.physical.getSchema() );
		}
	}

	@Test
	void creationOptionsCannotChangeTheTenantOfASharedConnection() throws Exception {
		try ( var fixture = new Fixture( true );
				var factory = fixture.buildFactory();
				var parent = factory.withOptions().tenantIdentifier( "alpha" )
						.connectionHandlingMode( DELAYED_ACQUISITION_AND_HOLD ).openSession() ) {
			parent.doWork( connection -> assertEquals( "ALPHA", connection.getSchema() ) );
			final var childBuilder = (SharedSessionBuilderImplementor) parent.sessionWithOptions().connection();
			childBuilder.withOption( new SessionCreationOption.TenantId( "beta" ) );
			assertThrows( SessionException.class, childBuilder::openSession );
			assertThrows( SessionException.class,
					() -> parent.sessionWithOptions().connection().tenantIdentifier( "beta" ).openSession() );
			childBuilder.withOption( new SessionCreationOption.TenantId( "alpha" ) );
			try ( var child = childBuilder.openSession() ) {
				assertEquals( "alpha", child.getTenantIdentifierValue() );
				child.doWork( connection -> assertEquals( "ALPHA", connection.getSchema() ) );
			}
		}
	}

	@Test
	void addingAMapperDoesNotChangeAnAlreadyBuiltOrdinaryFactory() throws Exception {
		try ( var fixture = new Fixture( false ); var factory = fixture.buildFactory() ) {
			final var originalOptions = factory.getSessionFactoryOptions();
			assertNull( originalOptions.getMultiTenantConnectionStrategy() );
			fixture.builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) tenant -> "ALPHA" );
			final var laterOptions = ((SessionFactoryBuilderImplementor) fixture.builder).buildSessionFactoryOptions();
			assertEquals( SCHEMA, laterOptions.getMultiTenantConnectionStrategy() );
			assertNotSame( originalOptions, laterOptions );
			assertNull( originalOptions.getMultiTenantConnectionStrategy() );
			assertNull( originalOptions.getTenantSchemaMapper() );
			try ( var ordinary = factory.openSession();
					var identified = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				ordinary.doWork( connection -> assertEquals( "PUBLIC", connection.getSchema() ) );
				identified.doWork( connection -> assertEquals( "PUBLIC", connection.getSchema() ) );
			}
		}
	}

	@Test
	void changingOrRemovingAMapperDoesNotChangeAnAlreadyBuiltSchemaFactory() throws Exception {
		try ( var fixture = new Fixture( false ) ) {
			fixture.builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) tenant -> "ALPHA" );
			try ( var factory = fixture.buildFactory() ) {
				fixture.builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) tenant -> "BETA" );
				final var laterOptions = ((SessionFactoryBuilderImplementor) fixture.builder).buildSessionFactoryOptions();
				assertEquals( "BETA", laterOptions.getTenantSchemaMapper().schemaName( "tenant" ) );
				fixture.builder.applyTenantSchemaMapper( null );
				assertNull( ((SessionFactoryBuilderImplementor) fixture.builder).buildSessionFactoryOptions()
						.getMultiTenantConnectionStrategy() );
				assertEquals( SCHEMA, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
				assertThrows( HibernateException.class, factory::openSession );
				try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
					session.doWork( connection -> assertEquals( "ALPHA", connection.getSchema() ) );
				}
			}
		}
	}

	@Test
	void registeredOrdinaryProviderConflictsWithBuiltinDatabaseRouting() {
		final var ordinaryProvider = mock( ConnectionProvider.class );
		final var source = mock( DataSource.class );
		final var naming = mock( JndiService.class );
		when( naming.locate( "jdbc/bootstrap" ) ).thenReturn( source );
		try ( var registry = databaseRegistryBuilder( naming )
				.addService( ConnectionProvider.class, ordinaryProvider ).build() ) {
			final var failure = assertThrows( RuntimeException.class,
					() -> new MetadataSources( registry ).buildMetadata().buildSessionFactory() );
			org.assertj.core.api.Assertions.assertThat( failure ).hasStackTraceContaining( "registered ConnectionProvider" );
			verifyNoInteractions( ordinaryProvider );
			verifyNoInteractions( source );
		}
	}

	@Test
	void registeredCustomMultiTenantProviderRetainsPrecedence() {
		final var ordinaryProvider = mock( ConnectionProvider.class );
		final var multiTenantProvider = mock( MultiTenantConnectionProvider.class );
		final var naming = mock( JndiService.class );
		try ( var registry = databaseRegistryBuilder( naming )
				.addService( ConnectionProvider.class, ordinaryProvider )
				.addService( MultiTenantConnectionProvider.class, multiTenantProvider ).build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
			assertTrue( factory.getSessionFactoryOptions().isMultiTenancyEnabled() );
			assertSame( multiTenantProvider, registry.getService( MultiTenantConnectionProvider.class ) );
			verifyNoInteractions( ordinaryProvider );
			verifyNoInteractions( naming );
		}
	}

	private static StandardServiceRegistryBuilder databaseRegistryBuilder(JndiService naming) {
		return new StandardServiceRegistryBuilder().clearSettings()
				.applySetting( DATASOURCE, "jdbc/bootstrap" )
				.applySetting( MULTI_TENANT_CONNECTION_STRATEGY, "DATABASE" )
				.applySetting( DIALECT, H2Dialect.class )
				.applySetting( ALLOW_METADATA_ON_BOOT, false )
				.addService( JndiService.class, naming );
	}

	private static class Fixture implements AutoCloseable {
		final Connection physical;
		final Connection pooled;
		final StandardServiceRegistry registry;
		final SessionFactoryBuilder builder;

		Fixture(boolean explicitSchema) throws Exception {
			physical = DriverManager.getConnection( "jdbc:h2:mem:" + UUID.randomUUID(), "sa", "" );
			try ( var statement = physical.createStatement() ) {
				statement.execute( "create schema ALPHA" );
				statement.execute( "create schema BETA" );
				statement.execute( "create schema \"MiXeD\"" );
			}
			pooled = spy( physical );
			doNothing().when( pooled ).close();
			final var source = mock( DataSource.class );
			when( source.getConnection() ).thenReturn( pooled );
			final var registryBuilder = new StandardServiceRegistryBuilder().clearSettings()
					.applySetting( DATASOURCE, source ).applySetting( DIALECT, H2Dialect.class )
					.applySetting( ALLOW_METADATA_ON_BOOT, false ).applySetting( HBM2DDL_AUTO, "none" );
			if ( explicitSchema ) {
				registryBuilder.applySetting( MULTI_TENANT_CONNECTION_STRATEGY, SCHEMA );
			}
			registry = registryBuilder.build();
			builder = new MetadataSources( registry ).buildMetadata().getSessionFactoryBuilder();
		}

		SessionFactoryImplementor buildFactory() {
			return (SessionFactoryImplementor) builder.build();
		}

		@Override
		public void close() throws Exception {
			try {
				registry.close();
			}
			finally {
				physical.close();
			}
		}
	}
}
