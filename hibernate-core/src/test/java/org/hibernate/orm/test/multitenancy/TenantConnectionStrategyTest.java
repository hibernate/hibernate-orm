package org.hibernate.orm.test.multitenancy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.naming.Context;
import javax.sql.DataSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryOptions;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.MultiTenantConnectionStrategy.DATABASE;
import static org.hibernate.MultiTenantConnectionStrategy.SCHEMA;
import static org.hibernate.cfg.AvailableSettings.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/// Regression coverage for built-in tenant connection selection and ordinary datasource use.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
class TenantConnectionStrategyTest {
	@Test
	void registeredProviderTakesPrecedenceOverBuiltinDatabaseRouting() throws Exception {
		final var provider = mock( MultiTenantConnectionProvider.class );
		try ( var registry = new StandardServiceRegistryBuilder().clearSettings()
				.applySettings( settings( dataSource(), DATABASE ) )
				.addService( MultiTenantConnectionProvider.class, provider ).build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry )
						.addAnnotatedClass( Item.class ).buildMetadata().buildSessionFactory() ) {
			assertSame( provider, factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class ) );
			assertEquals( DATABASE, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
		}
	}

	@Test
	void restorationFailureIsReportedAndConnectionIsReleased() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		try ( var physical = source.getConnection() ) {
			final var pooled = spy( physical );
			doNothing().when( pooled ).close();
			doThrow( new SQLException( "restore failed" ) ).when( pooled ).setSchema( "PUBLIC" );
			final var pool = mock( DataSource.class );
			when( pool.getConnection() ).thenReturn( pooled );
			withFactory( settings( pool, SCHEMA ), null, builder -> {}, factory -> {
				final var session = factory.withOptions().tenantIdentifier( "alpha" )
						.connectionHandlingMode( org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD )
						.openSession();
				assertSchema( session, "ALPHA" );
				final var failure = assertThrows( HibernateException.class, session::close );
				org.assertj.core.api.Assertions.assertThat( failure ).hasStackTraceContaining( "restore failed" );
			} );
			verify( pooled, times( 1 ) ).close();
		}
	}

	@Test
	void schemaCombinesWithDiscriminatorTenancy() throws Exception {
		final var source = dataSource();
		try ( var connection = source.getConnection(); var statement = connection.createStatement() ) {
			statement.execute( "create schema ALPHA" );
			statement.execute( "create table ALPHA.tenant_partition (id integer primary key, tenant varchar(100))" );
		}
		try ( var registry = new StandardServiceRegistryBuilder().clearSettings()
				.applySettings( settings( source, SCHEMA ) ).build();
				var factory = new MetadataSources( registry ).addAnnotatedClass( Partition.class )
						.buildMetadata().buildSessionFactory() ) {
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				final var transaction = session.beginTransaction();
				final var partition = new Partition();
				partition.id = 1;
				session.persist( partition );
				transaction.commit();
				assertEquals( "alpha", partition.tenant );
			}
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				assertEquals( "alpha", session.find( Partition.class, 1 ).tenant );
			}
		}
	}

	@Test
	void beanContainerSuppliesSchemaMapper() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		final var settings = settings( source, null );
		settings.put( ALLOW_EXTENSIONS_IN_CDI, true );
		settings.put( BEAN_CONTAINER, beanContainer( TenantSchemaMapper.class,
				(TenantSchemaMapper<String>) id -> "ALPHA" ) );
		withFactory( settings, null, builder -> {}, factory -> {
			assertEquals( SCHEMA, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
				assertSchema( session, "ALPHA" );
			}
		} );
	}

	@Test
	void beanContainerSuppliesDatabaseProvider() throws Exception {
		final var provider = mock( MultiTenantConnectionProvider.class );
		final var settings = settings( dataSource(), DATABASE );
		settings.put( ALLOW_EXTENSIONS_IN_CDI, true );
		settings.put( BEAN_CONTAINER, beanContainer( MultiTenantConnectionProvider.class, provider ) );
		withFactory( settings, null, builder -> {}, factory -> assertSame( provider,
				factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class ) ) );
	}

	@Test
	void customProviderCanDelegateSchemaSelectionToHibernate() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		final var provider = mock( MultiTenantConnectionProvider.class );
		when( provider.getConnection( any() ) ).thenAnswer( invocation -> source.getConnection() );
		doAnswer( invocation -> {
			((Connection) invocation.getArgument( 1 )).close();
			return null;
		} ).when( provider ).releaseConnection( any(), any() );
		final var settings = settings( source, SCHEMA );
		settings.put( MULTI_TENANT_CONNECTION_PROVIDER, provider );
		withFactory( settings, null, builder -> {}, factory -> {
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				assertSchema( session, "ALPHA" );
			}
		} );
		settings.remove( MULTI_TENANT_CONNECTION_STRATEGY );
		settings.put( MULTI_TENANT_SCHEMA_MAPPER, (TenantSchemaMapper<String>) id -> "ALPHA" );
		withFactory( settings, null, builder -> {}, factory -> {
			assertNull( factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
				assertSchema( session, "ALPHA" );
			}
		} );
	}

	@Test
	void schemaCombinesWithCredentialsAndCurrentSessions() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		when( source.getConnection( "tenant_user", "tenant_password" ) )
				.thenAnswer( invocation -> source.getConnection() );
		final var settings = settings( source, SCHEMA );
		settings.put( CURRENT_SESSION_CONTEXT_CLASS, "thread" );
		settings.put( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver( "alpha" ) );
		settings.put( MULTI_TENANT_CREDENTIALS_MAPPER, new TenantCredentialsMapper<String>() {
			@Override
			public String user(String id) {
				return "tenant_user";
			}

			@Override
			public String password(String id) {
				return "tenant_password";
			}
		} );
		withFactory( settings, null, builder -> {}, factory -> {
			final var session = factory.getCurrentSession();
			final var transaction = session.beginTransaction();
			assertSchema( session, "ALPHA" );
			transaction.rollback();
		} );
		verify( source ).getConnection( "tenant_user", "tenant_password" );
	}

	@Test
	void suppliedConnectionRemainsCallerManaged() throws Exception {
		final var source = dataSource();
		try ( var supplied = source.getConnection() ) {
			withFactory( settings( source, SCHEMA ), null, builder -> {}, factory -> {
				try ( var session = factory.withOptions().tenantIdentifier( "unprovisioned" )
						.connection( supplied ).openSession() ) {
					assertSchema( session, "PUBLIC" );
				}
			} );
			assertFalse( supplied.isClosed() );
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static BeanContainer beanContainer(Class<?> beanType, Object instance) {
		final var container = mock( BeanContainer.class );
		when( container.getBean( any( Class.class ), any(), any() ) ).thenAnswer( invocation -> {
			final Class<?> requested = invocation.getArgument( 0 );
			final BeanInstanceProducer fallback = invocation.getArgument( 2 );
			final Object beanInstance = requested == beanType ? instance : fallback.produceBeanInstance( requested );
			final ContainedBean bean = mock( ContainedBean.class );
			when( bean.getBeanInstance() ).thenReturn( beanInstance );
			return bean;
		} );
		return container;
	}

	@Test
	void reusedConnectionIsRestoredAfterSuccessAndFailure() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA", "BETA" );
		try ( var physical = source.getConnection() ) {
			final var pooled = spy( physical );
			doNothing().when( pooled ).close();
			final var pool = mock( DataSource.class );
			when( pool.getConnection() ).thenReturn( pooled );
			withFactory( settings( pool, SCHEMA ), null, builder -> {}, factory -> {
				for ( String tenant : new String[] { "alpha", "missing", "beta" } ) {
					try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
						if ( tenant.equals( "missing" ) ) {
							assertThrows( HibernateException.class, () -> assertSchema( session, "MISSING" ) );
						}
						else {
							assertSchema( session, tenant.toUpperCase( java.util.Locale.ROOT ) );
						}
					}
					try {
						assertEquals( "PUBLIC", physical.getSchema() );
					}
					catch (SQLException e) {
						throw new AssertionError( e );
					}
				}
			} );
			verify( pooled, times( 3 ) ).close();
		}
	}

	@Test
	void schemaFromDatasourceInstance() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA", "BETA" );
		withFactory( settings( source, SCHEMA ), null, builder -> {}, factory -> {
			assertFalse( factory.getSessionFactoryOptions().isMultiTenancyEnabled() );
			assertEquals( SCHEMA, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			assertEquals( SCHEMA, new AbstractDelegatingSessionFactoryOptions( factory.getSessionFactoryOptions() ) {}
					.getMultiTenantConnectionStrategy() );
			assertIsolation( factory, "alpha", "beta" );
			assertThrows( HibernateException.class, factory::openSession );
			assertThrows( HibernateException.class, factory::openStatelessSession );
			try ( var parent = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				try ( var child = parent.sessionWithOptions().openSession() ) {
					assertEquals( "alpha", child.getTenantIdentifierValue() );
				}
				assertThrows( HibernateException.class,
						() -> parent.sessionWithOptions().connection().tenantIdentifier( "beta" ).openSession() );
				assertThrows( HibernateException.class,
						() -> parent.statelessWithOptions().connection().tenantIdentifier( "beta" ).openStatelessSession() );
			}
		} );
	}

	@Test
	void schemaFromJndiAndResolverOverride() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA", "BETA" );
		final var settings = settings( "jdbc/default", " schema " );
		settings.put( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver( "alpha" ) );
		withFactory( settings, jndi( Map.of( "jdbc/default", source ) ), builder -> builder.applyMultiTenancy( true ), factory -> {
			assertFalse( factory.getSessionFactoryOptions().isMultiTenancyEnabled() );
			try ( var session = factory.openSession() ) {
				assertEquals( "alpha", session.getTenantIdentifierValue() );
				assertSchema( session, "ALPHA" );
			}
			try ( var session = factory.withOptions().tenantIdentifier( "beta" ).openSession() ) {
				assertSchema( session, "BETA" );
			}
		} );
	}

	@Test
	void mapperCanBeAppliedByBuilder() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		withFactory( settings( source, null ), null,
				builder -> builder.applyTenantSchemaMapper( (TenantSchemaMapper<Integer>) id -> "ALPHA" ), factory -> {
					assertEquals( SCHEMA, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
					assertThrows( HibernateException.class, factory::openSession );
					try ( var session = factory.withOptions().tenantIdentifier( 7 ).openSession() ) {
						assertSchema( session, "ALPHA" );
					}
				} );
	}

	@Test
	void mapperFromSettingsAndNullResolver() throws Exception {
		final var source = dataSource();
		initialize( source, "ALPHA" );
		final var settings = settings( source, null );
		settings.put( MULTI_TENANT_SCHEMA_MAPPER, (TenantSchemaMapper<String>) id -> "ALPHA" );
		settings.put( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver( null ) );
		withFactory( settings, null, builder -> {}, factory -> {
			assertEquals( SCHEMA, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			assertThrows( HibernateException.class, factory::openSession );
			try ( var session = factory.withOptions().tenantIdentifier( "tenant" ).openSession() ) {
				assertSchema( session, "ALPHA" );
			}
		} );
	}

	@Test
	void databaseRoutingAndMapper() throws Exception {
		final var bootstrap = dataSource();
		final var alpha = dataSource();
		final var beta = dataSource();
		initialize( alpha, "PUBLIC" );
		initialize( beta, "PUBLIC" );
		final var naming = jndi( Map.of( "jdbc/bootstrap", bootstrap, "jdbc/alpha", alpha, "jdbc/beta", beta ) );
		final var settings = settings( "jdbc/bootstrap", DATABASE );
		settings.put( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver( "alpha" ) );
		withFactory( settings, naming, builder -> builder.applyMultiTenancy( false ), factory -> {
			assertTrue( factory.getSessionFactoryOptions().isMultiTenancyEnabled() );
			assertIsolation( factory, "alpha", "beta" );
			try ( var session = factory.withOptions().tenantIdentifier( "missing" ).openSession() ) {
				assertThrows( HibernateException.class, () -> session.doWork( connection -> {} ) );
			}
		} );
		initialize( alpha, "MAPPED" );
		settings.put( MULTI_TENANT_SCHEMA_MAPPER, (TenantSchemaMapper<String>) id -> "MAPPED" );
		withFactory( settings, naming, builder -> {}, factory -> {
			assertEquals( DATABASE, factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			try ( var session = factory.openSession() ) {
				assertSchema( session, "MAPPED" );
			}
		} );
	}

	@Test
	void databaseContextAndInvalidBootstrapBinding() throws Exception {
		final var source = dataSource();
		initialize( source, "PUBLIC" );
		final var naming = jndi( Map.of( "jdbc/tenants", mock( Context.class ), "jdbc/tenants/7", source ) );
		final var settings = settings( "jdbc/tenants", DATABASE );
		settings.put( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY, 7 );
		withFactory( settings, naming, builder -> {}, factory -> {
			try ( var session = factory.withOptions().tenantIdentifier( 7 ).openSession() ) {
				assertSchema( session, "PUBLIC" );
			}
		} );
		settings.remove( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY );
		assertBootstrapFailure( settings, naming, "identifier to use for ANY" );
		settings.put( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY, "absent" );
		assertBootstrapFailure( settings, naming, "does not name a DataSource" );
	}

	@Test
	void ordinaryDatasourceDoesNotInferRouting() throws Exception {
		final var source = dataSource();
		final var naming = jndi( Map.of( "jdbc/default", source ) );
		final var settings = settings( "jdbc/default", null );
		settings.put( MULTI_TENANT_IDENTIFIER_RESOLVER, resolver( "other" ) );
		settings.put( "hibernate.multiTenancy", "SCHEMA" );
		withFactory( settings, naming, builder -> {}, factory -> {
			assertNull( factory.getSessionFactoryOptions().getMultiTenantConnectionStrategy() );
			try ( var session = factory.openSession() ) {
				assertSchema( session, "PUBLIC" );
			}
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				assertSchema( session, "PUBLIC" );
			}
		} );
		verify( naming, never() ).locate( "jdbc/other" );
		verify( naming, never() ).locate( "jdbc/alpha" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "NONE", "DISCRIMINATOR", "typo" })
	void invalidStrategiesFail(String strategy) throws Exception {
		assertBootstrapFailure( settings( dataSource(), strategy ), null, MULTI_TENANT_CONNECTION_STRATEGY );
	}

	@Test
	void invalidConfigurationFailsClearly() throws Exception {
		assertBootstrapFailure( settings( dataSource(), 1 ), null, MULTI_TENANT_CONNECTION_STRATEGY );
		assertBootstrapFailure( settings( dataSource(), DATABASE ), null, "JNDI" );
		final var settings = settings( "jdbc/default", DATABASE );
		settings.put( CONNECTION_PROVIDER, mock( ConnectionProvider.class ) );
		assertBootstrapFailure( settings, null, "conflicts" );
	}

	@Test
	void customProviderPrecedenceAndSchemaOwnership() throws Exception {
		final var source = dataSource();
		final var provider = mock( MultiTenantConnectionProvider.class );
		when( provider.getConnection( any() ) ).thenAnswer( invocation -> source.getConnection() );
		doAnswer( invocation -> {
			((Connection) invocation.getArgument( 1 )).close();
			return null;
		} ).when( provider ).releaseConnection( any(), any() );
		when( provider.handlesConnectionSchema() ).thenReturn( true );
		final var mapper = mock( TenantSchemaMapper.class );
		final var settings = settings( source, SCHEMA );
		settings.put( MULTI_TENANT_CONNECTION_PROVIDER, provider );
		settings.put( MULTI_TENANT_SCHEMA_MAPPER, mapper );
		withFactory( settings, null, builder -> {}, factory -> {
			try ( var session = factory.withOptions().tenantIdentifier( 7 ).openSession() ) {
				assertSchema( session, "PUBLIC" );
			}
		} );
		verifyNoInteractions( mapper );
		settings.put( MULTI_TENANT_CREDENTIALS_MAPPER, mock( TenantCredentialsMapper.class ) );
		assertBootstrapFailure( settings, null, MULTI_TENANT_CREDENTIALS_MAPPER );
	}

	@Test
	void invalidSchemaAndNonStringIdentifiersDoNotLeakConnections() throws Exception {
		final var source = dataSource();
		withFactory( settings( source, SCHEMA ), null, builder -> {}, factory -> {
			try ( var session = factory.withOptions().tenantIdentifier( 7 ).openSession() ) {
				assertThrows( HibernateException.class, () -> session.doWork( connection -> fail() ) );
			}
		} );
		for ( String schema : new String[] { null, "", " " } ) {
			withFactory( settings( source, SCHEMA ), null,
					builder -> builder.applyTenantSchemaMapper( (TenantSchemaMapper<String>) id -> schema ), factory -> {
						try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
							assertThrows( HibernateException.class, () -> session.doWork( connection -> fail() ) );
						}
					} );
		}
	}

	private static void assertIsolation(SessionFactory factory, String first, String second) {
		for ( String tenant : new String[] { first, second } ) {
			try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
				final var transaction = session.beginTransaction();
				session.persist( new Item( 1, tenant ) );
				transaction.commit();
			}
		}
		for ( String tenant : new String[] { first, second } ) {
			try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
				assertEquals( tenant, session.find( Item.class, 1 ).name );
			}
			try ( var session = factory.withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
				assertEquals( tenant, session.get( Item.class, 1 ).name );
			}
		}
	}

	private static void assertSchema(Session session, String schema) {
		session.doWork( connection -> assertEquals( schema, connection.getSchema() ) );
	}

	private static Map<String, Object> settings(Object source, Object strategy) {
		final var settings = new HashMap<String, Object>();
		settings.put( DATASOURCE, source );
		if ( strategy != null ) {
			settings.put( MULTI_TENANT_CONNECTION_STRATEGY, strategy );
		}
		settings.put( DIALECT, H2Dialect.class );
		settings.put( ALLOW_METADATA_ON_BOOT, false );
		settings.put( HBM2DDL_AUTO, "none" );
		return settings;
	}

	private static JndiService jndi(Map<String, Object> bindings) {
		final var naming = mock( JndiService.class );
		when( naming.locate( anyString() ) ).thenAnswer( invocation -> bindings.get( invocation.getArgument( 0 ) ) );
		return naming;
	}

	private static DataSource dataSource() throws SQLException {
		final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
		final var source = mock( DataSource.class );
		when( source.getConnection() ).thenAnswer( invocation -> DriverManager.getConnection( url, "sa", "" ) );
		return source;
	}

	private static void initialize(DataSource source, String... schemas) throws SQLException {
		try ( var connection = source.getConnection(); var statement = connection.createStatement() ) {
			for ( String schema : schemas ) {
				statement.execute( "create schema if not exists " + schema );
				statement.execute( "create table " + schema + ".tenant_item (id integer primary key, name varchar(100))" );
			}
		}
	}

	private static CurrentTenantIdentifierResolver<String> resolver(String identifier) {
		return new CurrentTenantIdentifierResolver<>() {
			@Override
			public String resolveCurrentTenantIdentifier() {
				return identifier;
			}

			@Override
			public boolean validateExistingCurrentSessions() {
				return true;
			}
		};
	}

	private static void withFactory(Map<String, Object> settings, JndiService naming,
			Consumer<SessionFactoryBuilder> configure, Consumer<SessionFactoryImplementor> action) {
		final var builder = new StandardServiceRegistryBuilder().clearSettings().applySettings( settings );
		if ( naming != null ) {
			builder.addService( JndiService.class, naming );
		}
		try ( StandardServiceRegistry registry = builder.build() ) {
			final var factoryBuilder = new MetadataSources( registry ).addAnnotatedClass( Item.class )
					.buildMetadata().getSessionFactoryBuilder();
			configure.accept( factoryBuilder );
			try ( var factory = (SessionFactoryImplementor) factoryBuilder.build() ) {
				action.accept( factory );
			}
		}
	}

	private static void assertBootstrapFailure(Map<String, Object> settings, JndiService naming, String message) {
		final var failure = assertThrows( RuntimeException.class,
				() -> withFactory( settings, naming, builder -> {}, factory -> fail( "Bootstrap should fail" ) ) );
		org.assertj.core.api.Assertions.assertThat( failure ).hasStackTraceContaining( message );
	}

	@Entity(name = "TenantConnectionItem")
	@Table(name = "tenant_item")
	static class Item {
		@Id
		int id;
		String name;

		Item() {
		}

		Item(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "TenantConnectionPartition")
	@Table(name = "tenant_partition")
	static class Partition {
		@Id
		int id;
		@org.hibernate.annotations.TenantId
		String tenant;
	}
}
