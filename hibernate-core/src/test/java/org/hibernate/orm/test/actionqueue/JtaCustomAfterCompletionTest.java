/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = JtaCustomAfterCompletionTest.SimpleEntity.class,
		settingProviders = @SettingProvider(settingName = AvailableSettings.JTA_PLATFORM,
				provider = JtaPlatformSettingProvider.class),
		integrationSettings = {
				@Setting(name = AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, value = "jta"),
				@Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true"),
		}
)
@SessionFactory
public class JtaCustomAfterCompletionTest {

	@AfterEach
	public void afterEach(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getSchemaManager()
				.truncateMappedObjects();
	}

	@Test
	public void success(EntityManagerFactoryScope scope) {
		AtomicBoolean called = new AtomicBoolean( false );
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			scope.inEntityManager( session -> {
				session.unwrap( SessionImplementor.class ).getActionQueue()
						.registerCallback( new AfterTransactionCompletionProcess() {
							@Override
							public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
								called.set( true );
							}
						} );
				assertFalse( called.get() );

				session.persist( new SimpleEntity( "jack" ) );

			} );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			assertTrue( called.get() );

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			// Check that the transaction was committed
			scope.inEntityManager( session -> {
				long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
						.getSingleResult();
				assertEquals( 1L, count );
			} );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			// TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getTransaction().rollback();
			fail( "Should not have thrown an exception" );
		}
	}

	@Test
	public void rollback(EntityManagerFactoryScope scope) {
		try {
			AtomicBoolean called = new AtomicBoolean( false );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			scope.inEntityManager( session -> {
				session.unwrap( SessionImplementor.class ).getActionQueue()
						.registerCallback( new AfterTransactionCompletionProcess() {
							@Override
							public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
								called.set( true );
							}
						} );
				assertFalse( called.get() );
				scope.inEntityManager( theSession -> {
					theSession.persist( new SimpleEntity( "jack" ) );
					theSession.getTransaction().setRollbackOnly();
				} );
			} );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			assertTrue( called.get() );

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			// Check that the transaction was not committed
			scope.inEntityManager( session -> {
				long count = session.createQuery( "select count(*) from SimpleEntity", Long.class )
						.getSingleResult();
				assertEquals( 0L, count );
			} );
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			// TestingJtaPlatformImpl.INSTANCE.getTransactionManager().getTransaction().rollback();
			fail( "Should not have thrown an exception", e );
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
