/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cache;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = CacheTest.Person.class,
		sharedCacheMode = SharedCacheMode.ALL
)
public class CacheTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();;
	}

	@Test
	public void testSettingCacheRetrieveModeToBypass(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Person p = new Person( 1, "Andrea" );
					entityManager.persist( p );

				}
		);

		scope.getEntityManagerFactory().getCache().evictAll();

		scope.inEntityManager(
				entityManager -> {
					Cache cache = entityManager.getEntityManagerFactory().getCache();
					assertNotNull( cache );
					assertFalse( cache.contains( Person.class, 1 ) );

					entityManager.setProperty(
							"jakarta.persistence.cache.retrieveMode",
							CacheRetrieveMode.BYPASS
					);
					entityManager.setProperty(
							"jakarta.persistence.cache.storeMode",
							CacheStoreMode.BYPASS
					);

					entityManager.find( Person.class, 1 );

					cache = entityManager.getEntityManagerFactory().getCache();
					assertNotNull( cache );
					assertFalse( cache.contains( Person.class, 1 ) );
				}
		);
	}

	@Test
	public void testSettingCacheStoreModeToBypass(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						entityManager.setProperty(
								"jakarta.persistence.cache.storeMode",
								CacheStoreMode.BYPASS
						);
						Person p = new Person( 1, "Andrea" );
						entityManager.persist( p );
						entityManager.flush();
						entityManager.getTransaction().commit();
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
					Cache cache = entityManager.getEntityManagerFactory().getCache();
					assertNotNull( cache );
					assertFalse( cache.contains( Person.class, 1 ) );

					final Person person = entityManager.find( Person.class, 1 );
					assertNotNull( person );
					cache = entityManager.getEntityManagerFactory().getCache();
					assertNotNull( cache );
					assertFalse( cache.contains( Person.class, 1 ) );
				}
		);

	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	@Cacheable
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}


}
