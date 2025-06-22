/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "JtdsConnection.isValid not implemented")
public class C3P0ConnectionProviderTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void releaseSessionFactory() {
		super.releaseSessionFactory();
		try {
			//c3p0 does not close physical connections right away, so without this hack a connection leak false alarm is triggered.
			Thread.sleep( 100 );
		}
		catch ( InterruptedException e ) {
		}
	}

	@Test
	public void testC3P0isDefaultWhenThereIsC3P0Properties() {
		JdbcServices jdbcServices = serviceRegistry().requireService( JdbcServices.class );
		ConnectionProviderJdbcConnectionAccess connectionAccess =
			assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
			);
		assertTrue( connectionAccess.getConnectionProvider() instanceof C3P0ConnectionProvider );
	}

	@Test
	public void testHHH6635() throws Exception {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectName> set = mBeanServer.queryNames( null, null );
		boolean mbeanfound = false;
		for ( ObjectName obj : set ) {
			if ( obj.getKeyPropertyListString().indexOf( "PooledDataSource" ) > 0 ) {
				mbeanfound = true;

				// see according c3p0 settings in META-INF/persistence.xml

				int actual_minPoolSize = (Integer) mBeanServer.getAttribute( obj, "minPoolSize" );
				assertEquals( 0, actual_minPoolSize );

				int actual_initialPoolSize = (Integer) mBeanServer.getAttribute( obj, "initialPoolSize" );
				assertEquals( 0, actual_initialPoolSize );

				int actual_maxPoolSize = (Integer) mBeanServer.getAttribute( obj, "maxPoolSize" );
				assertEquals( 800, actual_maxPoolSize );

				int actual_maxStatements = (Integer) mBeanServer.getAttribute( obj, "maxStatements" );
				assertEquals( 50, actual_maxStatements );

				int actual_maxIdleTime = (Integer) mBeanServer.getAttribute( obj, "maxIdleTime" );
				assertEquals( 300, actual_maxIdleTime );

				int actual_idleConnectionTestPeriod = (Integer) mBeanServer.getAttribute(
						obj,
						"idleConnectionTestPeriod"
				);
				assertEquals( 3000, actual_idleConnectionTestPeriod );
				break;
			}
		}

		assertTrue( "PooledDataSource BMean not found, please verify version of c3p0", mbeanfound );
	}

	@Test @JiraKey(value="HHH-9498")
	public void testIsolationPropertyCouldBeEmpty() {
		C3P0ConnectionProvider provider = new C3P0ConnectionProvider();
		try {
			Map<String,Object> configuration = new HashMap<>();
			configuration.put( Environment.ISOLATION, "" );
			provider.configure( configuration );
		}
		finally {
			provider.stop();
		}
	}
}
