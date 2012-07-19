/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
import javax.transaction.Synchronization;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransaction;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Largely a copy of {@link org.hibernate.test.jpa.txn.TransactionJoiningTest}
 *
 * @author Steve Ebersole
 */
public class TransactionJoiningTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_TYPE, "JTA" );
	}

	@Test
	public void testExplicitJoining() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		EntityManager entityManager = entityManagerFactory().createEntityManager();
		TransactionJoinHandlingChecker.validateExplicitJoiningHandling( entityManager );
	}

	@Test
	public void testImplicitJoining() throws Exception {
		// here the transaction is started before the EM is opened...

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		Transaction hibernateTransaction = ( (Session) session ).getTransaction();
		assertTrue( CMTTransaction.class.isInstance( hibernateTransaction ) );
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

	@Test
	public void testCloseAfterCommit() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		Transaction hibernateTransaction = ( (Session) session ).getTransaction();
		assertTrue( CMTTransaction.class.isInstance( hibernateTransaction ) );
		assertTrue( session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertTrue( hibernateTransaction.isParticipating() );

		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );

		entityManager.close();
		assertFalse( entityManager.isOpen() );
		assertFalse( session.isOpen() );
	}

	@Test
	public void testImplicitJoiningWithExtraSynchronization() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		Transaction hibernateTransaction = ( (Session) session ).getTransaction();
		assertTrue( CMTTransaction.class.isInstance( hibernateTransaction ) );
		assertTrue( session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertTrue( hibernateTransaction.isParticipating() );

		entityManager.close();

		hibernateTransaction.registerSynchronization(
				new Synchronization() {
					public void beforeCompletion() {
						// nothing to do
					}
					public void afterCompletion( int i ) {
						// nothing to do
					}
				}
		);
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}
}
