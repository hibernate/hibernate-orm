/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone.jointable;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@Jpa(annotatedClasses =
		{ManyToOneImplicitJoinTableTest.X.class,
		ManyToOneImplicitJoinTableTest.Y.class})
class ManyToOneImplicitJoinTableTest {
	@JiraKey("HHH-19564") @Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			X x = new X();
			Y y = new Y();
			y.x = x;
			s.persist( x );
			s.persist( y );
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			y.name = "Gavin";
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNotNull(y.x);
		} );
	}
	@Entity(name="Y")
	static class Y {
		@Id
		long id;
		String name;
		@JoinTable
		@ManyToOne X x;
	}
	@Entity(name="X")
	static class X {
		@Id
		long id;
	}
}
