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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransaction;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

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
	
	/**
	 * In certain JTA environments (JBossTM, etc.), a background thread (reaper)
	 * can rollback a transaction if it times out.  These timeouts are rare and
	 * typically come from server failures.  However, we need to handle the
	 * multi-threaded nature of the transaction afterCompletion action.
	 * Emulate a timeout with a simple afterCompletion call in a thread.
	 * See HHH-7910
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-7910")
	public void testMultiThreadTransactionTimeout() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		EntityManager em = entityManagerFactory().createEntityManager();
		final SessionImpl sImpl = em.unwrap( SessionImpl.class );

		final CountDownLatch latch = new CountDownLatch( 1 );

		Thread thread = new Thread() {
			public void run() {
				sImpl.getTransactionCoordinator().getSynchronizationCallbackCoordinator()
						.afterCompletion( Status.STATUS_ROLLEDBACK );
				latch.countDown();
			}
		};
		thread.start();

		latch.await();

		boolean caught = false;
		try {
			em.persist( new Book( "The Book of Foo", 1 ) );
		}
		catch ( PersistenceException e ) {
			caught = e.getCause().getClass().equals( HibernateException.class );
		}
		assertTrue( caught );

		// Ensure that the connection was closed by the background thread.
		caught = false;
		try {
			em.createQuery( "from Book" ).getResultList();
		}
		catch ( PersistenceException e ) {
			caught = e.getCause().getClass().equals( GenericJDBCException.class );
		}
		assertTrue( caught );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}
}
