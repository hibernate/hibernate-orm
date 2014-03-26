/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.connection;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Collections;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverConnectionCreator;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.Service;
import org.hibernate.service.internal.ProvidedService;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class ConnectionCreatorTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-8621" )
	public void testBadUrl() throws Exception {
		DriverConnectionCreator connectionCreator = new DriverConnectionCreator(
				(Driver) Class.forName( "org.h2.Driver" ).newInstance(),
				new StandardServiceRegistryImpl(
						true,
						new BootstrapServiceRegistryImpl(),
						Collections.<StandardServiceInitiator>emptyList(),
						Collections.<ProvidedService>emptyList(),
						Collections.emptyMap()
				) {
					@Override
					@SuppressWarnings("unchecked")
					public <R extends Service> R getService(Class<R> serviceRole) {
						if ( JdbcServices.class.equals( serviceRole ) ) {
							// return a new, not fully initialized JdbcServicesImpl
							return (R) new JdbcServicesImpl();
						}
						return super.getService( serviceRole );
					}
				},
				"jdbc:h2:mem:test-bad-urls;nosuchparam=saywhat",
				new Properties(),
				false,
				null
		);

		try {
			Connection conn = connectionCreator.createConnection();
			conn.close();
			fail( "Expecting the bad Connection URL to cause an exception" );
		}
		catch (JDBCConnectionException expected) {
		}
	}
}
