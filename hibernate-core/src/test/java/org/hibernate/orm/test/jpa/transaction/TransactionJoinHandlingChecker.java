/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.internal.SessionImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.ExtraAssertions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Helper for centralized transaction join checking
 *
 * @author Steve Ebersole
 */
public class TransactionJoinHandlingChecker {
	public static void validateExplicitJoiningHandling(EntityManager entityManager) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();

		try (SessionImpl session = entityManager.unwrap( SessionImpl.class )) {

			ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

			assertFalse( transactionCoordinator.isSynchronizationRegistered() );
			assertFalse( transactionCoordinator.isJtaTransactionCurrentlyActive() );
			assertFalse( transactionCoordinator.isJoined() );

			session.checkOpen();
			assertFalse( transactionCoordinator.isSynchronizationRegistered() );
			assertFalse( transactionCoordinator.isJtaTransactionCurrentlyActive() );
			assertFalse( transactionCoordinator.isJoined() );

			transactionManager.begin();
			assertTrue( JtaStatusHelper.isActive( transactionManager ) );
			assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
			assertFalse( transactionCoordinator.isJoined() );
			assertFalse( transactionCoordinator.isSynchronizationRegistered() );

			session.checkOpen();
			assertTrue( JtaStatusHelper.isActive( transactionManager ) );
			assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
			assertFalse( transactionCoordinator.isJoined() );
			assertFalse( transactionCoordinator.isSynchronizationRegistered() );

			entityManager.joinTransaction();
			assertTrue( JtaStatusHelper.isActive( transactionManager ) );
			assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
			assertTrue( transactionCoordinator.isSynchronizationRegistered() );
			assertTrue( transactionCoordinator.isJoined() );

			assertTrue( entityManager.isOpen() );
			assertTrue( session.isOpen() );
			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );

			transactionManager.commit();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		catch (Exception | AssertionError e){
			try {
				switch ( transactionManager.getStatus() ) {
					case Status.STATUS_ACTIVE:
					case Status.STATUS_MARKED_ROLLBACK:
						transactionManager.rollback();
				}
			}
			catch (Exception exception) {
				//ignore exception
			}
			throw e;
		}
	}
}
