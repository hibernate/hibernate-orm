/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.transaction;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransaction;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

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
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		assertFalse( session.getTransactionCoordinator().isSynchronizationRegistered() );
		Transaction hibernateTransaction = ((Session) session).getTransaction();
		ExtraAssertions.assertTyping( CMTTransaction.class, hibernateTransaction );
		assertFalse( hibernateTransaction.isParticipating() );

		session.getFlushMode();
		assertFalse( session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertFalse( hibernateTransaction.isParticipating() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( hibernateTransaction.isActive() );
		assertFalse( hibernateTransaction.isParticipating() );
		assertFalse( session.getTransactionCoordinator().isSynchronizationRegistered() );

		session.getFlushMode();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( hibernateTransaction.isActive() );
		assertFalse( session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertFalse( hibernateTransaction.isParticipating() );

		entityManager.joinTransaction();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( hibernateTransaction.isActive() );
		assertTrue( session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertTrue( hibernateTransaction.isParticipating() );

		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );
		entityManager.close();
		assertFalse( entityManager.isOpen() );
		assertTrue( session.isOpen() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( entityManager.isOpen() );
		assertFalse( session.isOpen() );
	}
}