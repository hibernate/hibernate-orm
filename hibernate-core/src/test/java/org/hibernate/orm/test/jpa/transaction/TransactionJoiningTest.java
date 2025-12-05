/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import java.util.concurrent.CountDownLatch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TransactionRequiredException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.orm.test.jpa.txn.JtaTransactionJoiningTest;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.ExtraAssertions;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Largely a copy of {@link JtaTransactionJoiningTest}
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
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				)
		}
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
		assertFalse(
				JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ),
				"setup problem"
		);

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

		EntityManager entityManager = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		assertFalse( JtaStatusHelper.isActive( transactionManager ) );
		try {
			transactionManager.begin();
			entityManager = scope.getEntityManagerFactory().createEntityManager();
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

			transactionManager.commit();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( entityManager, transactionManager );
		}
	}

	@Test
	@JiraKey(value = "HHH-10807")
	public void testIsJoinedAfterMarkedForRollbackImplicit(EntityManagerFactoryScope scope) throws Exception {
		EntityManager entityManager = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		assertFalse( JtaStatusHelper.isActive( transactionManager ) );
		try {
			transactionManager.begin();
			entityManager = scope.getEntityManagerFactory().createEntityManager();
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


			transactionManager.rollback();
			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( entityManager, transactionManager );
		}
	}

	@Test
	@JiraKey(value = "HHH-10807")
	public void testIsJoinedAfterMarkedForRollbackExplicit(EntityManagerFactoryScope scope) throws Exception {
		EntityManager entityManager = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		assertFalse( JtaStatusHelper.isActive( transactionManager ) );
		try {
			entityManager = scope.getEntityManagerFactory()
					.createEntityManager( SynchronizationType.UNSYNCHRONIZED );
			SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );
			assertTrue( entityManager.isOpen() );
			assertTrue( session.isOpen() );

			ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

			transactionManager.begin();
			entityManager.joinTransaction();

			assertTrue( transactionCoordinator.isSynchronizationRegistered() );
			assertTrue( transactionCoordinator.isActive() );
			assertTrue( transactionCoordinator.isJoined() );

			transactionCoordinator.getTransactionDriverControl().markRollbackOnly();

			assertTrue( transactionCoordinator.isActive() );
			assertTrue( transactionCoordinator.isJoined() );
			assertTrue( entityManager.isJoinedToTransaction() );

			transactionManager.rollback();

			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( entityManager, transactionManager );
		}
	}

	@Test
	public void testCloseAfterCommit(EntityManagerFactoryScope scope) throws Exception {
		EntityManager entityManager = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		assertFalse( JtaStatusHelper.isActive( transactionManager ) );
		try {
			transactionManager.begin();
			entityManager = scope.getEntityManagerFactory().createEntityManager();
			SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

			ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

			assertTrue( transactionCoordinator.isSynchronizationRegistered() );
			assertTrue( transactionCoordinator.isActive() );
			assertTrue( transactionCoordinator.isJoined() );

			assertTrue( entityManager.isOpen() );
			assertTrue( session.isOpen() );

			transactionManager.commit();
			assertTrue( entityManager.isOpen() );
			assertTrue( session.isOpen() );

			entityManager.close();
			assertFalse( entityManager.isOpen() );
			assertFalse( session.isOpen() );
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( entityManager, transactionManager );
		}
	}

	@Test
	public void testImplicitJoiningWithExtraSynchronization(EntityManagerFactoryScope scope) throws Exception {
		EntityManager entityManager = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		assertFalse( JtaStatusHelper.isActive( transactionManager ) );
		try {
			transactionManager.begin();
			entityManager = scope.getEntityManagerFactory().createEntityManager();
			SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

			ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

			assertTrue( transactionCoordinator.isSynchronizationRegistered() );
			assertTrue( transactionCoordinator.isActive() );
			assertTrue( transactionCoordinator.isJoined() );

			entityManager.close();

			transactionManager.commit();
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( entityManager, transactionManager );
		}
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
	@JiraKey(value = "HHH-7910")
	public void testMultiThreadTransactionTimeout(EntityManagerFactoryScope scope) throws Exception {
		EntityManager em = null;
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();

			em = scope.getEntityManagerFactory().createEntityManager();

			final SessionImplementor sImpl = em.unwrap( SessionImplementor.class );

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
				caught = e.getClass().equals( HibernateException.class );
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

			transactionManager.rollback();
		}
		finally {
			rollbackActiveTransacionrAndCloseEntityManager( em, transactionManager );
		}
	}

	private void rollbackActiveTransacionrAndCloseEntityManager(
			EntityManager em,
			TransactionManager transactionManager) {
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
		// ensure the entityManager is closed in case the rollback call fails
		if ( em != null && em.isOpen() ) {
			em.close();
		}
	}
}
