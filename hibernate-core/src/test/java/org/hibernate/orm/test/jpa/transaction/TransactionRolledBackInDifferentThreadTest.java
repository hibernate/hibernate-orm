/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import jakarta.persistence.EntityManager;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Recreate test failure that occurs when three threads share the same entity manager and
 * one of them calls set rollback only on its transaction.
 *
 * @author Scott Marlow
 */
@Jpa(
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
public class TransactionRolledBackInDifferentThreadTest {

	@Test
	public void testTransactionRolledBackInDifferentThreadFailure(EntityManagerFactoryScope scope) throws Exception {

		/*
		 * The three test threads share the same entity manager.
		 * The main test thread creates an EntityManager, joins it to the transaction and ends the transaction.
		 * Test thread 1 joins the EntityManager to its transaction, sets rollbackonly and ends the transaction.
		 * Test thread 2 attempts to join the EntityManager to its transaction but will fail with a
		 *   HibernateException("Transaction was rolled back in a different thread!")
		 */

		// main test thread

		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		transactionManager.begin();
		final EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		try {
			em.joinTransaction();

			transactionManager.commit();

			// will be set to the failing exception
			final HibernateException[] transactionRolledBackInDifferentThreadException = new HibernateException[2];
			transactionRolledBackInDifferentThreadException[0] = transactionRolledBackInDifferentThreadException[1] = null;

			// background test thread 1
			final Runnable run1 = () -> {
				try {
					transactionManager.begin();
					em.joinTransaction();
					transactionManager.setRollbackOnly();
					transactionManager.commit();
				}
				catch (jakarta.persistence.PersistenceException e) {
					if ( e.getCause() instanceof HibernateException &&
							e.getCause().getMessage().equals( "Transaction was rolled back in a different thread!" ) ) {
						/*
						 * Save the exception for the main test thread to fail
						 */
						e.printStackTrace();    // show the error first
						transactionRolledBackInDifferentThreadException[0] = (HibernateException) e.getCause();
					}
				}
				catch (RollbackException ignored) {
					// expected to see RollbackException: ARJUNA016053: Could not commit transaction.
				}
				catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				finally {
					try {
						if ( transactionManager
								.getStatus() != Status.STATUS_NO_TRANSACTION ) {
							transactionManager.rollback();
						}
					}
					catch (SystemException ignore) {
					}
				}
			};

			// test thread 2
			final Runnable run2 = () -> {
				try {
					transactionManager.begin();
					/*
					 * the following call to em.joinTransaction() will throw:
					 *   org.hibernate.HibernateException: Transaction was rolled back in a different thread!
					 */
					em.joinTransaction();
					transactionManager.commit();
				}
				catch (jakarta.persistence.PersistenceException e) {
					if ( e.getCause() instanceof HibernateException &&
							e.getCause().getMessage().equals( "Transaction was rolled back in a different thread!" ) ) {
						/*
						 * Save the exception for the main test thread to fail
						 */
						e.printStackTrace();    // show the error first
						transactionRolledBackInDifferentThreadException[1] = (HibernateException) e.getCause();
					}
				}
				catch (Throwable throwable) {
					throwable.printStackTrace();
				}
				finally {
					try {
						if ( transactionManager
								.getStatus() != Status.STATUS_NO_TRANSACTION ) {
							transactionManager.rollback();
						}
					}
					catch (SystemException ignore) {
					}
				}
			};

			Thread thread = new Thread( run1, "test thread1" );
			thread.start();
			thread.join();

			Thread thread2 = new Thread( run2, "test thread2" );
			thread2.start();
			thread2.join();

			// show failure for exception caught in run2.run()
			if ( transactionRolledBackInDifferentThreadException[0] != null
					|| transactionRolledBackInDifferentThreadException[1] != null ) {
				fail(
						"failure in test thread 1 = " +
								( transactionRolledBackInDifferentThreadException[0] != null ?
										transactionRolledBackInDifferentThreadException[0].getMessage() :
										"(none)" )
								+ ", failure in test thread 2 = " +
								( transactionRolledBackInDifferentThreadException[1] != null ?
										transactionRolledBackInDifferentThreadException[1].getMessage() :
										"(none)" )
				);
			}
		}
		finally {
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
			em.close();
		}
	}
}
