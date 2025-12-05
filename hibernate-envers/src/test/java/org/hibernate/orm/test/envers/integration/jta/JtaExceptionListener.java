/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.integration.reventity.ExceptionListenerRevEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import jakarta.transaction.RollbackException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Same as {@link org.hibernate.orm.test.envers.integration.reventity.ExceptionListener}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = { StrTestEntity.class, ExceptionListenerRevEntity.class },
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class))
public class JtaExceptionListener {
	@Test
	public void testTransactionRollback(EntityManagerFactoryScope scope) {
		final var emf = scope.getEntityManagerFactory();
		assertThrows(
				RollbackException.class, () -> {
					TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

					var entityManager = emf.createEntityManager();
					try {
						// Trying to persist an entity - however the listener should throw an exception, so the entity
						// shouldn't be persisted
						StrTestEntity te = new StrTestEntity( "x" );
						entityManager.persist( te );
					}
					finally {
						entityManager.close();
						TestingJtaPlatformImpl.tryCommit();
					}
				}
		);
	}

	@Test
	public void testDataNotPersisted(EntityManagerFactoryScope scope) throws Exception {
		final var emf = scope.getEntityManagerFactory();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		// Checking if the entity became persisted
		var entityManager = emf.createEntityManager();
		try {
			long count = entityManager.createQuery( "from StrTestEntity s where s.str = 'x'", StrTestEntity.class )
					.getResultList().size();
			assertEquals( 0, count );
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}
}
