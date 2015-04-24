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
package org.hibernate.test.jpa.txn;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.junit.Test;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.test.jpa.AbstractJPATest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TransactionJoiningTest extends AbstractJPATest {
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
		assertFalse( transactionCoordinator.isActive() );
		assertFalse( transactionCoordinator.isJoined() );

		session.getFlushMode();  // causes a call to TransactionCoordinator#pulse

		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isActive() );
		assertFalse( transactionCoordinator.isJoined() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isActive() );
		assertFalse( transactionCoordinator.isJoined() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );

		session.getFlushMode();

		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isActive() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertFalse( transactionCoordinator.isJoined() );

		transactionCoordinator.explicitJoin();
		session.getFlushMode();

		assertTrue( JtaStatusHelper.isActive( TestingJtaPlatformImpl.INSTANCE.getTransactionManager() ) );
		assertTrue( transactionCoordinator.isActive() );
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
