/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import java.util.concurrent.CountDownLatch;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Status;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.test.transaction.Book;
import org.hibernate.jpa.test.transaction.TransactionJoinHandlingChecker;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Largely a copy of {@link org.hibernate.test.jpa.txn.JtaTransactionJoiningTest}
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				Book.class
		},
		integrationSettings = {
				@Setting(name = org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting(name = org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA")
		},
		nonStringValueSettingProviders = { JtaPlatformNonStringValueSettingProvider.class }
)
public class TransactionJoiningTest {

	@Test
	public void testExplicitJoining(EntityManagerFactoryScope scope) throws Exception {
			assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

			EntityManager entityManager = scope.getEntityManagerFactory()
					.createEntityManager( SynchronizationType.UNSYNCHRONIZED );
		try {
			TransactionJoinHandlingChecker.validateExplicitJoiningHandling( entityManager );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testExplicitJoiningTransactionRequiredException(EntityManagerFactoryScope scope) {
		// explicitly calling EntityManager#joinTransaction outside of an active transaction should cause
		// a TransactionRequiredException to be thrown

		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ), "setup problem" );

		try {
			Assertions.assertThrows(
					TransactionRequiredException.class,
					entityManager::joinTransaction,
					"Expected joinTransaction() to fail since there is no active JTA transaction"
			);
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testImplicitJoining(EntityManagerFactoryScope scope) throws Exception {
		// here the transaction is started before the EM is opened...

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
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

	@Test
	@TestForIssue(jiraKey = "HHH-10807")
	public void testIsJoinedAfterMarkedForRollbackImplicit(EntityManagerFactoryScope scope) throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );
		transactionCoordinator.getTransactionDriverControl().markRollbackOnly();

		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );
		assertTrue( entityManager.isJoinedToTransaction() );

		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			// ensure the entityManager is closed in case the rollback call fails
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10807")
	public void testIsJoinedAfterMarkedForRollbackExplicit(EntityManagerFactoryScope scope) throws Exception {

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED );
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );
		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		entityManager.joinTransaction();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

		transactionCoordinator.getTransactionDriverControl().markRollbackOnly();

		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );
		assertTrue( entityManager.isJoinedToTransaction() );

		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();

			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			// ensure the entityManager is closed in case the rollback call fails
			entityManager.close();
		}
	}

	@Test
	public void testCloseAfterCommit(EntityManagerFactoryScope scope) throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

		assertTrue( entityManager.isOpen() );
		assertTrue( session.isOpen() );
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			assertTrue( entityManager.isOpen() );
			assertTrue( session.isOpen() );

			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			// ensure the entityManager is closed in case the commit call fails
			entityManager.close();
		}
	}

	@Test
	public void testImplicitJoiningWithExtraSynchronization(EntityManagerFactoryScope scope) throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

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
	public void testMultiThreadTransactionTimeout(EntityManagerFactoryScope scope) throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		try {
			final SessionImpl sImpl = em.unwrap( SessionImpl.class );

			final CountDownLatch latch = new CountDownLatch( 1 );

			Thread thread = new Thread( () -> {
				( (JtaTransactionCoordinatorImpl) sImpl.getTransactionCoordinator() ).getSynchronizationCallbackCoordinator()
						.afterCompletion( Status.STATUS_ROLLEDBACK );
				latch.countDown();
			} );
			thread.start();

			latch.await();

			boolean caught = false;
			try {
				em.persist( new Book( "The Book of Foo", 1 ) );
			}
			catch (PersistenceException e) {
				caught = e.getCause().getClass().equals( HibernateException.class );
			}
			assertTrue( caught );

			// Ensure that the connection was closed by the background thread.
			caught = false;
			try {
				em.createQuery( "from Book" ).getResultList();
			}
			catch (PersistenceException e) {
				// HHH-9312
				caught = true;
			}
			catch (Exception e) {
				caught = true;
			}
			assertTrue( caught );

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		}
		finally {
			em.close();
		}
	}
}
