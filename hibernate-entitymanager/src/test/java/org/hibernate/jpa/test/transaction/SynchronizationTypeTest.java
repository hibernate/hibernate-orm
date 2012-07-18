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
import javax.persistence.LockModeType;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransaction;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests of the JPA 2.1 added {@link SynchronizationType} handling.  {@link SynchronizationType#SYNCHRONIZED} is
 * the same as 2.0 behavior, so we do not explicitly test for that ({@link TransactionJoiningTest} handles it).
 * Tests here specifically test the {@link SynchronizationType#UNSYNCHRONIZED} behavior
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7451" )
public class SynchronizationTypeTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_TYPE, "JTA" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Test
	public void testUnSynchronizedExplicitJoinHandling() throws Exception {
		// JPA 2.1 adds this notion allowing to open an EM using a specified "SynchronizationType".

		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		EntityManager entityManager = entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );
		TransactionJoinHandlingChecker.validateExplicitJoiningHandling( entityManager );
	}

	@Test
	public void testImplicitJoining() throws Exception {
		// here the transaction is started before the EM is opened.  Because the SynchronizationType is UNSYNCHRONIZED
		// though, it should not auto join the transaction

		assertFalse( "setup problem", JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( "setup problem", JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		EntityManager entityManager = entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );
		SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
		Transaction hibernateTransaction = ( (Session) session ).getTransaction();
		ExtraAssertions.assertTyping( CMTTransaction.class, hibernateTransaction );
		assertFalse( "EM was auto joined on creation", session.getTransactionCoordinator().isSynchronizationRegistered() );
		assertFalse( "EM was auto joined on creation", hibernateTransaction.isParticipating() );

		session.getFlushMode();
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

	@Test
	public void testDisallowedOperations() throws Exception {
		// test calling operations that are disallowed while a UNSYNCHRONIZED persistence context is not
		// yet joined/enlisted

		assertFalse( "setup problem", JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue(
				"setup problem", JtaStatusHelper.isActive(
				TestingJtaPlatformImpl.INSTANCE
						.getTransactionManager()
		)
		);

		EntityManager entityManager = entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, null );

		// explicit flushing
		try {
			entityManager.flush();
			fail( "Expecting flush() call to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		// bulk operations
		try {
			entityManager.createQuery( "delete Book" ).executeUpdate();
			fail( "Expecting executeUpdate() call to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		try	{
			entityManager.createQuery( "update Book set name = null" ).executeUpdate();
			fail( "Expecting executeUpdate() call to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		try	{
			CriteriaDelete<Book> deleteCriteria = entityManager.getCriteriaBuilder().createCriteriaDelete( Book.class );
			deleteCriteria.from( Book.class );
			entityManager.createQuery( deleteCriteria ).executeUpdate();
			fail( "Expecting executeUpdate() call to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		try {
			CriteriaUpdate<Book> updateCriteria = entityManager.getCriteriaBuilder().createCriteriaUpdate( Book.class );
			updateCriteria.from( Book.class );
			updateCriteria.set( Book_.name, (String) null );
			entityManager.createQuery( updateCriteria ).executeUpdate();
			fail( "Expecting executeUpdate() call to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		try {
			entityManager.createQuery( "select b from Book b" )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.getResultList();
			fail( "Expecting attempted pessimistic lock query to fail" );
		}
		catch (TransactionRequiredException expected) {
		}

		entityManager.close();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
	}
}
