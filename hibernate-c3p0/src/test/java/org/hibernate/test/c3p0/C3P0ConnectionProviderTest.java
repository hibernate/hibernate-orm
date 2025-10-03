/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Strong Liu
 */
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "JtdsConnection.isValid not implemented")
@ServiceRegistry
@DomainModel
@SessionFactory
public class C3P0ConnectionProviderTest {
	@Test
	public void testC3P0isDefaultWhenThereIsC3P0Properties(ServiceRegistryScope registryScope) throws Exception {
		var serviceRegistry = registryScope.getRegistry();
		var jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		var connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertThat( connectionAccess.getConnectionProvider() ).isInstanceOf( C3P0ConnectionProvider.class );
	}

	@Test
	public void testHHH6635(SessionFactoryScope factoryScope) throws Exception {
		var sf = factoryScope.getSessionFactory();
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		Set<ObjectName> set = mBeanServer.queryNames( null, null );
		boolean mbeanfound = false;
		for ( ObjectName obj : set ) {
			if ( obj.getKeyPropertyListString().indexOf( "PooledDataSource" ) > 0 ) {
				mbeanfound = true;

				// see according c3p0 settings in META-INF/persistence.xml

				int actual_minPoolSize = (Integer) mBeanServer.getAttribute( obj, "minPoolSize" );
				assertThat( actual_minPoolSize ).isEqualTo( 0 );

				int actual_initialPoolSize = (Integer) mBeanServer.getAttribute( obj, "initialPoolSize" );
				assertThat( actual_initialPoolSize ).isEqualTo( 0 );

				int actual_maxPoolSize = (Integer) mBeanServer.getAttribute( obj, "maxPoolSize" );
				assertThat( actual_maxPoolSize ).isEqualTo( 800 );

				int actual_maxStatements = (Integer) mBeanServer.getAttribute( obj, "maxStatements" );
				assertThat( actual_maxStatements ).isEqualTo( 50 );

				int actual_maxIdleTime = (Integer) mBeanServer.getAttribute( obj, "maxIdleTime" );
				assertThat( actual_maxIdleTime ).isEqualTo( 300 );

				int actual_idleConnectionTestPeriod = (Integer) mBeanServer.getAttribute(
						obj,
						"idleConnectionTestPeriod"
				);
				assertThat( actual_idleConnectionTestPeriod ).isEqualTo( 3000 );

				break;
			}
		}

		assertThat( mbeanfound ).as( "PooledDataSource BMean not found, please verify version of c3p0" ).isTrue();
	}
}
