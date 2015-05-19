/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.c3p0;

import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class C3P0ConnectionProviderTest extends BaseCoreFunctionalTestCase {

    @Test
    public void testC3P0isDefaultWhenThereIsC3P0Properties() {
        JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
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
                assertEquals( 50, actual_minPoolSize );

                int actual_initialPoolSize = (Integer) mBeanServer.getAttribute( obj, "initialPoolSize" );
                assertEquals( 50, actual_initialPoolSize );

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

    @Test @TestForIssue(jiraKey="HHH-9498")
    public void testIsolationPropertyCouldBeEmpty() {
        Properties configuration = new Properties();
        configuration.setProperty( Environment.ISOLATION, "" );
        C3P0ConnectionProvider provider = new C3P0ConnectionProvider();
        provider.configure( configuration );
    }
}
