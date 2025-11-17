/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Jpa(
		annotatedClasses = { JtaIllegalArgumentExceptionTest.Person.class },
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
@JiraKey( value = "HHH-15225")
public class JtaIllegalArgumentExceptionTest {

	@Test
	public void testNonExistingNativeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() -> {
									entityManager.createNamedQuery( "NonExisting_NativeQuery" );
								}
						)
		);
	}

	@Test
	public void testNonExistingNativeQuery2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() -> {
									entityManager.createNamedQuery( "NonExisting_NativeQuery", Person.class );
								}
						)
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;
	}
}
