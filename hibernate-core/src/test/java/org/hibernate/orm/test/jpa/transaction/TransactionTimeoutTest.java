/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import java.math.BigDecimal;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;

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
				@Setting(name = org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting(name = org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA")
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				)
		})
@JiraKey("HHH-10619")
public class TransactionTimeoutTest {

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testH2(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "create alias sleep for \"" + TransactionTimeoutTest.class.getName() + ".sleep\"" )
					.executeUpdate();
			entityManager.createNativeQuery( "select sleep(10000)", Long.class ).getResultList();
		});
	}

	@Test
	@RequiresDialect(HSQLDialect.class)
	public void testHSQL(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "create function sleep(secs bigint) returns bigint no sql language java parameter style java deterministic external name 'CLASSPATH:" + TransactionTimeoutTest.class.getName() + ".sleep'" )
					.executeUpdate();
			entityManager.createNativeQuery( "select sleep(10000) from (values (0)) d" ).getResultList();
		});
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(CockroachDialect.class)
	public void testPostgreSQL(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "select pg_sleep(10)" ).getResultList();
		});
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	public void testMySQL(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "select sleep(10)" ).getResultList();
		});
	}

	@Test
	@RequiresDialect(OracleDialect.class)
	public void testOracle(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createStoredProcedureQuery( "DBMS_SESSION.SLEEP" )
					.registerStoredProcedureParameter( 1, BigDecimal.class, ParameterMode.IN )
					.setParameter( 1, BigDecimal.valueOf( 10L ) )
					.execute();
		});
	}

	@Test
	@RequiresDialect(AbstractTransactSQLDialect.class)
	public void testTSQL(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createNativeQuery( "waitfor delay '00:00:10'" ).getResultList();
		});
	}

	@Test
	@RequiresDialect(value = DB2Dialect.class, majorVersion = 11, comment = "DBMS_LOCK.SLEEP is only supported since 11")
	public void testDB2(EntityManagerFactoryScope scope) throws Throwable {
		test( scope, entityManager -> {
			entityManager.createStoredProcedureQuery( "DBMS_LOCK.SLEEP" )
					.registerStoredProcedureParameter( 1, BigDecimal.class, ParameterMode.IN )
					.setParameter( 1, BigDecimal.valueOf( 10L ) )
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

	public static long sleep(long millis) throws InterruptedException {
		long start = System.currentTimeMillis();
		Thread.sleep( millis );
		return System.currentTimeMillis() - start;
	}

	public static void sleep(long millis, long[] slept) throws InterruptedException {
		slept[0] = sleep(millis);
	}

}
