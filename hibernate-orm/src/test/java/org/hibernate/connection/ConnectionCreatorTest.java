/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
