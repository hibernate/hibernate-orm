/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.transaction;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.jpa.test.transaction.Book;
import org.hibernate.jpa.test.transaction.Book_;
import org.hibernate.jpa.test.transaction.TransactionJoinHandlingChecker;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the JPA 2.1 added {@link SynchronizationType} handling.  {@link SynchronizationType#SYNCHRONIZED} is
 * the same as 2.0 behavior, so we do not explicitly test for that ({@link org.hibernate.orm.test.jpa.transaction.TransactionJoiningTest} handles it).
 * Tests here specifically test the {@link SynchronizationType#UNSYNCHRONIZED} behavior
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7451" )
@Jpa(
		annotatedClasses = {
				Book.class
		},
		integrationSettings = {
				@Setting(name = org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting(name = org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")
		},
		nonStringValueSettingProviders = { JtaPlatformNonStringValueSettingProvider.class }
)
public class SynchronizationTypeTest {

	@Test
	public void testUnSynchronizedExplicitJoinHandling(EntityManagerFactoryScope scope) throws Exception {
		// JPA 2.1 adds this notion allowing to open an EM using a specified "SynchronizationType".

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );
		TransactionJoinHandlingChecker.validateExplicitJoiningHandling( entityManager );
	}

	@Test
	public void testImplicitJoining(EntityManagerFactoryScope scope) throws Exception {
		// here the transaction is started before the EM is opened.  Because the SynchronizationType is UNSYNCHRONIZED
		// though, it should not auto join the transaction

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ), "setup problem" );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ), "setup problem" );

		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );
		SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertFalse( transactionCoordinator.isSynchronizationRegistered(), "EM was auto joined on creation" );
		assertTrue( transactionCoordinator.isActive(), "EM was auto joined on creation" );
		assertFalse( transactionCoordinator.isJoined(), "EM was auto joined on creation" );

		session.getFlushMode();
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertFalse( transactionCoordinator.isJoined() );

		entityManager.joinTransaction();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isActive() );
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
	@NotImplementedYet
	public void testDisallowedOperations(EntityManagerFactoryScope scope) throws Exception {
		// test calling operations that are disallowed while a UNSYNCHRONIZED persistence context is not
		// yet joined/enlisted

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ), "setup problem" );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ), "setup problem" );

		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );

		// explicit flushing
		assertThrows( TransactionRequiredException.class, entityManager::flush, "Expecting flush() call to fail" );

		// bulk operations
		assertThrows(
				TransactionRequiredException.class,
				() -> entityManager.createQuery( "delete Book" ).executeUpdate(),
				"Expecting executeUpdate() call to fail"
		);

		assertThrows(
				TransactionRequiredException.class,
				() -> entityManager.createQuery( "update Book set name = null" ).executeUpdate(),
				"Expecting executeUpdate() call to fail"
		);

		assertThrows(
				TransactionRequiredException.class,
				() -> {
					CriteriaDelete<Book> deleteCriteria = entityManager.getCriteriaBuilder().createCriteriaDelete( Book.class );
					deleteCriteria.from( Book.class );
					entityManager.createQuery( deleteCriteria ).executeUpdate();
				},
				"Expecting executeUpdate() call to fail"
		);

		assertThrows(
				TransactionRequiredException.class,
				() -> {
					CriteriaUpdate<Book> updateCriteria = entityManager.getCriteriaBuilder().createCriteriaUpdate( Book.class );
					updateCriteria.from( Book.class );
					updateCriteria.set( Book_.name, (String) null );
					entityManager.createQuery( updateCriteria ).executeUpdate();
				},
				"Expecting executeUpdate() call to fail"
		);

		assertThrows(
				TransactionRequiredException.class,
				() -> entityManager.createQuery( "select b from Book b" )
							.setLockMode( LockModeType.PESSIMISTIC_WRITE )
							.getResultList(),
				"Expecting attempted pessimistic lock query to fail"
		);

		entityManager.close();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
	}
}
