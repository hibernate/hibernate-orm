/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.txn;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.ExtraAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
public class JtaTransactionJoiningTest extends AbstractJPATest {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );
		TestingJtaBootstrap.prepare( builder.getSettings() );
		builder.applySetting(
				AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
				JtaTransactionCoordinatorBuilderImpl.class.getName()
		);
	}

	@Test
	public void testExplicitJoining() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		try (SessionImplementor session = (SessionImplementor) sessionFactory().withOptions()
				.autoJoinTransactions( false )
				.openSession()) {
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
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
	}

	@Test
	public void testImplicitJoining() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		try (SessionImplementor session = (SessionImplementor) sessionFactory().withOptions()
				.autoJoinTransactions( false )
				.openSession()) {
			session.getFlushMode();
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
	}

	@Test
	public void control() throws Exception {
		assertFalse( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );

		try (SessionImplementor session = (SessionImplementor) sessionFactory().openSession()) {
			ExtraAssertions.assertTyping( JtaTransactionCoordinatorImpl.class, session.getTransactionCoordinator() );
			JtaTransactionCoordinatorImpl transactionCoordinator = (JtaTransactionCoordinatorImpl) session.getTransactionCoordinator();

			assertTrue( transactionCoordinator.isSynchronizationRegistered() );
			assertTrue( transactionCoordinator.isActive() );
			assertTrue( transactionCoordinator.isJoined() );
		}
		finally {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
	}

}
