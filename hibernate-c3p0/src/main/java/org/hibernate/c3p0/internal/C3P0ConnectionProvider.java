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
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

/**
 * A connection provider that uses a C3P0 connection pool. Hibernate will use this by
 * default if the <tt>hibernate.c3p0.*</tt> properties are set.
 *
 * @author various people
 * @see ConnectionProvider
 */
public class C3P0ConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {

	private static final C3P0MessageLogger LOG = Logger.getMessageLogger(
			C3P0MessageLogger.class,
			C3P0ConnectionProvider.class.getName()
	);

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
	@SuppressWarnings("UnnecessaryUnboxing")
	public Connection getConnection() throws SQLException {
		final Connection c = ds.getConnection();
		if ( isolation != null ) {
			c.setTransactionIsolation( isolation.intValue() );
		}
		if ( c.getAutoCommit() != autocommit ) {
			c.setAutoCommit( autocommit );
		}
		return c;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				C3P0ConnectionProvider.class.isAssignableFrom( unwrapType ) ||
				DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				C3P0ConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
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
	@SuppressWarnings({"unchecked"})
	public void configure(Map props) {
		final String jdbcDriverClass = (String) props.get( Environment.DRIVER );
		final String jdbcUrl = (String) props.get( Environment.URL );
		final Properties connectionProps = ConnectionProviderInitiator.getConnectionProperties( props );

		LOG.c3p0UsingDriver( jdbcDriverClass, jdbcUrl );
		LOG.connectionProperties( ConfigurationHelper.maskOut( connectionProps, "password" ) );

		autocommit = ConfigurationHelper.getBoolean( Environment.AUTOCOMMIT, props );
		LOG.autoCommitMode( autocommit );

		if ( jdbcDriverClass == null ) {
			LOG.jdbcDriverNotSpecified( Environment.DRIVER );
		}
		else {
			try {
				serviceRegistry.getService( ClassLoaderService.class ).classForName( jdbcDriverClass );
			}
			catch (ClassLoadingException e) {
				throw new ClassLoadingException( LOG.jdbcDriverNotFound( jdbcDriverClass ), e );
			}
		}

		try {

			//swaldman 2004-02-07: modify to allow null values to signify fall through to c3p0 PoolConfig defaults
			final Integer minPoolSize = ConfigurationHelper.getInteger( Environment.C3P0_MIN_SIZE, props );
			final Integer maxPoolSize = ConfigurationHelper.getInteger( Environment.C3P0_MAX_SIZE, props );
			final Integer maxIdleTime = ConfigurationHelper.getInteger( Environment.C3P0_TIMEOUT, props );
			final Integer maxStatements = ConfigurationHelper.getInteger( Environment.C3P0_MAX_STATEMENTS, props );
			final Integer acquireIncrement = ConfigurationHelper.getInteger( Environment.C3P0_ACQUIRE_INCREMENT, props );
			final Integer idleTestPeriod = ConfigurationHelper.getInteger( Environment.C3P0_IDLE_TEST_PERIOD, props );

			final Properties c3props = new Properties();

			// turn hibernate.c3p0.* into c3p0.*, so c3p0
			// gets a chance to see all hibernate.c3p0.*
			for ( Object o : props.keySet() ) {
				if ( !String.class.isInstance( o ) ) {
					continue;
				}
				final String key = (String) o;
				if ( key.startsWith( "hibernate.c3p0." ) ) {
					final String newKey = key.substring( 15 );
					if ( props.containsKey( newKey ) ) {
						warnPropertyConflict( key, newKey );
					}
					c3props.put( newKey, props.get( key ) );
				}
			}

			setOverwriteProperty( Environment.C3P0_MIN_SIZE, C3P0_STYLE_MIN_POOL_SIZE, props, c3props, minPoolSize );
			setOverwriteProperty( Environment.C3P0_MAX_SIZE, C3P0_STYLE_MAX_POOL_SIZE, props, c3props, maxPoolSize );
			setOverwriteProperty( Environment.C3P0_TIMEOUT, C3P0_STYLE_MAX_IDLE_TIME, props, c3props, maxIdleTime );
			setOverwriteProperty(
					Environment.C3P0_MAX_STATEMENTS, C3P0_STYLE_MAX_STATEMENTS, props, c3props, maxStatements
			);
			setOverwriteProperty(
					Environment.C3P0_ACQUIRE_INCREMENT, C3P0_STYLE_ACQUIRE_INCREMENT, props, c3props, acquireIncrement
			);
			setOverwriteProperty(
					Environment.C3P0_IDLE_TEST_PERIOD,
					C3P0_STYLE_IDLE_CONNECTION_TEST_PERIOD,
					props,
					c3props,
					idleTestPeriod
			);

			// revert to traditional hibernate behavior of setting initialPoolSize to minPoolSize
			// unless otherwise specified with a c3p0.*-style parameter.
			final Integer initialPoolSize = ConfigurationHelper.getInteger( C3P0_STYLE_INITIAL_POOL_SIZE, props );
			if ( initialPoolSize == null ) {
				setOverwriteProperty( "", C3P0_STYLE_INITIAL_POOL_SIZE, props, c3props, minPoolSize );
			}

			final DataSource unpooled = DataSources.unpooledDataSource( jdbcUrl, connectionProps );

			final Map allProps = new HashMap();
			allProps.putAll( props );
			allProps.putAll( c3props );

			ds = DataSources.pooledDataSource( unpooled, allProps );
		}
		catch (Exception e) {
			LOG.error( LOG.unableToInstantiateC3p0ConnectionPool(), e );
			throw new HibernateException( LOG.unableToInstantiateC3p0ConnectionPool(), e );
		}

		final String i = (String) props.get( Environment.ISOLATION );
		if ( StringHelper.isEmpty( i ) ) {
			isolation = null;
		}
		else {
			isolation = Integer.valueOf( i );
			LOG.jdbcIsolationLevel( Environment.isolationLevelToString( isolation ) );
		}

	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	private void setOverwriteProperty(
			String hibernateStyleKey,
			String c3p0StyleKey,
			Map hibp,
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
		LOG.bothHibernateAndC3p0StylesSet( hibernateStyle, c3p0Style );
	}

	@Override
	public void stop() {
		try {
			DataSources.destroy( ds );
		}
		catch (SQLException sqle) {
			LOG.unableToDestroyC3p0ConnectionPool( sqle );
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
