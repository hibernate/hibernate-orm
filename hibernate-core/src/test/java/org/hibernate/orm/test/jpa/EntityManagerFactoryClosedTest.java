/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 * This test was separated from the EntityManagerTest, which now extends the {@link EntityManagerFactoryBasedFunctionalTest}
 * That only creates the {@link jakarta.persistence.EntityManagerFactory} once per class, so this test caused problems
 * for the other tests
 */
@Jpa(
		integrationSettings = {@Setting(name = JpaComplianceSettings.JPA_CLOSED_COMPLIANCE, value = "true")}
)
public class EntityManagerFactoryClosedTest {

	@Test
	public void testFactoryClosed(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		assertTrue( entityManager.isOpen() );
		assertTrue( entityManager.getEntityManagerFactory().isOpen() );

		// closing the entity manager factory should close the EntityManager
		entityManager.getEntityManagerFactory().close();
		assertFalse( entityManager.isOpen() );

		assertThrows(
				IllegalStateException.class,
				entityManager::close,
				"closing entity manager that uses a closed session factory, must throw IllegalStateException"
		);
	}

}
