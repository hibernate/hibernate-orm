/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.c3p0.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import com.mchange.v2.c3p0.DataSources;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.C3p0Settings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.c3p0.internal.C3P0MessageLogger.C3P0_LOGGER;
import static org.hibernate.c3p0.internal.C3P0MessageLogger.C3P0_MSG_LOGGER;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractSetting;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;

/**
 * A connection provider that uses a C3P0 connection pool. Hibernate will use this by
 * default if the {@code hibernate.c3p0.*} properties are set.
 *
 * @author various people
 * @see ConnectionProvider
 */
public class C3P0ConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {
	private static volatile String HIBERNATE_STYLE_SETTING_PREFIX = C3p0Settings.C3P0_CONFIG_PREFIX + ".";

	//swaldman 2006-08-28: define c3p0-style configuration parameters for properties with
	//                     hibernate-specific overrides to detect and warn about conflicting
	//                     declarations
	private static final String C3P0_STYLE_MIN_POOL_SIZE = "c3p0.minPoolSize";
	private static final String C3P0_STYLE_MAX_POOL_SIZE = "c3p0.maxPoolSize";
	private static final String C3P0_STYLE_MAX_IDLE_TIME = "c3p0.maxIdleTime";
	private static final String C3P0_STYLE_MAX_STATEMENTS = "c3p0.maxStatements";
	private static final String C3P0_STYLE_ACQUIRE_INCREMENT = "c3p0.acquireIncrement";
	private static final String C3P0_STYLE_IDLE_CONNECTION_TEST_PERIOD = "c3p0.idleConnectionTestPeriod";

	//swaldman 2006-08-28: define c3p0-style configuration parameters for initialPoolSize, which
	//                     hibernate sensibly lets default to minPoolSize, but we'll let users
	//                     override it with the c3p0-style property if they want.
	private static final String C3P0_STYLE_INITIAL_POOL_SIZE = "c3p0.initialPoolSize";

	private DataSource ds;
	private Integer isolation;
	private boolean autocommit;

	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public Connection getConnection() throws SQLException {
		final Connection connection = ds.getConnection();
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
		return ConnectionProvider.class.equals( unwrapType )
			|| C3P0ConnectionProvider.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType )
				|| C3P0ConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) ds;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void configure(Map<String, Object> props) {
		final String jdbcDriverClass = extractSetting(
				props,
				JdbcSettings.JAKARTA_JDBC_DRIVER,
				JdbcSettings.DRIVER,
				JdbcSettings.JPA_JDBC_DRIVER
		);
		final String jdbcUrl = extractSetting(
				props,
				JdbcSettings.JAKARTA_JDBC_URL,
				JdbcSettings.URL,
				JdbcSettings.JPA_JDBC_URL
		);
		final Properties connectionProps = ConnectionProviderInitiator.getConnectionProperties( props );

		C3P0_MSG_LOGGER.c3p0UsingDriver( jdbcDriverClass, jdbcUrl );
		C3P0_MSG_LOGGER.connectionProperties( ConfigurationHelper.maskOut( connectionProps, "password" ) );

		autocommit = getBoolean( JdbcSettings.AUTOCOMMIT, props );
		C3P0_MSG_LOGGER.autoCommitMode( autocommit );

		if ( jdbcDriverClass == null ) {
			C3P0_MSG_LOGGER.jdbcDriverNotSpecified();
		}
		else {
			try {
				serviceRegistry.requireService( ClassLoaderService.class ).classForName( jdbcDriverClass );
			}
			catch (ClassLoadingException e) {
				throw new ClassLoadingException( C3P0_MSG_LOGGER.jdbcDriverNotFound( jdbcDriverClass ), e );
			}
		}

		try {

			//swaldman 2004-02-07: modify to allow null values to signify fall through to c3p0 PoolConfig defaults
			final Integer minPoolSize = getInteger( C3p0Settings.C3P0_MIN_SIZE, props );
			final Integer maxPoolSize = getInteger( C3p0Settings.C3P0_MAX_SIZE, props );
			final Integer maxIdleTime = getInteger( C3p0Settings.C3P0_TIMEOUT, props );
			final Integer maxStatements = getInteger( C3p0Settings.C3P0_MAX_STATEMENTS, props );
			final Integer acquireIncrement = getInteger( C3p0Settings.C3P0_ACQUIRE_INCREMENT, props );
			final Integer idleTestPeriod = getInteger( C3p0Settings.C3P0_IDLE_TEST_PERIOD, props );

			final Properties c3props = new Properties();

			// turn hibernate.c3p0.* into c3p0.*, so c3p0
			// gets a chance to see all hibernate.c3p0.*
			for ( String key : props.keySet() ) {
				if ( key.startsWith( HIBERNATE_STYLE_SETTING_PREFIX ) ) {
					final String newKey = key.substring( HIBERNATE_STYLE_SETTING_PREFIX.length() );
					if ( props.containsKey( newKey ) ) {
						warnPropertyConflict( key, newKey );
					}
					c3props.put( newKey, props.get( key ) );
				}
			}

			setOverwriteProperty( C3p0Settings.C3P0_MIN_SIZE, C3P0_STYLE_MIN_POOL_SIZE, props, c3props, minPoolSize );
			setOverwriteProperty( C3p0Settings.C3P0_MAX_SIZE, C3P0_STYLE_MAX_POOL_SIZE, props, c3props, maxPoolSize );
			setOverwriteProperty( C3p0Settings.C3P0_TIMEOUT, C3P0_STYLE_MAX_IDLE_TIME, props, c3props, maxIdleTime );
			setOverwriteProperty( C3p0Settings.C3P0_MAX_STATEMENTS, C3P0_STYLE_MAX_STATEMENTS, props, c3props, maxStatements );
			setOverwriteProperty( C3p0Settings.C3P0_ACQUIRE_INCREMENT, C3P0_STYLE_ACQUIRE_INCREMENT, props, c3props, acquireIncrement );
			setOverwriteProperty(
					C3p0Settings.C3P0_IDLE_TEST_PERIOD,
					C3P0_STYLE_IDLE_CONNECTION_TEST_PERIOD,
					props,
					c3props,
					idleTestPeriod
			);

			// revert to traditional hibernate behavior of setting initialPoolSize to minPoolSize
			// unless otherwise specified with a c3p0.*-style parameter.
			final Integer initialPoolSize = getInteger( C3P0_STYLE_INITIAL_POOL_SIZE, props );
			if ( initialPoolSize == null ) {
				setOverwriteProperty( "", C3P0_STYLE_INITIAL_POOL_SIZE, props, c3props, minPoolSize );
			}

			final DataSource unpooled = DataSources.unpooledDataSource( jdbcUrl, connectionProps );

			final Map<String,Object> allProps = new HashMap<>();
			allProps.putAll( props );
			allProps.putAll( PropertiesHelper.map(c3props) );

			ds = DataSources.pooledDataSource( unpooled, allProps );
		}
		catch (Exception e) {
			C3P0_LOGGER.error( C3P0_MSG_LOGGER.unableToInstantiateC3p0ConnectionPool(), e );
			throw new HibernateException( C3P0_MSG_LOGGER.unableToInstantiateC3p0ConnectionPool(), e );
		}

		isolation = ConnectionProviderInitiator.extractIsolation( props );
		C3P0_MSG_LOGGER.jdbcIsolationLevel( ConnectionProviderInitiator.toIsolationNiceName( isolation ) );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	private void setOverwriteProperty(
			String hibernateStyleKey,
			String c3p0StyleKey,
			Map<String,Object> hibp,
			Properties c3p,
			Integer value) {
		if ( value != null ) {
			final String peeledC3p0Key = c3p0StyleKey.substring( 5 );
			c3p.put( peeledC3p0Key, String.valueOf( value ).trim() );
			if ( hibp.containsKey( c3p0StyleKey ) ) {
				warnPropertyConflict( hibernateStyleKey, c3p0StyleKey );
			}
			final String longC3p0StyleKey = "hibernate." + c3p0StyleKey;
			if ( hibp.containsKey( longC3p0StyleKey ) ) {
				warnPropertyConflict( hibernateStyleKey, longC3p0StyleKey );
			}
		}
	}

	private void warnPropertyConflict(String hibernateStyle, String c3p0Style) {
		C3P0_MSG_LOGGER.bothHibernateAndC3p0StylesSet( hibernateStyle, c3p0Style );
	}

	@Override
	public void stop() {
		try {
			DataSources.destroy( ds );
		}
		catch (SQLException sqle) {
			C3P0_MSG_LOGGER.unableToDestroyC3p0ConnectionPool( sqle );
		}
	}

	/**
	 * Close the provider.
	 *
	 * @deprecated Use {@link #stop} instead
	 */
	@SuppressWarnings("UnusedDeclaration")
	@Deprecated
	public void close() {
		stop();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
