/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.ucp.internal;

import jakarta.annotation.Nonnull;
import oracle.ucp.UniversalConnectionPool;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.UniversalConnectionPoolLifeCycleState;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;
import java.io.Serial;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.hibernate.cfg.UCPSettings.UCP_CONFIG_PREFIX;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasSchema;
import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;

/**
 * {@link ConnectionProvider} based on the Oracle Universal Connection Pool.
 * <p>
 * To force the use of this {@code ConnectionProvider}, set
 * {@value org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER}
 * to {@code ucp} or {@code oracleucp}.
 *
 * @author Loïc Lefèvre
 */
public class UCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	@Serial
	private static final long serialVersionUID = 1L;

	public static final String CONFIG_PREFIX = UCP_CONFIG_PREFIX + ".";

	private PoolDataSource ucpDS;

	private boolean autoCommit;

	private Integer isolation;

	@Override
	public void configure(@Nonnull Map<String, Object> props) throws HibernateException {
		try {
			CONNECTION_INFO_LOGGER.configureConnectionPool( "UCP" );

			isolation = ConnectionProviderInitiator.extractIsolation( props );
			autoCommit = ConfigurationHelper.getBoolean( JdbcSettings.AUTOCOMMIT, props );

			UniversalConnectionPoolManager poolManager = UniversalConnectionPoolManagerImpl.
					getUniversalConnectionPoolManager();
			ucpDS = PoolDataSourceFactory.getPoolDataSource();
			Properties ucpProps = getConfiguration( props );
			configureDataSource( ucpDS, ucpProps );
			poolManager.createConnectionPool((UniversalConnectionPoolAdapter)ucpDS);
			poolManager.startConnectionPool( ucpDS.getConnectionPoolName() );
		}
		catch (Exception e) {
			CONNECTION_INFO_LOGGER.unableToInstantiateConnectionPool( e );
			throw new HibernateException( e );
		}
	}

	private void configureDataSource(PoolDataSource ucpDS, Properties ucpProps) {
		List<Method> methods = Arrays.asList( PoolDataSourceImpl.class.getDeclaredMethods() );

		for ( String propName : ucpProps.stringPropertyNames() ) {
			String value = ucpProps.getProperty( propName );

			final String methodName = "set" + propName.substring( 0, 1 )
					.toUpperCase( Locale.ENGLISH ) + propName.substring( 1 );
			Method writeMethod = methods.stream()
					.filter( m -> m.getName().equals( methodName ) && m.getParameterCount() == 1 ).findFirst()
					.orElse( null );
			if ( writeMethod == null ) {
				// skip properties whenever there is no related UCP DataSource property to be set
				continue;
			}

			try {
				Class<?> paramClass = writeMethod.getParameterTypes()[0];
				if ( paramClass == int.class ) {
					writeMethod.invoke( ucpDS, Integer.parseInt( value ) );
				}
				else if ( paramClass == long.class ) {
					writeMethod.invoke( ucpDS, Long.parseLong( value ) );
				}
				else if ( paramClass == boolean.class || paramClass == Boolean.class ) {
					writeMethod.invoke( ucpDS, Boolean.parseBoolean( value ) );
				}
				else if ( paramClass == String.class ) {
					writeMethod.invoke( ucpDS, value );
				}
				else {
					if ( propName.equals( "connectionProperties" ) ||
						propName.equals( "connectionFactoryProperties" ) ) {
						if ( value != null ) {
							Properties connProps = new Properties();

							// The Properties string is in the following format:
							// {prop1=val1, prop2=val2, ..., propN=valN}
							String[] propStrs = value.substring( 1, value.length() - 1 ).split( ", " );
							for ( String onePropStr : propStrs ) {
								// Separate the name and value strings for each property
								String[] nvPair = onePropStr.split( "=" );
								connProps.setProperty( nvPair[0], nvPair[1] );
							}

							writeMethod.invoke( ucpDS, connProps );
						}
					}
					else {
						writeMethod.invoke( ucpDS, value );
					}
				}
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}

	private Properties getConfiguration(Map<String, Object> props) {
		final Properties ucpProps = ConnectionProviderInitiator.getConnectionProperties( props );

		copyProperty( AvailableSettings.URL, props, "URL", ucpProps );
		copyProperty( AvailableSettings.USER, props, "user", ucpProps );
		copyProperty( AvailableSettings.PASS, props, "password", ucpProps );

		for ( Object keyObject : props.keySet() ) {
			if ( keyObject instanceof String key ) {
				if ( key.startsWith( CONFIG_PREFIX ) ) {
					ucpProps.setProperty( key.substring( CONFIG_PREFIX.length() ), (String) props.get( key ) );
				}
			}
		}

		return ucpProps;
	}

	@SuppressWarnings("rawtypes")
	private static void copyProperty(String srcKey, Map src, String dstKey, Properties dst) {
		if ( src.containsKey( srcKey ) ) {
			dst.setProperty( dstKey, (String) src.get( srcKey ) );
		}
	}

	// *************************************************************************
	// ConnectionProvider
	// *************************************************************************

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = null;

		try {
			if ( ucpDS != null ) {
				UniversalConnectionPoolManager poolManager = UniversalConnectionPoolManagerImpl.
						getUniversalConnectionPoolManager();
				UniversalConnectionPool ucp = poolManager.getConnectionPool( ucpDS.getConnectionPoolName() );
				UniversalConnectionPoolLifeCycleState lifeCycleState = ucp.getLifeCycleState();

				if( lifeCycleState == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING) {
					conn = ucpDS.getConnection();
					if ( isolation != null && isolation != conn.getTransactionIsolation() ) {
						conn.setTransactionIsolation( isolation );
					}

					if ( conn.getAutoCommit() != autoCommit ) {
						conn.setAutoCommit( autoCommit );
					}
				}
				else if(lifeCycleState == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_FAILED ||
						lifeCycleState == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_STOPPED ||
						lifeCycleState == UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_STOPPING) {
					throw new SQLException("UCP connection pool " + this + " has been destroyed.");
				}
			}
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new SQLException( ucpe );
		}

		return conn;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		try (var connection = ucpDS.getConnection()) {
			final var info = new DatabaseConnectionInfoImpl(
					UCPConnectionProvider.class,
					ucpDS.getURL(),
					ucpDS.getConnectionFactoryClassName(),
					dialect.getClass(),
					dialect.getVersion(),
					hasSchema( connection ),
					hasCatalog( connection ),
					ucpDS.getUser(),
					ucpDS.getUser(),
					Boolean.toString( autoCommit ),
					isolation != null
							? ConnectionProviderInitiator.toIsolationConnectionConstantName( isolation )
							: toIsolationNiceName( getIsolation( connection ) ),
					ucpDS.getMinIdle(),
					ucpDS.getMaxPoolSize(),
					getFetchSize( connection )
			);
			if ( !connection.getAutoCommit() ) {
				connection.rollback();
			}
			return info;
		}
		catch (SQLException e) {
			throw new JDBCConnectionException( "Could not create connection", e );
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isUnwrappableAs(@Nonnull Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
			|| UCPConnectionProvider.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(@Nonnull Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
			UCPConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) ucpDS;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void stop() {
		if ( this.ucpDS != null && ucpDS.getConnectionPoolName() != null ) {
			CONNECTION_INFO_LOGGER.cleaningUpConnectionPool( UCP_CONFIG_PREFIX + " [" + ucpDS.getConnectionPoolName() + "]" );
			try {
				UniversalConnectionPoolManager poolManager = UniversalConnectionPoolManagerImpl.
						getUniversalConnectionPoolManager();
				poolManager.stopConnectionPool( ucpDS.getConnectionPoolName() );
				poolManager.destroyConnectionPool( ucpDS.getConnectionPoolName() );
			}
			catch (UniversalConnectionPoolException e) {
				CONNECTION_INFO_LOGGER.unableToDestroyConnectionPool( e );
			}
		}
	}
}
