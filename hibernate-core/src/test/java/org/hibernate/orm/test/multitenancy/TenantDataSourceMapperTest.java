package org.hibernate.orm.test.multitenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.context.spi.TenantDataSourceMapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.multitenancy.guide.MultitenancyGuideTests;
import org.hibernate.orm.test.util.connections.BaseDataSource;
import org.hibernate.resource.beans.container.internal.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.AvailableSettings.ALLOW_EXTENSIONS_IN_CDI;
import static org.hibernate.cfg.AvailableSettings.BEAN_CONTAINER;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DATASOURCE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.hibernate.cfg.AvailableSettings.JNDI_CLASS;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_DATASOURCE_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
class TenantDataSourceMapperTest {
	private static final String BOOTSTRAP = "jdbc/bootstrap";
	private TestingJndiContext context;
	private TrackingDataSource bootstrapDataSource;
	private TrackingDataSource tenantDataSource;
	private TrackingDataSource otherDataSource;

	@BeforeEach
	void bindDataSources() throws NamingException {
		context = new TestingJndiContext();
		bootstrapDataSource = new TrackingDataSource();
		tenantDataSource = new TrackingDataSource();
		otherDataSource = new TrackingDataSource();
		context.createSubcontext( "jdbc" );
		context.createSubcontext( "tenants" );
		context.bind( BOOTSTRAP, bootstrapDataSource );
		context.bind( "jdbc/other", otherDataSource );
		context.bind( "tenants/tenant-42", tenantDataSource );
		context.bind( "tenants/tenant-43", otherDataSource );
		context.bind( "tenants/tenant-bootstrap", tenantDataSource );
	}

	@AfterEach
	void closeConnections() throws SQLException {
		for ( var dataSource : List.of( bootstrapDataSource, tenantDataSource, otherDataSource ) ) {
			for ( var connection : dataSource.connections ) {
				if ( !connection.isClosed() ) {
					connection.close();
				}
			}
		}
	}

	static Stream<Object> mappers() {
		return Stream.of( new Mapper(), Mapper.class, Mapper.class.getName() );
	}

	@ParameterizedTest
	@MethodSource("mappers")
	void configuredMapperEnablesProvider(Object mapper) throws SQLException, NamingException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, mapper ).build();
			var factory = factory( registry ) ) {
			assertEquals( BOOTSTRAP,
					registry.requireService( MultiTenancy.class ).getBootstrapDataSourceJndiName() );
			final var provider = provider( registry );
			assertInstanceOf( DataSourceConnectionProvider.class, provider );
			assertNull( registry.getService( MultiTenantConnectionProvider.class ) );
			assertAnyConnection( provider, bootstrapDataSource );
			assertTenantConnection( factory, 42, tenantDataSource );
			// A cached data source remains usable after its JNDI binding is removed.
			context.unbind( "tenants/tenant-42" );
			assertTenantConnection( factory, 42, tenantDataSource );
		}
	}

	@Test
	void differentTenantsUseDifferentDataSources() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, Mapper.class ).build();
			var factory = factory( registry ) ) {
			assertTenantConnection( factory, 42, tenantDataSource );
			assertTenantConnection( factory, 43, otherDataSource );
			assertTenantConnection( factory, 42, tenantDataSource );
		}
	}

	@Test
	void bootstrapDataSourceMayBeAnInstance() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( DATASOURCE, bootstrapDataSource )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, new Mapper() ).build();
			var factory = factory( registry ) ) {
			assertAnyConnection( provider( registry ), bootstrapDataSource );
			assertTenantConnection( factory, 42, tenantDataSource );
			assertThrows( HibernateException.class, factory::openSession );
		}
	}

	@Test
	void readOnlySessionUsesMappedDataSource() {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, new Mapper() ).build();
			var factory = factory( registry );
			var session = factory.withOptions().tenantIdentifier( 42 ).readOnly( true ).openSession() ) {
			session.doWork( connection -> assertConnection( connection, tenantDataSource ) );
		}
	}

	@Test
	void explicitConnectionProviderIsNotReplaced() {
		try ( var registry = registryBuilder()
				.applySetting( CONNECTION_PROVIDER, DriverManagerConnectionProvider.class )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, new Mapper() ).build();
			var factory = factory( registry );
			var session = factory.withOptions().tenantIdentifier( 42 ).openSession() ) {
			assertInstanceOf( DriverManagerConnectionProvider.class, provider( registry ) );
			assertSame( provider( registry ), factory.getServiceRegistry().requireService( ConnectionProvider.class ) );
			assertThrows( UnsupportedOperationException.class, () -> session.doWork( connection -> {} ) );
		}
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	void dataSourceAndSchemaMappersCanBeCombined() throws SQLException {
		final String schema = "TENANT_DS_" + UUID.randomUUID().toString().replace( "-", "" ).toUpperCase( Locale.ROOT );
		try ( var connection = bootstrapDataSource.getConnection(); var statement = connection.createStatement() ) {
			statement.execute( "create schema " + schema );
			try ( var registry = registryBuilder()
					.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, new Mapper() ).build();
				var factory = new MetadataSources( registry ).buildMetadata().getSessionFactoryBuilder()
						.applyTenantSchemaMapper( tenant -> schema ).build();
				var session = factory.withOptions().tenantIdentifier( 42 ).openSession() ) {
				session.doWork( tenantConnection -> {
					assertConnection( tenantConnection, tenantDataSource );
					assertEquals( schema, tenantConnection.getSchema() );
				} );
			}
			finally {
				statement.execute( "drop schema " + schema );
			}
		}
	}

	@Test
	void guideConfigurationEnablesProvider() throws NamingException {
		final var jdbcContext = context.createSubcontext( "java:comp" )
				.createSubcontext( "env" ).createSubcontext( "jdbc" );
		jdbcContext.bind( "alpha", tenantDataSource );
		jdbcContext.bind( "beta", otherDataSource );
		try ( var factory = MultitenancyGuideTests.dataSourceConfiguration()
				.property( CONNECTION_PROVIDER, DataSourceConnectionProvider.class )
				.property( JNDI_CLASS, ContextFactory.class.getName() )
				.property( "hibernate.jndi.testContext", context )
				.createEntityManagerFactory() ) {
			try ( var session = factory.withOptions().tenantIdentifier( "alpha" ).openSession() ) {
				session.doWork( connection -> assertConnection( connection, tenantDataSource ) );
			}
			try ( var session = factory.withOptions().tenantIdentifier( "beta" ).openSession() ) {
				session.doWork( connection -> assertConnection( connection, otherDataSource ) );
			}
		}
	}

	@Test
	void bootstrapNameIsNotTreatedAsTenantIdentifier() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
						(TenantDataSourceMapper<String>) tenant -> "tenants/tenant-" + tenant )
				.build();
			var factory = factory( registry ) ) {
			final var provider = provider( registry );
			assertAnyConnection( provider, bootstrapDataSource );
			assertTenantConnection( factory, "bootstrap", tenantDataSource );
		}
	}

	@Test
	void mapperCanSelectBootstrapDataSource() throws SQLException {
		final var builder = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, Mapper.class )
				.applySetting( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY, 42 );
		builder.getSettings().remove( DATASOURCE );
		try ( var registry = builder.build() ) {
			assertEquals( "tenants/tenant-42",
					registry.requireService( MultiTenancy.class ).getBootstrapDataSourceJndiName() );
			assertAnyConnection( provider( registry ), tenantDataSource );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void defaultStrategyPreservesConventionalNames(boolean useContext) throws SQLException {
		final var builder = registryBuilder().applySetting( MULTI_TENANT_DATASOURCE_MAPPER, "default" );
		if ( useContext ) {
			builder.applySetting( DATASOURCE, "jdbc" )
					.applySetting( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY, "bootstrap" );
		}
		try ( var registry = builder.build();
			var factory = factory( registry ) ) {
			final var provider = provider( registry );
			assertAnyConnection( provider, bootstrapDataSource );
			assertTenantConnection( factory, "other", otherDataSource );
		}
	}

	@Test
	void customMapperWithContextUsesFullNames() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( DATASOURCE, "jdbc" )
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, Mapper.class )
				.applySetting( TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY, 42 )
				.build() ) {
			assertEquals( "tenants/tenant-42",
					registry.requireService( MultiTenancy.class ).getBootstrapDataSourceJndiName() );
			assertAnyConnection( provider( registry ), tenantDataSource );
		}
	}

	@Test
	void defaultStrategyTakesPrecedenceOverBeanContainer() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, "default" )
				.applySetting( ALLOW_EXTENSIONS_IN_CDI, true )
				.applySetting( BEAN_CONTAINER, new MapperBeanContainer( tenant -> "tenants/tenant-42" ) )
				.build();
			var factory = factory( registry ) ) {
			assertAnyConnection( provider( registry ), bootstrapDataSource );
			assertTenantConnection( factory, "other", otherDataSource );
		}
	}

	@Test
	void explicitProviderStillUsesConventionalNamesWithoutMapper() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_CONNECTION_PROVIDER, DataSourceBasedMultiTenantConnectionProviderImpl.class )
				.build() ) {
			assertAnyConnection( multiTenantProvider( registry ), bootstrapDataSource );
			assertTenantConnection( multiTenantProvider( registry ), "other", otherDataSource );
		}
	}

	@Test
	void noMapperDoesNotEnableMultitenancy() {
		try ( var registry = registryBuilder().build() ) {
			assertNull( registry.getService( MultiTenantConnectionProvider.class ) );
		}
	}

	@ParameterizedTest
	@CsvSource({ "false, false", "true, false", "false, true", "true, true" })
	void explicitProviderTakesPrecedence(boolean service, boolean conventionalNaming) throws SQLException {
		final var customProvider = new AbstractDataSourceBasedMultiTenantConnectionProviderImpl<Object>() {
			@Override
			protected DataSource selectAnyDataSource() {
				return bootstrapDataSource;
			}

			@Override
			protected DataSource selectDataSource(Object tenantIdentifier) {
				return otherDataSource;
			}
		};
		final var builder = registryBuilder().applySetting( MULTI_TENANT_DATASOURCE_MAPPER,
				conventionalNaming ? "default" : Mapper.class );
		if ( conventionalNaming ) {
			// A custom provider does not need Hibernate's JNDI namespace configuration.
			builder.getSettings().remove( DATASOURCE );
		}
		if ( service ) {
			builder.addService( MultiTenantConnectionProvider.class, customProvider );
		}
		else {
			builder.applySetting( MULTI_TENANT_CONNECTION_PROVIDER, customProvider );
		}
		try ( var registry = builder.build(); var factory = factory( registry ) ) {
			assertSame( customProvider, multiTenantProvider( registry ) );
			assertSame( customProvider, factory.getServiceRegistry().requireService( MultiTenantConnectionProvider.class ) );
			assertTenantConnection( factory, 42, otherDataSource );
		}
	}

	@Test
	void beanContainerMapperEnablesProvider() {
		final var mapper = new Mapper();
		try ( var registry = registryBuilder()
				.applySetting( ALLOW_EXTENSIONS_IN_CDI, true )
				.applySetting( BEAN_CONTAINER, new MapperBeanContainer( mapper ) )
				.build();
			var factory = (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory() ) {
			assertSame( mapper, factory.getSessionFactoryOptions().getTenantDataSourceMapper() );
			assertTenantSession( factory );
		}
	}

	@Test
	void settingTakesPrecedenceOverBeanContainer() throws SQLException {
		try ( var registry = registryBuilder()
				.applySetting( MULTI_TENANT_DATASOURCE_MAPPER, Mapper.class )
				.applySetting( ALLOW_EXTENSIONS_IN_CDI, true )
				.applySetting( BEAN_CONTAINER, new MapperBeanContainer( tenant -> "jdbc/other" ) )
				.build();
			var factory = factory( registry ) ) {
			assertTenantConnection( factory, 42, tenantDataSource );
		}
	}

	@Test
	void configurationSettingEnablesProvider() {
		final var configuration = new Configuration();
		configuration.getProperties().putAll( registryBuilder().getSettings() );
		final var mapper = new Mapper();
		configuration.getProperties().put( MULTI_TENANT_DATASOURCE_MAPPER, mapper );
		assertSame( mapper, configuration.getProperties().get( MULTI_TENANT_DATASOURCE_MAPPER ) );
		try ( var factory = (SessionFactoryImplementor) configuration.buildSessionFactory() ) {
			assertTenantSession( factory );
		}
	}

	private void assertTenantSession(SessionFactoryImplementor factory) {
		assertTrue( factory.getSessionFactoryOptions().isMultiTenancyEnabled() );
		assertInstanceOf( DataSourceConnectionProvider.class,
				factory.getServiceRegistry().getService( ConnectionProvider.class ) );
		assertNull( factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class ) );
		try ( var session = factory.withOptions().tenantIdentifier( 42 ).openSession() ) {
			session.doWork( connection -> assertConnection( connection, tenantDataSource ) );
		}
	}

	private void assertTenantConnection(
			MultiTenantConnectionProvider<Object> provider, Object tenant, TrackingDataSource dataSource)
			throws SQLException {
		final var connection = provider.getConnection( tenant );
		try {
			assertConnection( connection, dataSource );
		}
		finally {
			provider.releaseConnection( tenant, connection );
		}
		assertTrue( connection.isClosed() );
	}

	private SessionFactoryImplementor factory(StandardServiceRegistry registry) {
		return (SessionFactoryImplementor) new MetadataSources( registry ).buildMetadata().buildSessionFactory();
	}

	private void assertTenantConnection(SessionFactoryImplementor factory, Object tenant, TrackingDataSource dataSource) {
		try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
			session.doWork( connection -> assertConnection( connection, dataSource ) );
		}
	}

	private void assertAnyConnection(ConnectionProvider provider, TrackingDataSource dataSource) throws SQLException {
		final var connection = provider.getConnection();
		try {
			assertConnection( connection, dataSource );
		}
		finally {
			provider.closeConnection( connection );
		}
		assertTrue( connection.isClosed() );
	}

	private void assertAnyConnection(MultiTenantConnectionProvider<Object> provider, TrackingDataSource dataSource)
			throws SQLException {
		final var connection = provider.getAnyConnection();
		try {
			assertConnection( connection, dataSource );
		}
		finally {
			provider.releaseAnyConnection( connection );
		}
		assertTrue( connection.isClosed() );
	}

	private void assertConnection(Connection connection, TrackingDataSource dataSource) throws SQLException {
		assertSame( dataSource.connections.get( dataSource.connections.size() - 1 ), connection );
		assertFalse( connection.isClosed() );
		assertNotNull( connection.getMetaData().getDatabaseProductName() );
	}

	private StandardServiceRegistryBuilder registryBuilder() {
		final var builder = new StandardServiceRegistryBuilder()
				.applySetting( DATASOURCE, BOOTSTRAP )
				.applySetting( JNDI_CLASS, ContextFactory.class.getName() )
				.applySetting( "hibernate.jndi.testContext", context )
				.applySetting( HBM2DDL_AUTO, "none" );
		builder.getSettings().remove( CONNECTION_PROVIDER );
		return builder;
	}

	private ConnectionProvider provider(StandardServiceRegistry registry) {
		return registry.requireService( ConnectionProvider.class );
	}

	@SuppressWarnings("unchecked")
	private MultiTenantConnectionProvider<Object> multiTenantProvider(StandardServiceRegistry registry) {
		return registry.requireService( MultiTenantConnectionProvider.class );
	}

	public static class Mapper implements TenantDataSourceMapper<Integer> {
		@Override
		public @Nonnull String dataSourceName(@Nonnull Integer tenantIdentifier) {
			return "tenants/tenant-" + tenantIdentifier;
		}
	}

	public static class ContextFactory implements InitialContextFactory {
		@Override
		public Context getInitialContext(Hashtable<?, ?> environment) {
			return (Context) environment.get( "testContext" );
		}
	}

	private static class TrackingDataSource extends BaseDataSource {
		private final List<Connection> connections = new ArrayList<>();

		TrackingDataSource() {
			super( Environment.getProperties() );
		}

		@Override
		public Connection getConnection() throws SQLException {
			final var connection = super.getConnection();
			connections.add( connection );
			return connection;
		}

		@Override
		public Connection getConnection(String user, String password) throws SQLException {
			final var connection = super.getConnection( user, password );
			connections.add( connection );
			return connection;
		}
	}

	private static class MapperBeanContainer extends AbstractBeanContainer {
		private final TenantDataSourceMapper<?> mapper;

		MapperBeanContainer(TenantDataSourceMapper<?> mapper) {
			this.mapper = mapper;
		}

		@Override
		protected <B> ContainedBean<B> createBean(
				Class<B> beanType, LifecycleOptions options, BeanInstanceProducer fallbackProducer) {
			final B bean = beanType == TenantDataSourceMapper.class
					? beanType.cast( mapper ) : fallbackProducer.produceBeanInstance( beanType );
			return new ContainedBean<>() {
				@Override
				public B getBeanInstance() {
					return bean;
				}

				@Override
				public Class<B> getBeanClass() {
					return beanType;
				}

				@Override
				public void initialize() {
				}

				@Override
				public void release() {
				}
			};
		}

		@Override
		protected <B> ContainedBean<B> createBean(
				String name, Class<B> beanType, LifecycleOptions options, BeanInstanceProducer fallbackProducer) {
			return createBean( beanType, options, fallbackProducer );
		}
	}
}
