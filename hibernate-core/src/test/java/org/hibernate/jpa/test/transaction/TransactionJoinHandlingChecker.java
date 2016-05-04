/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Helper for centralized transaction join checking
 *
 * @author Steve Ebersole
 */
public class TransactionJoinHandlingChecker {
	static void validateExplicitJoiningHandling(EntityManager entityManager) throws Exception {
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertFalse( transactionCoordinator.isJoined() );

		session.getFlushMode();
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertFalse( transactionCoordinator.isJoined() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertFalse( transactionCoordinator.isJoined() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );

		session.getFlushMode();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertFalse( transactionCoordinator.isJoined() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );

		entityManager.joinTransaction();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isJoined() );

		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );
		entityManager.close();
		assertFalse( entityManager.isOpen() );
		assertFalse( session.isOpen() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( entityManager.isOpen() );
		assertFalse( session.isOpen() );
	}
}