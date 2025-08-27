/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tm;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Transaction;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
public class JtaAfterCompletionTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		TestingJtaBootstrap.prepare( builder.getSettings() );
		builder.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		builder.applySetting( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@Test
	@JiraKey(value = "HHH-12448")
	public void testAfterCompletionCallbackExecutedAfterTransactionTimeout() throws Exception {
		// This makes sure that hbm2ddl runs before we start a transaction for a test
		// This is important for database that only support SNAPSHOT/SERIALIZABLE isolation,
		// because a test transaction still sees the state before the DDL executed
		final SessionFactoryImplementor sessionFactory = sessionFactory();

		// Set timeout to 5 seconds
		// Allows the reaper thread to abort our running thread for us
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setTransactionTimeout( 5 );

		// Begin the transaction
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		Session session = null;
		try {
			session = sessionFactory.openSession();

			SimpleEntity entity = new SimpleEntity( "Hello World" );
			session.persist( entity );

			// Register before and after callback handlers
			// The before causes the original thread to wait until Reaper aborts the transaction
			// The after tracks whether it is invoked since this test is to guarantee it is called
			final SessionImplementor sessionImplementor = (SessionImplementor) session;
			final ActionQueue actionQueue = sessionImplementor.getActionQueue();
			actionQueue.registerProcess( new AfterCallbackCompletionHandler() );
			actionQueue.registerProcess( new BeforeCallbackCompletionHandler() );

			TestingJtaPlatformImpl.transactionManager().commit();
		}
		catch (Exception e) {
			// This is expected
			assertTyping( RollbackException.class, e );
		}
		finally {
			try {
				if ( session != null ) {
					session.close();
				}
			}
			catch (HibernateException e) {
				// This is expected
				assertEquals( "Transaction was rolled back in a different thread", e.getMessage() );
			}

			// verify that the callback was fired.
			assertEquals( 1, AfterCallbackCompletionHandler.invoked );
		}
	}

	public static class BeforeCallbackCompletionHandler implements BeforeTransactionCompletionProcess {
		@Override
		public void doBeforeTransactionCompletion(SessionImplementor session) {
			try {
				// Wait for the transaction to be rolled back by the Reaper thread.
				final Transaction transaction = TestingJtaPlatformImpl.transactionManager().getTransaction();
				while ( transaction.getStatus() != Status.STATUS_ROLLEDBACK ) {
					Thread.sleep( 10 );
				}
			}
			catch (Exception e) {
				// we aren't concerned with this.
			}
		}
	}

	public static class AfterCallbackCompletionHandler implements AfterTransactionCompletionProcess {
		static int invoked = 0;

		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
			assertTrue( !success );
			invoked++;
		}
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		SimpleEntity() {

		}

		SimpleEntity(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


}
