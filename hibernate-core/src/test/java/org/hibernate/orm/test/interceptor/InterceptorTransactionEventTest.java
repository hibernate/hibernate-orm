/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests, whether {@link Interceptor} gets the transaction events
 */
public class InterceptorTransactionEventTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testTransactionEvents() {
		LoggingInterceptor interceptor = new LoggingInterceptor();

		Session s = openSession(interceptor);
		Transaction tx = s.beginTransaction();
		// Do nothing, open and closing the transaction is enough
		tx.commit();
		s.close();

		assertTrue("afterTransactionBeginCalled not called", interceptor.isAfterTransactionBeginCalled());
		assertTrue("afterTransactionCompletionCalled not called", interceptor.isAfterTransactionCompletionCalled());
		assertTrue("beforeTransactionCompletionCalled not called", interceptor.isBeforeTransactionCompletionCalled());
	}

	private static class LoggingInterceptor implements Interceptor {
		private boolean afterTransactionBeginCalled;
		private boolean afterTransactionCompletionCalled;
		private boolean beforeTransactionCompletionCalled;

		@Override
		public void afterTransactionBegin(Transaction tx) {
			afterTransactionBeginCalled = true;
		}

		@Override
		public void afterTransactionCompletion(Transaction tx) {
			afterTransactionCompletionCalled = true;
		}

		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			beforeTransactionCompletionCalled = true;
		}

		public boolean isAfterTransactionBeginCalled() {
			return afterTransactionBeginCalled;
		}

		public boolean isAfterTransactionCompletionCalled() {
			return afterTransactionCompletionCalled;
		}

		public boolean isBeforeTransactionCompletionCalled() {
			return beforeTransactionCompletionCalled;
		}

	}
}
