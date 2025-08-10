/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.c3p0.internal;

import com.mchange.v2.c3p0.DataSources;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static com.mchange.v2.c3p0.DataSources.pooledDataSource;
import static com.mchange.v2.c3p0.DataSources.unpooledDataSource;
import static java.util.Objects.requireNonNullElse;
import static org.hibernate.c3p0.internal.C3P0MessageLogger.C3P0_MSG_LOGGER;
import static org.hibernate.cfg.C3p0Settings.C3P0_ACQUIRE_INCREMENT;
import static org.hibernate.cfg.C3p0Settings.C3P0_CONFIG_PREFIX;
import static org.hibernate.cfg.C3p0Settings.C3P0_IDLE_TEST_PERIOD;
import static org.hibernate.cfg.C3p0Settings.C3P0_MAX_SIZE;
import static org.hibernate.cfg.C3p0Settings.C3P0_MAX_STATEMENTS;
import static org.hibernate.cfg.C3p0Settings.C3P0_MIN_SIZE;
import static org.hibernate.cfg.C3p0Settings.C3P0_TIMEOUT;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractIsolation;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractSetting;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.getConnectionProperties;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getDriverName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getSchema;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasSchema;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;

/**
 * {@link ConnectionProvider} based on c3p0 connection pool.
 * <p>
 * To force the use of this {@code ConnectionProvider} set
 * {@value org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER}
 * to {@code c3p0}.
 * <p>
 * Hibernate selects this by default if the {@code hibernate.c3p0.*} properties are set.
 *
 * @author various people
 * @see ConnectionProvider
 */
public class C3P0ConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {

	// as specified by c3p0 documentation:
	public static final int DEFAULT_MIN_POOL_SIZE = 3;
	public static final int DEFAULT_MAX_POOL_SIZE = 15;

	private static final String HIBERNATE_STYLE_SETTING_PREFIX = C3P0_CONFIG_PREFIX + ".";

	//swaldman 2006-08-28: define c3p0-style configuration parameters for properties with
	//                     hibernate-specific overrides to detect and warn about conflicting
	//                     declarations
	private static final String C3P0_STYLE_MIN_POOL_SIZE = "c3p0.minPoolSize";
	private static final String C3P0_STYLE_MAX_POOL_SIZE = "c3p0.maxPoolSize";
	private static final String C3P0_STYLE_MAX_IDLE_TIME = "c3p0.maxIdleTime";
	private static final String C3P0_STYLE_MAX_STATEMENTS = "c3p0.maxStatements";
	private static final String C3P0_STYLE_ACQUIRE_INCREMENT = "c3p0.acquireIncrement";
	private static final String C3P0_STYLE_IDLE_CONNECTION_TEST_PERIOD = "c3p0.idleConnectionTestPeriod";
	//swaldman 2006-08-28: define c3p0-style configuration parameters for initialPoolSize,
	//                     which hibernate sensibly lets default to minPoolSize, but we'll
	//                     let users override it with the c3p0-style property if they want.
	private static final String C3P0_STYLE_INITIAL_POOL_SIZE = "c3p0.initialPoolSize";

	private DataSource dataSource;
	private Integer isolation;
	private boolean autocommit;

	private Function<Dialect,DatabaseConnectionInfo> dbInfoProducer;
	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public Connection getConnection() throws SQLException {
		final Connection connection = dataSource.getConnection();
		if ( isolation != null && isolation != connection.getTransactionIsolation() ) {
			connection.setTransactionIsolation( isolation );
		}
		if ( connection.getAutoCommit() != autocommit ) {
			connection.setAutoCommit( autocommit );
		}
		return connection;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isAssignableFrom( C3P0ConnectionProvider.class )
			|| unwrapType.isAssignableFrom( DataSource.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( unwrapType.isAssignableFrom( C3P0ConnectionProvider.class ) ) {
			return (T) this;
		}
		else if ( unwrapType.isAssignableFrom( DataSource.class ) ) {
			return (T) dataSource;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void configure(Map<String, Object> properties) {
		ConnectionInfoLogger.INSTANCE.configureConnectionPool( "c3p0" );

		final String jdbcDriverClass = extractSetting(
				properties,
				JdbcSettings.JAKARTA_JDBC_DRIVER,
				JdbcSettings.DRIVER,
				JdbcSettings.JPA_JDBC_DRIVER
		);
		final String jdbcUrl = extractSetting(
				properties,
				JdbcSettings.JAKARTA_JDBC_URL,
				JdbcSettings.URL,
				JdbcSettings.JPA_JDBC_URL
		);

		loadDriverClass( jdbcDriverClass );

		// c3p0 returns Connections with autocommit enabled, but for
		// historical reasons we default to calling setAutocommit(false)
		// as soon as we obtain a new connection. This maybe isn't ideal,
		// and it's not what we do with Agroal or Hikari.
		autocommit = getBoolean( JdbcSettings.AUTOCOMMIT, properties ); // defaults to false
		isolation = extractIsolation( properties );

		final Properties connectionProps = getConnectionProperties( properties );
		final var poolSettings = poolSettings( properties );
		dataSource = createDataSource( jdbcUrl, connectionProps, poolSettings );

		try ( var connection = dataSource.getConnection() ) {
			final Integer fetchSize = getFetchSize( connection );
			final boolean hasSchema = hasSchema( connection );
			final boolean hasCatalog = hasCatalog( connection );
			final String schema = getSchema( connection );
			final String catalog = getCatalog( connection );
			final String driverName = getDriverName( connection );
			if ( isolation == null ) {
				isolation = getIsolation( connection );
			}
			dbInfoProducer = dialect -> new DatabaseConnectionInfoImpl(
					C3P0ConnectionProvider.class,
					jdbcUrl,
					driverName,
					dialect.getClass(),
					dialect.getVersion(),
					hasSchema,
					hasCatalog,
					schema,
					catalog,
					Boolean.toString( autocommit ),
					isolation == null ? null : toIsolationNiceName( isolation ),
					requireNonNullElse( getInteger( C3P0_STYLE_MIN_POOL_SIZE.substring( 5 ), poolSettings ),
							DEFAULT_MIN_POOL_SIZE ),
					requireNonNullElse( getInteger( C3P0_STYLE_MAX_POOL_SIZE.substring( 5 ), poolSettings ),
							DEFAULT_MAX_POOL_SIZE ),
					fetchSize
			);
			if ( !connection.getAutoCommit() ) {
				connection.rollback();
			}
		}
		catch (SQLException e) {
			throw new JDBCConnectionException( "Could not create connection", e );
		}
	}

	private DataSource createDataSource(String jdbcUrl, Properties connectionProps, Map<String, Object> poolProperties) {
		try {
			return pooledDataSource( unpooledDataSource( jdbcUrl, connectionProps ), poolProperties );
		}
		catch (Exception e) {
			ConnectionInfoLogger.INSTANCE.unableToInstantiateConnectionPool( e );
			throw new ConnectionProviderConfigurationException(
					"Could not configure c3p0: " + e.getMessage(),  e );
		}
	}

	private void loadDriverClass(String jdbcDriverClass) {
		if ( jdbcDriverClass == null ) {
			ConnectionInfoLogger.INSTANCE.jdbcDriverNotSpecified();
		}
		else {
			try {
				serviceRegistry.requireService( ClassLoaderService.class ).classForName( jdbcDriverClass );
			}
			catch (ClassLoadingException e) {
				throw new ClassLoadingException( "JDBC Driver class " + jdbcDriverClass + " not found", e );
			}
		}
	}

	private Map<String, Object> poolSettings(Map<String, Object> hibernateProperties) {
		//swaldman 2004-02-07: modify to allow null values to signify fall through to c3p0 PoolConfig defaults
		Integer maxPoolSize = getInteger( C3P0_MAX_SIZE, hibernateProperties );
		if ( maxPoolSize == null ) {
			// if hibernate.c3p0.max_size is not specified, use hibernate.connection.pool_size
			maxPoolSize = getInteger( JdbcSettings.POOL_SIZE, hibernateProperties );
		}
		final Integer minPoolSize = getInteger( C3P0_MIN_SIZE, hibernateProperties );
		final Integer maxIdleTime = getInteger( C3P0_TIMEOUT, hibernateProperties );
		final Integer maxStatements = getInteger( C3P0_MAX_STATEMENTS, hibernateProperties );
		final Integer acquireIncrement = getInteger( C3P0_ACQUIRE_INCREMENT, hibernateProperties );
		final Integer idleTestPeriod = getInteger( C3P0_IDLE_TEST_PERIOD, hibernateProperties );

		final Map<String,Object> c3p0Properties = new HashMap<>();
		// turn hibernate.c3p0.* into c3p0.*, so c3p0
		// gets a chance to see all hibernate.c3p0.*
		for ( String key : hibernateProperties.keySet() ) {
			if ( key.startsWith( HIBERNATE_STYLE_SETTING_PREFIX ) ) {
				final String newKey = key.substring( HIBERNATE_STYLE_SETTING_PREFIX.length() );
				if ( hibernateProperties.containsKey( newKey ) ) {
					warnPropertyConflict( key, newKey );
				}
				c3p0Properties.put( newKey, hibernateProperties.get( key ) );
			}
		}

		setOverwriteProperty( C3P0_MIN_SIZE, C3P0_STYLE_MIN_POOL_SIZE,
				hibernateProperties, c3p0Properties, minPoolSize );
		setOverwriteProperty( C3P0_MAX_SIZE, C3P0_STYLE_MAX_POOL_SIZE,
				hibernateProperties, c3p0Properties, maxPoolSize );
		setOverwriteProperty( C3P0_TIMEOUT, C3P0_STYLE_MAX_IDLE_TIME,
				hibernateProperties, c3p0Properties, maxIdleTime );
		setOverwriteProperty( C3P0_MAX_STATEMENTS, C3P0_STYLE_MAX_STATEMENTS,
				hibernateProperties, c3p0Properties, maxStatements );
		setOverwriteProperty( C3P0_ACQUIRE_INCREMENT, C3P0_STYLE_ACQUIRE_INCREMENT,
				hibernateProperties, c3p0Properties, acquireIncrement );
		setOverwriteProperty( C3P0_IDLE_TEST_PERIOD, C3P0_STYLE_IDLE_CONNECTION_TEST_PERIOD,
				hibernateProperties, c3p0Properties, idleTestPeriod );

		// revert to traditional behavior of setting initialPoolSize to minPoolSize
		// unless otherwise specified with a c3p0.*-style parameter
		final Integer initialPoolSize = getInteger( C3P0_STYLE_INITIAL_POOL_SIZE, hibernateProperties );
		if ( initialPoolSize == null ) {
			setOverwriteProperty( "", C3P0_STYLE_INITIAL_POOL_SIZE,
					hibernateProperties, c3p0Properties, minPoolSize );
		}

		final Map<String, Object> aggregatedProperties = new HashMap<>();
		aggregatedProperties.putAll( hibernateProperties );
		aggregatedProperties.putAll( c3p0Properties );
		return aggregatedProperties;
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return dbInfoProducer.apply( dialect );
	}

	private void setOverwriteProperty(
			String hibernateStyleKey,
			String c3p0StyleKey,
			Map<String, Object> hibernateProperties,
			Map<String, Object> c3p0Properties,
			Integer value) {
		if ( value != null ) {
			final String peeledC3p0Key = c3p0StyleKey.substring( 5 );
			c3p0Properties.put( peeledC3p0Key, String.valueOf( value ).trim() );
			if ( hibernateProperties.containsKey( c3p0StyleKey ) ) {
				warnPropertyConflict( hibernateStyleKey, c3p0StyleKey );
			}
			final String longC3p0StyleKey = "hibernate." + c3p0StyleKey;
			if ( hibernateProperties.containsKey( longC3p0StyleKey ) ) {
				warnPropertyConflict( hibernateStyleKey, longC3p0StyleKey );
			}
		}
	}

	private void warnPropertyConflict(String hibernateStyle, String c3p0Style) {
		C3P0_MSG_LOGGER.bothHibernateAndC3p0StylesSet( hibernateStyle, c3p0Style );
	}

	@Override
	public void stop() {
		ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( C3P0_CONFIG_PREFIX );
		try {
			DataSources.destroy( dataSource );
		}
		catch (SQLException sqle) {
			ConnectionInfoLogger.INSTANCE.unableToDestroyConnectionPool( sqle );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
