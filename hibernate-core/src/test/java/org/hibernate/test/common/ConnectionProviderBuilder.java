/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.common;

import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ConnectionProviderBuilder {
	public static final String DRIVER = "org.h2.Driver";
	public static final String URL = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE";
	public static final String USER = "sa";
	public static final String PASS = "";

	public static Properties getConnectionProviderProperties() {
		Properties props = new Properties( null );
		props.put( Environment.DRIVER, DRIVER );
		props.put( Environment.URL, URL );
		props.put( Environment.USER, USER );
		return props;
	}

	public static ConnectionProvider buildConnectionProvider() {
		return buildConnectionProvider( false );
	}

	public static ConnectionProvider buildConnectionProvider(final boolean allowAggressiveRelease) {
		final Properties props = getConnectionProviderProperties();
		DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl() {
			public boolean supportsAggressiveRelease() {
				return allowAggressiveRelease;
			}
		};
		connectionProvider.configure( props );
		return connectionProvider;
	}

	public static Dialect getCorrespondingDialect() {
		return new H2Dialect();
	}
}
