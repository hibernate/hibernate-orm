/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.tm;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class TestInterceptor implements Interceptor {

	private static final Logger LOGGER = Logger.getLogger( TestInterceptor.class );
	private static Map<TestInterceptor, Integer> interceptorInvocations = new HashMap<>();

	public TestInterceptor() {
		interceptorInvocations.put( this, 0 );
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
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
