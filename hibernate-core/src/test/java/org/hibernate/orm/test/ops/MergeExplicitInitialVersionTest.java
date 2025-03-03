/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {MergeExplicitInitialVersionTest.E.class,
		MergeExplicitInitialVersionTest.F.class,
		MergeExplicitInitialVersionTest.G.class})
public class MergeExplicitInitialVersionTest {
	@Test public void testGeneratedId(EntityManagerFactoryScope scope) {
		E e = new E();
		scope.inTransaction(s->s.persist(e));
		assertEquals(e.version, 1);
		e.text = "hello";
		E e2 = scope.fromTransaction(s->s.merge(e));
		assertEquals(e2.version, 2);
	}
	@Test public void testAssignedId(EntityManagerFactoryScope scope) {
		F f = new F();
		scope.inTransaction(s->s.persist(f));
		assertEquals(f.version, 1);
		f.text = "hello";
		F f2 = scope.fromTransaction(s->s.merge(f));
		assertEquals(f2.version, 2);
	}
	@Test public void testNegativeVersion(EntityManagerFactoryScope scope) {
		G g = new G();
		scope.inTransaction(s->s.persist(g));
		assertEquals(g.version, 0);
		g.text = "hello";
		G g2 = scope.fromTransaction(s->s.merge(g));
		assertEquals(g2.version, 1);
	}

	@Entity
	static class E {
		@Id
		@GeneratedValue
		long id;
		@Version
		int version = 1;
		String text;
	}

	@Entity
	static class F {
		@Id
		long id = 5;
		@Version
		int version = 1;
		String text;
	}

	@Entity
	static class G {
		@Id
		@GeneratedValue
		long id;
		@Version
		int version = -1;
		String text;
	}

}
