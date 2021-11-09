/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.tm;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.internal.SessionImpl;
import org.junit.Test;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

/**
 * @author Chris Cranford
 */
public class SessionFactoryInterceptorTransactionTest extends BaseEnversJPAFunctionalTestCase {

    private TestInterceptor interceptor;
    private TransactionManager tm;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { StrTestEntity.class };
    }

    @Override
    protected void addConfigOptions(Map options) {
        super.addConfigOptions( options );

        TestInterceptor.reset();

        this.interceptor = new TestInterceptor();
        options.put( AvailableSettings.INTERCEPTOR, interceptor );
        options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, true );

        TestingJtaBootstrap.prepare( options );
        tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
    }

    @Test
    @Priority(10)
    public void initData() throws Exception {
        // Revision 1
        EntityManager em = getEntityManager();
        // Explicitly use manual flush to trigger separate temporary session write via Envers
        em.unwrap( SessionImpl.class ).setHibernateFlushMode( FlushMode.MANUAL );
        tm.begin();
        StrTestEntity entity = new StrTestEntity( "Test" );
        em.persist( entity );
        em.flush();
        tm.commit();
    }

    @Test
    public void testInterceptorInvocations() throws Exception {
        // Expect the interceptor to have been created once and invoked twice, once for the original session
        // and follow-up for the Envers temporary session.
        final Map<TestInterceptor, Integer> invocationMap = TestInterceptor.getBeforeCompletionCallbacks();
        assertEquals( 1, invocationMap.size() );
        assertEquals( invocationMap.values().stream().filter( v -> v == 2 ).count(), 1 );
    }
}
