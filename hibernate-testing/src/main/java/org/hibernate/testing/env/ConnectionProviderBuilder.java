/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.env;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.internal.util.ReflectHelper;

import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * Defines the JDBC connection information (currently H2) used by Hibernate for unit (not functional!) tests
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class ConnectionProviderBuilder implements DialectCheck {
	public static final String DRIVER = "org.h2.Driver";
	public static final String DATA_SOURCE = "org.h2.jdbcx.JdbcDataSource";
	public static final String SHARED_DATABASE_NAME = "db1";
//	public static final String URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1";
	public static final String URL_FORMAT = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE";
	public static final String URL = String.format( URL_FORMAT, SHARED_DATABASE_NAME );
	public static final String USER = "sa";
	public static final String PASS = "";

	public static Properties getConnectionProviderProperties(String dbName) {
		return getConnectionProviderProperties( dbName, Collections.emptyMap() );
	}

	public static Properties getConnectionProviderProperties(String dbName, Map<String, String> environmentOverrides) {
		final Properties globalProperties = Environment.getProperties();
		// since returned global properties are a copy, we just add our overrides to them:
		globalProperties.putAll( environmentOverrides );

		assert globalProperties.getProperty( Environment.URL ).startsWith( "jdbc:h2:" )
				: "Connection provider properties are only usable when running against H2";
		final Properties props = new Properties( null );
		props.put( Environment.DRIVER, DRIVER );
		props.put( Environment.URL, String.format( URL_FORMAT, dbName ) );
		props.put( Environment.USER, USER );
		props.put( Environment.PASS, PASS );
		props.put( DriverManagerConnectionProvider.INIT_SQL, "" );
		props.put( Environment.AUTOCOMMIT, "false" );
		if ( SHARED_DATABASE_NAME.equals( dbName ) ) {
			ServiceRegistryUtil.applySettings( props );
		}
		return props;
	}

	public static Properties getJpaConnectionProviderProperties(String dbName) {
		final Properties globalProperties = Environment.getProperties();
		assert globalProperties.getProperty( Environment.URL ).startsWith( "jdbc:h2:" )
				: "Connection provider properties are only usable when running against H2";
		Properties props = new Properties( null );
		props.put( Environment.JPA_JDBC_DRIVER, DRIVER );
		props.put( Environment.JPA_JDBC_URL, String.format( URL_FORMAT, dbName ) );
		props.put( Environment.JPA_JDBC_USER, USER );
		props.put( Environment.JPA_JDBC_PASSWORD, PASS );
		props.put( DriverManagerConnectionProvider.INIT_SQL, "" );
		props.put( Environment.AUTOCOMMIT, "false" );
		if ( SHARED_DATABASE_NAME.equals( dbName ) ) {
			ServiceRegistryUtil.applySettings( props );
		}
		return props;
	}

	public static Properties getConnectionProviderProperties() {
		return getConnectionProviderProperties( SHARED_DATABASE_NAME );
	}

	public static Properties getJpaConnectionProviderProperties() {
		return getJpaConnectionProviderProperties( SHARED_DATABASE_NAME );
	}

	public static ConnectionProvider buildConnectionProvider() {
		return buildConnectionProvider( false );
	}

	public static ConnectionProvider buildConnectionProvider(String dbName) {
		return buildConnectionProvider( getConnectionProviderProperties( dbName ), false );
	}

	public static ConnectionProvider buildConnectionProvider(String dbName, Map<String, String> environmentOverrides) {
		return buildConnectionProvider( getConnectionProviderProperties( dbName, environmentOverrides ), false );
	}

	public static ConnectionProvider buildDataSourceConnectionProvider(String dbName) {
		final Properties globalProperties = Environment.getProperties();
		assert globalProperties.getProperty( Environment.URL ).startsWith( "jdbc:h2:" )
				: "Connection provider properties are only usable when running against H2";

		try {
			Class<?> dataSourceClass = ReflectHelper.classForName( DATA_SOURCE, ConnectionProviderBuilder.class );
			DataSource actualDataSource = (DataSource) dataSourceClass.newInstance();
			ReflectHelper.findSetterMethod( dataSourceClass, "URL", String.class ).invoke(
					actualDataSource,
					String.format( URL_FORMAT, dbName )
			);
			ReflectHelper.findSetterMethod( dataSourceClass, "user", String.class )
					.invoke( actualDataSource, globalProperties.getProperty( Environment.USER ) );
			ReflectHelper.findSetterMethod( dataSourceClass, "password", String.class )
					.invoke( actualDataSource, globalProperties.getProperty( Environment.PASS ) );

			final var dataSourceInvocationHandler = new DataSourceInvocationHandler( actualDataSource );

			var connectionProvider = new DataSourceConnectionProvider() {
				@Override
				public void stop() {
					dataSourceInvocationHandler.stop();
				}
			};

			connectionProvider.configure(
					Collections.singletonMap(
							Environment.DATASOURCE,
							Proxy.newProxyInstance(
									Thread.currentThread().getContextClassLoader(),
									new Class[] {DataSource.class},
									dataSourceInvocationHandler
							)
					)
			);
			return connectionProvider;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( e );
		}
	}

	private static class DataSourceInvocationHandler implements InvocationHandler {

		private final DataSource target;

		private Connection actualConnection;

		private Connection connectionProxy;

		public DataSourceInvocationHandler(DataSource target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( "getConnection".equals( method.getName() ) ) {
				if(actualConnection == null) {
					actualConnection = (Connection) method.invoke( target, args);
					connectionProxy = (Connection) Proxy.newProxyInstance(
							this.getClass().getClassLoader(),
							new Class[] { Connection.class },
							new ConnectionInvocationHandler( actualConnection )
					);
				}
			}
			return connectionProxy;
		}

		private static class ConnectionInvocationHandler implements InvocationHandler {

			private final Connection target;

			public ConnectionInvocationHandler(Connection target) {
				this.target = target;
			}

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if ("close".equals(method.getName())) {
					//Do nothing
					return null;
				}
				return method.invoke(target, args);
			}
		}

		public void stop() {
			try {
				actualConnection.close();
			}
			catch (SQLException ignore) {
			}
		}
	}

	public static ConnectionProvider buildConnectionProvider(final boolean allowAggressiveRelease) {
		return buildConnectionProvider( getConnectionProviderProperties( SHARED_DATABASE_NAME ), allowAggressiveRelease );
	}

	private static ConnectionProvider buildConnectionProvider(Properties props, final boolean allowAggressiveRelease) {
		ConnectionProviderDelegate connectionProvider = new ConnectionProviderDelegate() {
			public boolean supportsAggressiveRelease() {
				return allowAggressiveRelease;
			}
		};
		if ( props.containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) props.get( AvailableSettings.CONNECTION_PROVIDER ) );
		}
		else {
			connectionProvider.setConnectionProvider( new DriverManagerConnectionProvider() );
		}
		connectionProvider.configure( PropertiesHelper.map( props ) );
		return connectionProvider;
	}

	public static Dialect getCorrespondingDialect() {
		return TestingDatabaseInfo.DIALECT;
	}

	@Override
	public boolean isMatch(Dialect dialect) {
		return getCorrespondingDialect().getClass().equals( dialect.getClass() );
	}
}
