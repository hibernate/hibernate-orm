/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cacheable.disableselective;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cache;
import jakarta.persistence.SharedCacheMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		xmlMappings = {
				"org/hibernate/orm/test/jpa/cacheable/disableselective/orm.xml",
		},
		sharedCacheMode = SharedCacheMode.DISABLE_SELECTIVE
)
@JiraKey( "HHH-18041" )
public class DisableCacheTest {

	@Test
	public void testDisableCache(EntityManagerFactoryScope scope) {
		final Long id = 1l;
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						City city = new City( id, "Gradoli" );
						entityManager.persist( city );

						Person person = new Person( "1", "And", "Bor" );
						entityManager.persist( person );
						entityManager.flush();

						entityManager.getTransaction().commit();
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}

					Cache cache = entityManager.getEntityManagerFactory().getCache();
					assertTrue( cache.contains( City.class, id ) );
					assertFalse( cache.contains( Person.class, "1" ) );
				}
		);
	}
}
