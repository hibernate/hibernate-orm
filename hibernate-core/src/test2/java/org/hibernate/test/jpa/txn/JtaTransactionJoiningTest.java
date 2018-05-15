/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.txn;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.test.jpa.AbstractJPATest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class JtaTransactionJoiningTest extends AbstractJPATest {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		TestingJtaBootstrap.prepare( cfg.getProperties() );
		cfg.setProperty(
				AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
				JtaTransactionCoordinatorBuilderImpl.class.getName()
		);
	}

	@Test
	public void testExplicitJoining() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		SessionImplementor session = (SessionImplementor) sessionFactory().withOptions()
				.autoJoinTransactions( false )
				.openSession();

		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertFalse( transactionCoordinator.isJoined() );

		session.getFlushMode();  // causes a call to TransactionCoordinator#pulse

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
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isJoined() );

		transactionCoordinator.explicitJoin();
		session.getFlushMode();

		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isJtaTransactionCurrentlyActive() );
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isJoined() );

		((Session) session).close();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testImplicitJoining() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		SessionImplementor session = (SessionImplementor) sessionFactory().withOptions()
				.autoJoinTransactions( false )
				.openSession();

		session.getFlushMode();
	}

	@Test
	public void control() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		SessionImplementor session = (SessionImplementor) sessionFactory().openSession();
		ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
		JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		assertTrue( transactionCoordinator.isActive() );
		assertTrue( transactionCoordinator.isJoined() );

		( (Session) session ).close();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

}
