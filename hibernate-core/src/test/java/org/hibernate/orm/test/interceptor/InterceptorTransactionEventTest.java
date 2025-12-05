/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests, whether {@link Interceptor} gets the transaction events
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
public class InterceptorTransactionEventTest {

	@Test
	public void testTransactionEvents(SessionFactoryScope factoryScope) {
		LoggingInterceptor interceptor = new LoggingInterceptor();

		factoryScope.inTransaction(
				(sf) -> sf.withOptions().interceptor( interceptor ).openSession(),
				(s) -> {
					// Do nothing, open and closing the transaction is enough
				}
		);

		Assertions.assertTrue( interceptor.isAfterTransactionBeginCalled(), "afterTransactionBeginCalled not called" );
		Assertions.assertTrue( interceptor.isAfterTransactionCompletionCalled(),
				"afterTransactionCompletionCalled not called" );
		Assertions.assertTrue( interceptor.isBeforeTransactionCompletionCalled(),
				"beforeTransactionCompletionCalled not called" );
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
