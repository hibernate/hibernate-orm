/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.transaction.Status;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.hibernate.cfg.JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE;
import static org.hibernate.cfg.TransactionSettings.ALLOW_JTA_TRANSACTION_ACCESS;
import static org.hibernate.cfg.TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey( value = "HHH-13326")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = InterceptorJtaTransactionTest.SimpleEntity.class)
@SessionFactory
@ParameterizedClass
@MethodSource("options")
public class InterceptorJtaTransactionTest implements ServiceRegistryProducer {

	public record Options(
			boolean jpaBootstrap,
			JpaComplianceTransactionSetting jpaTransactionCompliance,
			JtaAllowTransactionAccessSetting allowJtaTransactionAccess) {
		void apply(StandardServiceRegistryBuilder registryBuilder) {
			registryBuilder.applySetting( JpaComplianceSettings.JPA_COMPLIANCE, jpaBootstrap );

			switch ( jpaTransactionCompliance ) {
				case DEFAULT:
					// Keep the default (false)
					break;
				case TRUE:
					registryBuilder.applySetting( JPA_TRANSACTION_COMPLIANCE, "true" );
					break;
				case FALSE:
					registryBuilder.applySetting( JPA_TRANSACTION_COMPLIANCE, "false" );
					break;

			}
			switch ( allowJtaTransactionAccess ) {
				case DEFAULT:
					// Keep the default (true native bootstrap; false if jpa bootstrap)
					break;
				case TRUE:
					registryBuilder.applySetting( ALLOW_JTA_TRANSACTION_ACCESS, "true" );
					break;
				case FALSE:
					registryBuilder.applySetting( ALLOW_JTA_TRANSACTION_ACCESS, "false" );
					break;
			}
		}
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		TestingJtaBootstrap.prepare( builder );
		builder.applySetting( TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		options.apply( builder );
		return builder.build();
	}

	public enum JpaComplianceTransactionSetting { DEFAULT, TRUE, FALSE }
	public enum JtaAllowTransactionAccessSetting {
		DEFAULT {
			@Override
			public boolean allowTransactionAccess(
					SessionFactoryImplementor sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				return !sessionFactory.getSessionFactoryOptions().isJpaBootstrap() ||
						jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE;
			}
		},
		TRUE {
			@Override
			public boolean allowTransactionAccess(
					SessionFactoryImplementor sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				return true;
			}
		},
		FALSE {
			@Override
			public boolean allowTransactionAccess(
					SessionFactoryImplementor sessionFactory,
					JpaComplianceTransactionSetting jpaComplianceTransactionSetting) {
				// setting is ignored if jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE
				return jpaComplianceTransactionSetting != JpaComplianceTransactionSetting.TRUE;
			}
		};

		public abstract boolean allowTransactionAccess(
				SessionFactoryImplementor sessionFactory,
				JpaComplianceTransactionSetting jpaComplianceTransactionSetting
		);
	};

	public static Iterable<Options> options() {
		return Arrays.asList(
				new Options( true, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT ),
				new Options( true, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT ),
				new Options( true, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE ),
				new Options( true, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE ),
				new Options( true, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE ),
				new Options( false, JpaComplianceTransactionSetting.DEFAULT, JtaAllowTransactionAccessSetting.DEFAULT ),
				new Options( false, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.DEFAULT ),
				new Options( false, JpaComplianceTransactionSetting.TRUE, JtaAllowTransactionAccessSetting.TRUE ),
				new Options( false, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.TRUE ),
				new Options( false, JpaComplianceTransactionSetting.FALSE, JtaAllowTransactionAccessSetting.FALSE )
		);
	}

	private final Options options;

	public InterceptorJtaTransactionTest(Options options) {
		this.options = options;
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testHibernateTransactionApi(SessionFactoryScope factoryScope) throws Exception {
		final TransactionInterceptor interceptor = new TransactionInterceptor();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		try (SessionImplementor session = sessionFactory.withOptions().interceptor( interceptor ).openSession()) {
			try {
				session.getTransaction().begin();
				if ( !options.allowJtaTransactionAccess.allowTransactionAccess(
						sessionFactory,
						options.jpaTransactionCompliance
				) ) {
					Assertions.fail( "IllegalStateException should have been thrown." );
				}
			}
			catch (IllegalStateException ex) {
				if ( TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getStatus() == Status.STATUS_ACTIVE ) {
					TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setRollbackOnly();
				}
				assertEquals( JpaComplianceTransactionSetting.TRUE, options.jpaTransactionCompliance );
			}

			// Interceptor#afterTransactionBegin is never called when using JTA
			assertFalse( interceptor.afterTransactionBeginMethodCalled );
			assertNull( interceptor.afterTransactionBeginAssertionPassed );
			assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
			assertNull( interceptor.afterTransactionCompletionAssertionPassed );

			SimpleEntity entity = new SimpleEntity( "Hello World" );
			session.persist( entity );

			interceptor.reset();

			session.getTransaction().commit();

			assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
			assertTrue( interceptor.afterTransactionCompletionMethodCalled );
			assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
			assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
			assertNull( interceptor.afterTransactionBeginAssertionPassed );
		}
	}

	@Test
	public void testJtaApi(SessionFactoryScope factoryScope) throws Exception {
		final TransactionInterceptor interceptor = new TransactionInterceptor();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		try (Session session = sessionFactory.withOptions().interceptor( interceptor ).openSession()) {

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

			// Interceptor#afterTransactionBegin is never called when using JTA
			assertFalse( interceptor.afterTransactionBeginMethodCalled );
			assertNull( interceptor.afterTransactionBeginAssertionPassed );
			assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
			assertNull( interceptor.afterTransactionCompletionAssertionPassed );

			SimpleEntity entity = new SimpleEntity( "Hello World" );
			session.persist( entity );

			interceptor.reset();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
			assertTrue( interceptor.afterTransactionCompletionMethodCalled );
			if ( options.allowJtaTransactionAccess.allowTransactionAccess(
					sessionFactory,
					options.jpaTransactionCompliance
			) ) {
				assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
				assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
			}
			else {
				assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
				assertNull( interceptor.afterTransactionCompletionAssertionPassed );
			}

			assertNull( interceptor.afterTransactionBeginAssertionPassed );
		}
	}

	@Test
	public void testJtaApiWithSharedTransactionCoordinator(SessionFactoryScope factoryScope) throws Exception {
		final TransactionInterceptor interceptor = new TransactionInterceptor();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		try (Session originalSession = sessionFactory.openSession()) {
			try (Session session =  originalSession.sessionWithOptions().connection().interceptor( interceptor ).openSession()) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

				// Interceptor#afterTransactionBegin is never called when using JTA
				assertFalse( interceptor.afterTransactionBeginMethodCalled );
				assertNull( interceptor.afterTransactionBeginAssertionPassed );
				assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
				assertNull( interceptor.afterTransactionCompletionAssertionPassed );

				SimpleEntity entity = new SimpleEntity( "Hello World" );
				session.persist( entity );

				interceptor.reset();

				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

				assertTrue( interceptor.beforeTransactionCompletionMethodCalled );
				assertTrue( interceptor.afterTransactionCompletionMethodCalled );
				if ( options.allowJtaTransactionAccess.allowTransactionAccess(
						sessionFactory,
						options.jpaTransactionCompliance
				) ) {
					assertEquals( true, interceptor.beforeTransactionCompletionAssertionPassed );
					assertEquals( true, interceptor.afterTransactionCompletionAssertionPassed );
				}
				else {
					assertNull( interceptor.beforeTransactionCompletionAssertionPassed );
					assertNull( interceptor.afterTransactionCompletionAssertionPassed );
				}

				assertNull( interceptor.afterTransactionBeginAssertionPassed );
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
