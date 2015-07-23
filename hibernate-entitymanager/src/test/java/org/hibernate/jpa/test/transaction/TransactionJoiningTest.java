/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Status;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Largely a copy of {@link org.hibernate.test.jpa.txn.JtaTransactionJoiningTest}
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

		EntityManager entityManager = entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED );
		TransactionJoinHandlingChecker.validateExplicitJoiningHandling( entityManager );
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testExplicitJoiningTransactionRequiredException() throws Exception {
		// explicitly calling EntityManager#joinTransaction outside of an active transaction should cause
		// a TransactionRequiredException to be thrown

		EntityManager entityManager = entityManagerFactory().createEntityManager();
		assertFalse("setup problem", JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()));

		try {
			entityManager.joinTransaction();
			fail( "Expected joinTransaction() to fail since there is no active JTA transaction" );
		}
		catch (TransactionRequiredException expected) {
		}
	}

	@Test
	public void testImplicitJoining() throws Exception {
		// here the transaction is started before the EM is opened...

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

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

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

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

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

		entityManager.close();

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
				((JtaTransactionCoordinatorImpl)sImpl.getTransactionCoordinator()).getSynchronizationCallbackCoordinator()
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
			// HHH-9312
			caught = true;
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
