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
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.TransactionSettings.ALLOW_JTA_TRANSACTION_ACCESS;
import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = {
				@Setting(name=TRANSACTION_COORDINATOR_STRATEGY, value="jta"),
				@Setting(name=ALLOW_JTA_TRANSACTION_ACCESS, value="true"),
		},
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
@DomainModel(annotatedClasses = JtaAfterCompletionTest.SimpleEntity.class)
@SessionFactory
public class JtaAfterCompletionTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-12448")
	public void testAfterCompletionCallbackExecutedAfterTransactionTimeout(SessionFactoryScope factoryScope) throws Exception {
		// This makes sure that hbm2ddl runs before we start a transaction for a test
		// This is important for database that only support SNAPSHOT/SERIALIZABLE isolation,
		// because a test transaction still sees the state before the DDL executed
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		// Set timeout to 5 seconds
		// Allows the reaper thread to abort our running thread for us
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setTransactionTimeout( 5 );

		// Begin the transaction
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		try (SessionImplementor session = sessionFactory.openSession()) {
			try {
				SimpleEntity entity = new SimpleEntity( "Hello World" );
				session.persist( entity );

				// Register before and after callback handlers
				// The before causes the original thread to wait until Reaper aborts the transaction
				// The after tracks whether it is invoked since this test is to guarantee it is called
				final ActionQueue actionQueue = session.getActionQueue();
				actionQueue.registerCallback( new AfterCallbackCompletionHandler() );
				actionQueue.registerCallback( new BeforeCallbackCompletionHandler() );

				TestingJtaPlatformImpl.transactionManager().commit();
			}
			catch (Exception e) {
				// This is expected
				assertTyping( RollbackException.class, e );
			}
		}
		catch (HibernateException e) {
			// This is expected
			assertEquals( "Transaction was rolled back in a different thread", e.getMessage() );
		}
		finally {
			// verify that the callback was fired.
			assertEquals( 1, AfterCallbackCompletionHandler.invoked );
		}
	}

	public static class BeforeCallbackCompletionHandler implements BeforeTransactionCompletionProcess {
		@Override
		public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
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
			assertFalse( success );
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
