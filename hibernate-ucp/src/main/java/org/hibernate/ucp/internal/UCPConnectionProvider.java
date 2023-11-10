/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.oracleucp.internal;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.hibernate.cfg.AvailableSettings;


public class UCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger( "UCPConnectionProvider.class" );
	private PoolDataSource ucpDS = null;
	private UniversalConnectionPoolManager poolManager = null;
	private static final String CONFIG_PREFIX = "hibernate.oracleucp.";
	private boolean autoCommit;
	private Integer isolation;

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map props) throws HibernateException {
		try {
			LOGGER.trace( "Configuring oracle UCP" );

			isolation = ConnectionProviderInitiator.extractIsolation( props );
			autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, props );

			UniversalConnectionPoolManager poolManager = UniversalConnectionPoolManagerImpl.
				getUniversalConnectionPoolManager();
			ucpDS = PoolDataSourceFactory.getPoolDataSource();
			Properties ucpProps = getConfiguration(props);
			configureDataSource(ucpDS, ucpProps);
		}
		catch (Exception e) {
			LOGGER.debug( "oracle UCP Configuration failed" );
			throw new HibernateException( e );
		}

		LOGGER.trace( "oracle UCP Configured" );
	}
	
	private void configureDataSource(PoolDataSource ucpDS, Properties ucpProps) {
		
		List<Method> methods = Arrays.asList(PoolDataSource.class.getDeclaredMethods());
		
		for(String propName : ucpProps.stringPropertyNames()) {
			String value = ucpProps.getProperty(propName);
	
			final String methodName = "set" + propName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propName.substring(1);
			Method writeMethod = methods.stream().filter(m -> m.getName().equals(methodName) && m.getParameterCount() == 1).findFirst().orElse(null);
			if (writeMethod == null) {
				throw new RuntimeException("Property " + propName + " does not exist on target " + PoolDataSource.class);
			}
	
			try {
				Class<?> paramClass = writeMethod.getParameterTypes()[0];
				if (paramClass == int.class) {
					writeMethod.invoke(ucpDS, Integer.parseInt(value.toString()));
				}
				else if (paramClass == long.class) {
					writeMethod.invoke(ucpDS, Long.parseLong(value.toString()));
				}
				else if (paramClass == boolean.class || paramClass == Boolean.class) {
					writeMethod.invoke(ucpDS, Boolean.parseBoolean(value.toString()));
				}
				else if (paramClass == String.class) {
					writeMethod.invoke(ucpDS, value.toString());
				}
				else {
					if(propName.equals("connectionProperties") || 
						propName.equals("connectionFactoryProperties")) {
							if (value != null) {
								Properties connProps = new Properties();

								// The Properties string is in the following format:
								// {prop1=val1, prop2=val2, ..., propN=valN}
								String[] propStrs = value.substring(1, value.length() - 1).split(", ");
								for (String onePropStr : propStrs) {
									// Separate the name and value strings for each property
									String[] nvPair = onePropStr.split("=");
									connProps.setProperty(nvPair[0], nvPair[1]);
								}

								writeMethod.invoke(ucpDS, connProps);
							}
					}
					else {
						writeMethod.invoke(ucpDS, value);
					}
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Properties getConfiguration(Map<?,?> props) {
		Properties ucpProps = new Properties();
		
		copyProperty( AvailableSettings.URL, props, "URL", ucpProps );
		copyProperty( AvailableSettings.USER, props, "user", ucpProps );
		copyProperty( AvailableSettings.PASS, props, "password", ucpProps );
		
		for ( Object keyo : props.keySet() ) {
			if ( !(keyo instanceof String) ) {
				continue;
			}
			String key = (String) keyo;
			if ( key.startsWith( CONFIG_PREFIX ) ) {
				ucpProps.setProperty( key.substring( CONFIG_PREFIX.length() ), (String) props.get( key ) );
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
		if ( ucpDS != null ) {
			conn = ucpDS.getConnection();
			if ( isolation != null && isolation != conn.getTransactionIsolation()) {
				conn.setTransactionIsolation( isolation );
			}

			if ( conn.getAutoCommit() != autoCommit ) {
				conn.setAutoCommit( autoCommit );
			}
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
	@SuppressWarnings("rawtypes")
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
				|| UCPConnectionProvider.class.isAssignableFrom( unwrapType )
				|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
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
		if(this.ucpDS!=null && ucpDS.getConnectionPoolName() != null) {
			try {
				UniversalConnectionPoolManager poolManager = UniversalConnectionPoolManagerImpl.
						getUniversalConnectionPoolManager();
				poolManager.destroyConnectionPool(ucpDS.getConnectionPoolName());
			}
			catch (UniversalConnectionPoolException e) {
				LOGGER.debug("Unable to destroy UCP connection pool");
			}
		}
	}
	
}

