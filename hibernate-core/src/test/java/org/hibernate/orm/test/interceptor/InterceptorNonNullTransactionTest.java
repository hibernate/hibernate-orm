/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.exceptionhandling.BaseJpaOrNativeBootstrapFunctionalTestCase;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NOTE: even though the Jira and commits imply that this test tests interceptors with JTA
 * it does not - it uses (has always used) JDBC transactions
 */
@ParameterizedClass
@MethodSource("parameters")
public class InterceptorNonNullTransactionTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	public enum JpaComplianceTransactionSetting { DEFAULT, TRUE, FALSE }
	public enum JtaAllowTransactionAccessSetting { DEFAULT, TRUE, FALSE }

	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.JPA, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.FALSE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.DEFAULT },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE },
				{ BootstrapMethod.NATIVE, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE },
		} );
	}

	private final JpaComplianceTransactionSetting jpaComplianceTransactionSetting;
	private final JtaAllowTransactionAccessSetting jtaAllowTransactionAccessSetting;

	public InterceptorNonNullTransactionTest(
			BootstrapMethod bootstrapMethod,
			JpaComplianceTransactionSetting jpaComplianceTransactionSetting,
			JtaAllowTransactionAccessSetting jtaAllowTransactionAccessSetting) {
		super( bootstrapMethod );
		this.jpaComplianceTransactionSetting = jpaComplianceTransactionSetting;
		this.jtaAllowTransactionAccessSetting = jtaAllowTransactionAccessSetting;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected void configure(Map<String, Object> properties) {
		super.configure( properties );

		switch ( jpaComplianceTransactionSetting ) {
			case DEFAULT:
				// Keep the default (false)
				break;
			case TRUE:
				properties.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
				break;
			case FALSE:
				properties.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "false" );
				break;

		}
		switch ( jtaAllowTransactionAccessSetting ) {
			case DEFAULT:
				// Keep the default (true native bootstrap; false if jpa bootstrap)
				break;
			case TRUE:
				properties.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
				break;
			case FALSE:
				properties.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "false" );
				break;
		}
	}

	@Test
	public void testHibernateTransactionApi() {
		final var interceptor = new TransactionInterceptor();

		//noinspection resource
		var session = sessionFactory().withOptions().interceptor( interceptor ).openSession();

		session.getTransaction().begin();
		assertTrue( interceptor.afterTransactionBeginMethodCalled );
		assertTrue( interceptor.afterTransactionBeginAssertionPassed );
		assertFalse( interceptor.beforeTransactionCompletionMethodCalled );
		assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
		assertFalse( interceptor.afterTransactionCompletionMethodCalled );
		assertNull( interceptor.afterTransactionCompletionAssertionPassed );


		SimpleEntity entity = new SimpleEntity( "Hello World" );
		session.persist( entity );

		interceptor.reset();
		session.getTransaction().commit();
		assertFalse( interceptor.afterTransactionBeginMethodCalled );
		assertNull( interceptor.afterTransactionBeginAssertionPassed );
		assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
		assertTrue( interceptor.afterTransactionCompletionMethodCalled );
		assertTrue( interceptor.beforeTransactionCompletionAssertionPassed );
		assertTrue( interceptor.afterTransactionCompletionAssertionPassed );

		session.close();
	}

	@Test
	public void testJtaApiWithSharedTransactionCoordinator() {
		final var interceptor = new TransactionInterceptor();

		// NOTE: the test "cheats" in that the interceptor passed during the creation of the
		// child Session is irrelevant; the relevant bit is the passing of said interceptor
		// to the creation of the parent session

		//noinspection resource
		try (var originalSession = sessionFactory().withOptions().interceptor( interceptor ).openSession()) {
			try (var session =  originalSession.sessionWithOptions().interceptor( interceptor ).connection().openSession()) {
				interceptor.reset();

				session.getTransaction().begin();

				assertTrue( interceptor.afterTransactionBeginMethodCalled );
				assertTrue( interceptor.afterTransactionBeginAssertionPassed );
				assertFalse( interceptor.beforeTransactionCompletionMethodCalled );
				assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
				assertFalse( interceptor.afterTransactionCompletionMethodCalled );
				assertNull( interceptor.afterTransactionCompletionAssertionPassed );

				SimpleEntity entity = new SimpleEntity( "Hello World" );
				session.persist( entity );

				interceptor.reset();
				session.getTransaction().commit();

				assertFalse( interceptor.afterTransactionBeginMethodCalled );
				assertNull( interceptor.afterTransactionBeginAssertionPassed );
				assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
				assertTrue( interceptor.afterTransactionCompletionMethodCalled );
				assertTrue( interceptor.beforeTransactionCompletionAssertionPassed );
				assertTrue( interceptor.afterTransactionCompletionAssertionPassed );
			}
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

	private static class TransactionInterceptor implements Interceptor {
		private boolean afterTransactionBeginMethodCalled;
		private Boolean afterTransactionBeginAssertionPassed;


		private boolean beforeTransactionCompletionMethodCalled;
		private Boolean beforeTransactionCompletionAssertionPassed;

		private boolean afterTransactionCompletionMethodCalled;
		private Boolean afterTransactionCompletionAssertionPassed;

		public void reset() {
			afterTransactionBeginMethodCalled = false;
			afterTransactionBeginAssertionPassed = null;
			beforeTransactionCompletionMethodCalled = false;
			beforeTransactionCompletionAssertionPassed = null;
			afterTransactionCompletionMethodCalled = false;
			afterTransactionCompletionAssertionPassed = null;
		}

		@Override
		public void afterTransactionBegin(org.hibernate.Transaction tx) {
			afterTransactionBeginMethodCalled = true;
			if ( tx != null ) {
				afterTransactionBeginAssertionPassed = false;
				assertEquals( TransactionStatus.ACTIVE, tx.getStatus() );
				afterTransactionBeginAssertionPassed = true;
			}
		}
		@Override
		public void beforeTransactionCompletion(org.hibernate.Transaction tx) {
			beforeTransactionCompletionMethodCalled = true;
			if ( tx != null ) {
				beforeTransactionCompletionAssertionPassed = false;
				assertEquals( TransactionStatus.ACTIVE, tx.getStatus() );
				beforeTransactionCompletionAssertionPassed = true;
			}
		}
		@Override
		public void afterTransactionCompletion(org.hibernate.Transaction tx) {
			afterTransactionCompletionMethodCalled = true;
			if ( tx != null ) {
				afterTransactionCompletionAssertionPassed = false;
				assertEquals( TransactionStatus.COMMITTED, tx.getStatus() );
				afterTransactionCompletionAssertionPassed = true;
			}
		}
	};
}
