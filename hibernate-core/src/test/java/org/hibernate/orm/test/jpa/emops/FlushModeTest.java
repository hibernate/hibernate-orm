/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {
				Dress.class
		},
		integrationSettings = { @Setting(name = HibernateHints.HINT_FLUSH_MODE, value = "manual") }
)
public class FlushModeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCreateEMFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Dress dress = new Dress();
						dress.name = "long dress";
						entityManager.persist( dress );
						entityManager.getTransaction().commit();

						entityManager.clear();

						Assertions.assertNull( entityManager.find( Dress.class, dress.name ) );
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
