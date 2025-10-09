/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.transactions;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
public class TransactionLifecycleTest {
	@Test
	void testCommit(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		assertFalse( session.getTransaction().wasStarted() );
		session.getTransaction().begin();
		assertTrue( session.getTransaction().wasStarted() );
		assertTrue( session.getTransaction().isActive() );
		session.getTransaction().runBeforeCompletion( () -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertFalse( session.getTransaction().isComplete() );
			assertTrue( session.getTransaction().isActive() );
		} );
		session.getTransaction().runAfterCompletion( status -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertTrue( session.getTransaction().isComplete() );
			assertFalse( session.getTransaction().isActive() );
			assertTrue( session.getTransaction().wasSuccessful() );
			assertFalse( session.getTransaction().wasFailure() );
		} );
		session.getTransaction().commit();
		assertTrue( session.getTransaction().wasStarted() );
		assertFalse( session.getTransaction().isInCompletionProcess() );
		assertTrue( session.getTransaction().isComplete() );
		assertFalse( session.getTransaction().isActive() );
		assertTrue( session.getTransaction().wasSuccessful() );
		assertFalse( session.getTransaction().wasFailure() );
		session.close();
	}

	@Test
	void testRollback(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		assertFalse( session.getTransaction().wasStarted() );
		session.getTransaction().begin();
		assertTrue( session.getTransaction().wasStarted() );
		assertTrue( session.getTransaction().isActive() );
		session.getTransaction().runBeforeCompletion( () -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertFalse( session.getTransaction().isComplete() );
			assertTrue( session.getTransaction().isActive() );
		} );
		session.getTransaction().runAfterCompletion( status -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertTrue( session.getTransaction().isComplete() );
			assertFalse( session.getTransaction().isActive() );
			assertFalse( session.getTransaction().wasSuccessful() );
			assertTrue( session.getTransaction().wasFailure() );
		} );
		session.getTransaction().rollback();
		assertTrue( session.getTransaction().wasStarted() );
		assertFalse( session.getTransaction().isInCompletionProcess() );
		assertTrue( session.getTransaction().isComplete() );
		assertFalse( session.getTransaction().isActive() );
		assertFalse( session.getTransaction().wasSuccessful() );
		assertTrue( session.getTransaction().wasFailure() );
		session.close();
	}

	@Test
	void testRollbackOnly(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().openSession();
		assertFalse( session.getTransaction().wasStarted() );
		session.getTransaction().begin();
		assertTrue( session.getTransaction().wasStarted() );
		assertTrue( session.getTransaction().isActive() );
		session.getTransaction().setRollbackOnly();
		session.getTransaction().runBeforeCompletion( () -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertFalse( session.getTransaction().isComplete() );
			assertTrue( session.getTransaction().isActive() );
			assertTrue( session.getTransaction().getRollbackOnly() );
		} );
		session.getTransaction().runAfterCompletion( status -> {
			assertTrue( session.getTransaction().wasStarted() );
			assertFalse( session.getTransaction().isInCompletionProcess() );
			assertTrue( session.getTransaction().isComplete() );
			assertFalse( session.getTransaction().isActive() );
			assertFalse( session.getTransaction().wasSuccessful() );
			assertTrue( session.getTransaction().wasFailure() );
		} );
		assertTrue( session.getTransaction().getRollbackOnly() );
		session.getTransaction().commit();
		assertTrue( session.getTransaction().wasStarted() );
		assertFalse( session.getTransaction().isInCompletionProcess() );
		assertTrue( session.getTransaction().isComplete() );
		assertFalse( session.getTransaction().isActive() );
		assertFalse( session.getTransaction().wasSuccessful() );
		assertTrue( session.getTransaction().wasFailure() );
		session.close();
	}
}
