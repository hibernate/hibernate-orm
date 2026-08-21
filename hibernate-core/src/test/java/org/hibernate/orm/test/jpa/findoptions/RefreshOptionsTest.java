/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.findoptions;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = RefreshOptionsTest.MyEntity.class)
public class RefreshOptionsTest {
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLockTimeouts.class)
	void test(EntityManagerFactoryScope scope) {
		MyEntity hello = new MyEntity("Hello");
		scope.getEntityManagerFactory()
				.runInTransaction(em -> em.persist(hello));
		scope.getEntityManagerFactory()
				.runInTransaction(em -> {
					MyEntity entity = em.find(MyEntity.class, hello.id);
					em.refresh(entity, LockModeType.PESSIMISTIC_WRITE, Timeout.seconds(10),
							CacheStoreMode.BYPASS);
					assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(entity));
					assertEquals(hello.name, entity.name);
					assertFalse(isInitialized(entity.list));
				});
		scope.getEntityManagerFactory()
				.runInTransaction(em -> {
					try {
						em.refresh(em.find(MyEntity.class, hello.id), LockMode.OPTIMISTIC);
					}
					catch (HibernateException he) {
						return;
					}
					fail();
				});
		scope.getEntityManagerFactory()
				.runInTransaction(em -> {
					try {
						em.refresh(em.find(MyEntity.class, hello.id), LockMode.OPTIMISTIC_FORCE_INCREMENT);
					}
					catch (HibernateException he) {
						return;
					}
					fail();
				});
		scope.getEntityManagerFactory()
				.runInTransaction(em -> {
					try {
						em.refresh(em.find(MyEntity.class, hello.id), LockMode.PESSIMISTIC_FORCE_INCREMENT);
					}
					catch (HibernateException he) {
						return;
					}
					fail();
				});
	}

	@Entity(name = "RefOptMyEntity")
	@FetchProfile(name = "hello world")
	public static class MyEntity {
		@Id @GeneratedValue long id;
		String name;

		@ElementCollection(fetch = FetchType.LAZY)
		@FetchProfileOverride(profile = "hello world",
				fetch = FetchType.EAGER)
		List<String> list;

		public MyEntity(String name) {
			this.name = name;
		}
		MyEntity() {
		}
	}
}
