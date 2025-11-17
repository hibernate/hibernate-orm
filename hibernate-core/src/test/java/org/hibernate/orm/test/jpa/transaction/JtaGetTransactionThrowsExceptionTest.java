/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import jakarta.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andrea Boriero
 */
@Jpa(
		integrationSettings = {
				@Setting(name = AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting( name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				)
		}
)
public class JtaGetTransactionThrowsExceptionTest {

	@Test()
	@JiraKey(value = "HHH-12487")
	public void onCloseEntityManagerTest(EntityManagerFactoryScope scope) {
		EntityManager em = createEntityManager( scope );
		em.close();
		assertThrows(
				IllegalStateException.class,
				() -> em.getTransaction(),
				"Calling getTransaction on a JTA entity manager should throw an IllegalStateException"
		);
	}

	@Test()
	@JiraKey(value = "HHH-12487")
	public void onOpenEntityManagerTest(EntityManagerFactoryScope scope) {
		EntityManager em = createEntityManager( scope );
		try {
			assertThrows(
					IllegalStateException.class,
					() -> em.getTransaction(),
					"Calling getTransaction on a JTA entity manager should throw an IllegalStateException"
			);
		}
		finally {
			em.close();
		}
	}

	private EntityManager createEntityManager(EntityManagerFactoryScope scope) {
		return scope.getEntityManagerFactory().createEntityManager();
	}
}
