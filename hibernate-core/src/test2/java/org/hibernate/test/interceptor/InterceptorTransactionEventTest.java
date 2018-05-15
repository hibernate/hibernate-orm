/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.test.interceptor;

import org.hibernate.EmptyInterceptor;
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
	public void testTransactionEvents() throws Exception {
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

    private static class LoggingInterceptor extends EmptyInterceptor {
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
