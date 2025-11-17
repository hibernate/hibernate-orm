/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = { JtaReusingEntityTransactionTest.TestEntity.class },
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
public class JtaReusingEntityTransactionTest {

	@Test
	public void entityTransactionShouldBeReusableTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					EntityTransaction transaction = null;
					try {
						transaction = entityManager.getTransaction();
						entityManager.persist( new TestEntity() );
						transaction.begin();
						transaction.commit();
						transaction.begin();
						entityManager.persist( new TestEntity() );
						transaction.commit();
					}
					finally {
						if ( transaction != null && transaction.isActive() ) {
							transaction.rollback();
						}
					}
				}
		);

		scope.inTransaction(
				entityManager -> {
					List<TestEntity> results = entityManager.createQuery( "from TestEntity", TestEntity.class ).getResultList();
					assertThat( results.size(), is( 2 ) );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
