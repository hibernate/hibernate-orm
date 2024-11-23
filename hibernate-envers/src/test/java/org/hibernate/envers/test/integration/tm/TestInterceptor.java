/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.tm;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class TestInterceptor extends EmptyInterceptor {

    private static final Logger LOGGER = Logger.getLogger( TestInterceptor.class );
    private static Map<TestInterceptor, Integer> interceptorInvocations = new HashMap<>();

    public TestInterceptor() {
        interceptorInvocations.put( this, 0 );
    }

    @Override
    public void beforeTransactionCompletion(Transaction tx) {
        super.beforeTransactionCompletion(tx);
        interceptorInvocations.put( this, interceptorInvocations.get( this ) + 1 );
        LOGGER.info( "Interceptor beforeTransactionCompletion invoked" );
    }

    public static Map<TestInterceptor, Integer> getBeforeCompletionCallbacks() {
        return interceptorInvocations;
    }

    public static void reset() {
        interceptorInvocations.clear();
    }
}
