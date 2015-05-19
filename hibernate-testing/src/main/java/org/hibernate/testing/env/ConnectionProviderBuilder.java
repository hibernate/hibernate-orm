/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.env;

import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;

import org.hibernate.testing.DialectCheck;

/**
 * Defines the JDBC connection information (currently H2) used by Hibernate for unit (not functional!) tests
 *
 * @author Steve Ebersole
 */
public class ConnectionProviderBuilder implements DialectCheck {
	public static final String DRIVER = "org.h2.Driver";
//	public static final String URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MVCC=TRUE";
	public static final String URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1";
	public static final String USER = "sa";
	public static final String PASS = "";

	public static Properties getConnectionProviderProperties(String dbName) {
		Properties props = new Properties( null );
		props.put( Environment.DRIVER, DRIVER );
		props.put( Environment.URL, String.format( URL, dbName ) );
		props.put( Environment.USER, USER );
		props.put( Environment.PASS, PASS );
		return props;
	}

	public static Properties getConnectionProviderProperties() {
		return getConnectionProviderProperties( "db1" );
	}

	public static DriverManagerConnectionProviderImpl buildConnectionProvider() {
		return buildConnectionProvider( false );
	}

	public static DriverManagerConnectionProviderImpl buildConnectionProvider(String dbName) {
		return buildConnectionProvider( getConnectionProviderProperties( dbName ), false );
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
