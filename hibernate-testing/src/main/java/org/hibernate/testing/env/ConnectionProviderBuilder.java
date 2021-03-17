/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.env;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.ReflectHelper;

import org.hibernate.testing.DialectCheck;

/**
 * Defines the JDBC connection information (currently H2) used by Hibernate for unit (not functional!) tests
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConnectionProviderBuilder implements DialectCheck {
	public static final String DRIVER = "org.h2.Driver";
	public static final String DATA_SOURCE = "org.h2.jdbcx.JdbcDataSource";
//	public static final String URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MVCC=TRUE";
	public static final String URL_FORMAT = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1";
	public static final String URL = URL_FORMAT;
	public static final String USER = "sa";
	public static final String PASS = "";

	public static Properties getConnectionProviderProperties(String dbName) {
		Properties props = new Properties( null );
		props.put( Environment.DRIVER, DRIVER );
		props.put( Environment.URL, String.format( URL_FORMAT, dbName ) );
		props.put( Environment.USER, USER );
		props.put( Environment.PASS, PASS );
		return props;
	}

	public static Properties getJpaConnectionProviderProperties(String dbName) {
		Properties props = new Properties( null );
		props.put( Environment.JPA_JDBC_DRIVER, DRIVER );
		props.put( Environment.JPA_JDBC_URL, String.format( URL_FORMAT, dbName ) );
		props.put( Environment.JPA_JDBC_USER, USER );
		props.put( Environment.JPA_JDBC_PASSWORD, PASS );
		return props;
	}

	public static Properties getConnectionProviderProperties() {
		return getConnectionProviderProperties( "db1" );
	}

	public static Properties getJpaConnectionProviderProperties() {
		return getJpaConnectionProviderProperties( "db1" );
	}

	public static DriverManagerConnectionProviderImpl buildConnectionProvider() {
		return buildConnectionProvider( false );
	}

	public static DriverManagerConnectionProviderImpl buildConnectionProvider(String dbName) {
		return buildConnectionProvider( getConnectionProviderProperties( dbName ), false );
	}

	public static DatasourceConnectionProviderImpl buildDataSourceConnectionProvider(String dbName) {
		try {
			Class dataSourceClass = ReflectHelper.classForName( DATA_SOURCE, ConnectionProviderBuilder.class );
			DataSource actualDataSource = (DataSource) dataSourceClass.newInstance();
			ReflectHelper.findSetterMethod( dataSourceClass, "URL", String.class ).invoke(
					actualDataSource,
					String.format( URL, dbName )
			);
			ReflectHelper.findSetterMethod( dataSourceClass, "user", String.class ).invoke( actualDataSource, USER );
			ReflectHelper.findSetterMethod( dataSourceClass, "password", String.class )
					.invoke( actualDataSource, PASS );

			final DataSourceInvocationHandler dataSourceInvocationHandler = new DataSourceInvocationHandler(
					actualDataSource );

			DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl() {
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
			if ("getConnection".equals(method.getName())) {
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

	public static DriverManagerConnectionProviderImpl buildConnectionProvider(final boolean allowAggressiveRelease) {
		return buildConnectionProvider( getConnectionProviderProperties( "db1" ), allowAggressiveRelease );
	}

	private static DriverManagerConnectionProviderImpl buildConnectionProvider(Properties props, final boolean allowAggressiveRelease) {
		DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl() {
			public boolean supportsAggressiveRelease() {
				return allowAggressiveRelease;
			}
		};
		connectionProvider.configure( props );
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
