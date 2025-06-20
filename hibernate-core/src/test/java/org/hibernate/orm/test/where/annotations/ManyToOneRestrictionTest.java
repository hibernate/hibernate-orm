/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

@Jpa(annotatedClasses = {ManyToOneRestrictionTest.X.class, ManyToOneRestrictionTest.Y.class})
class ManyToOneRestrictionTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(em -> {
			Y y = new Y();
			X x = new X();
			x.id = -1;
			y.x = x;
			em.persist(x);
			em.persist(y);
		});
		scope.inTransaction(em -> {
			Y y = em.find(Y.class, 0L);
			assertNull(y.x);
			var fk =
					em.createNativeQuery( "select xx from YY", long.class )
							.getSingleResultOrNull();
			assertNotNull(fk);
			y.name = "hello";
		});
		scope.inTransaction(em -> {
			Y y = em.find(Y.class, 0L);
			assertNull(y.x);
			var fk =
					em.createNativeQuery( "select xx from YY", long.class )
							.getSingleResultOrNull();
			assertNotNull(fk);
		});

	}

	@Entity
	@Table(name = "XX")
	@SQLRestriction("id>0")
	static class X {
		@Id
		long id;
	}
	@Entity
	@Table(name = "YY")
	static class Y {
		@Id
		long id;
		String name;
		@ManyToOne
		@JoinColumn(name = "xx")
		X x;
	}
}
