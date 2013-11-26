/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	public static final String URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MVCC=TRUE";
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
