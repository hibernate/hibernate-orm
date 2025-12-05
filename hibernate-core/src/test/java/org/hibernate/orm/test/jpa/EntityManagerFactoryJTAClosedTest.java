/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityManagerFactoryJTAClosedTest
 *
 * @author Scott Marlow
 */
@Jpa(
		integrationSettings = {
				@Setting(name = AvailableSettings.JAKARTA_TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
		},
		settingProviders = {@SettingProvider( settingName = AvailableSettings.JTA_PLATFORM, provider = JtaPlatformSettingProvider.class)}
)
public class EntityManagerFactoryJTAClosedTest {

	/**
	 * Test that using a closed EntityManagerFactory throws an IllegalStateException
	 * Also ensure that HHH-8586 doesn't regress.
	 * @throws Exception
	 */
	@Test
	public void testWithTransactionalEnvironment(EntityManagerFactoryScope scope) throws Exception {

		assertFalse( JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()) );
		EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();

		entityManagerFactory.close();	// close the underlying entity manager factory

		assertThrows(
				IllegalStateException.class,
				entityManagerFactory::createEntityManager,
				"expected IllegalStateException when calling emf.createEntityManager with closed emf"
		);

		assertThrows(
				IllegalStateException.class,
				entityManagerFactory::getCriteriaBuilder,
				"expected IllegalStateException when calling emf.getCriteriaBuilder with closed emf"
				);

		assertThrows(
				IllegalStateException.class,
				entityManagerFactory::getCache,
				"expected IllegalStateException when calling emf.getCache with closed emf"
				);

		assertFalse( entityManagerFactory.isOpen() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

}
