/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;


import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;

import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryTimeoutException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(
		integrationSettings = {
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA")
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				)
		})
@JiraKey("HHH-10619")
public class DerbyTransactionTimeoutTest {

	@Test
	@RequiresDialect(DerbyDialect.class)
	public void testDerby(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "create procedure sleep(in secs bigint, out slept bigint) no sql language java parameter style java deterministic external name '" + DerbyTransactionTimeoutTest.class.getName() + ".sleep'" )
					.executeUpdate();
			entityManager.createStoredProcedureQuery( "sleep" )
					.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, Long.class, ParameterMode.OUT )
					.setParameter( 1, 10_000L )
					.execute();
		});
	}

	private void test(EntityManagerFactoryScope scope, ThrowingConsumer<EntityManager> consumer) throws Throwable {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();

		transactionManager.setTransactionTimeout( 2 );
		transactionManager.begin();

		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			entityManager.joinTransaction();
			consumer.accept( entityManager );
		}
		catch (QueryTimeoutException | LockTimeoutException ex) {
			// This is fine
		}
		catch (HibernateException ex) {
			assertThat( ex.getMessage() ).isEqualTo( "Transaction was rolled back in a different thread" );
		}
		finally {
			assertThat( transactionManager.getStatus() ).isIn( Status.STATUS_ROLLEDBACK, Status.STATUS_ROLLING_BACK, Status.STATUS_MARKED_ROLLBACK );
			transactionManager.rollback();
		}
	}

}
