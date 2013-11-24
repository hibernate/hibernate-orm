/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.c3p0;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class C3P0ConnectionProviderTest extends BaseCoreFunctionalTestCase {

    @Test
    public void testC3P0isDefaultWhenThereIsC3P0Properties() {
        JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
        ConnectionProvider provider = jdbcServices.getConnectionProvider();
        assertTrue( provider instanceof C3P0ConnectionProvider );

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
}
