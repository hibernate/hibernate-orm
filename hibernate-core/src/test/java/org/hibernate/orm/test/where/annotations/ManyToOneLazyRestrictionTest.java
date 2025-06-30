/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(annotatedClasses = {ManyToOneLazyRestrictionTest.X.class, ManyToOneLazyRestrictionTest.Y.class})
class ManyToOneLazyRestrictionTest {
	@JiraKey("HHH-19565")
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(em -> {
			Y y = new Y();
			X x = new X();
			x.id = -1;
			y.x = x;
			em.persist(x);
			em.persist(y);
		});
		// @SQLRestrictions should not be applied to
		// foreign key associations, or the FK will
		// be set to null when the entity is updated,
		// leading to data loss
		scope.inTransaction(em -> {
			Y y = em.find(Y.class, 0L);
			assertNotNull(y.x);
			assertFalse(Hibernate.isInitialized(y.x));
			assertEquals(-1, y.x.getId());
			y.name = "hello";
		});
		scope.inTransaction(em -> {
			Y y = em.find(Y.class, 0L);
			assertNotNull(y.x);
			assertEquals(-1, y.x.getId());
			assertEquals("hello", y.name);
			assertFalse(Hibernate.isInitialized(y.x));
		});
	}

	@Entity
	@Table(name = "XX")
	@SQLRestriction("id>0")
	static class X {
		@Id
		long id;

		public long getId() {
			return id;
		}
	}
	@Entity
	@Table(name = "YY")
	static class Y {
		@Id
		long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "xx")
		X x;
	}
}
