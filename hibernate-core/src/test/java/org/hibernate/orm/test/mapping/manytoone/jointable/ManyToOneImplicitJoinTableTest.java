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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(annotatedClasses =
		{ManyToOneImplicitJoinTableTest.X.class,
		ManyToOneImplicitJoinTableTest.Y.class})
class ManyToOneImplicitJoinTableTest {
	@JiraKey("HHH-19564") @Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			X x = new X();
			x.id = 1;
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
			assertEquals( 1L, y.x.id );
		} );
		scope.inTransaction( s -> {
			Y y =
					s.createQuery( "from Y where id = ?1", Y.class )
							.setParameter( 1, 0L )
							.getSingleResult();
			assertEquals("Gavin", y.name);
			assertNotNull(y.x);
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResult();
			assertEquals( 1L, id );
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			X x = new X();
			x.id = -1;
			s.persist( x );
			y.x = x;
			// uses a SQL merge to update the join table
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNotNull(y.x);
			assertEquals( -1L, y.x.id );
		} );
		scope.inTransaction( s -> {
			Y y =
					s.createQuery( "from Y where id = ?1", Y.class )
							.setParameter( 1, 0L )
							.getSingleResult();
			assertEquals("Gavin", y.name);
			assertNotNull(y.x);
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResult();
			assertEquals( -1L, id );
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			y.x = null;
			// uses a SQL merge to update the join table
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNull(y.x);
		} );
		scope.inTransaction( s -> {
			Y y =
					s.createQuery( "from Y where id = ?1", Y.class )
							.setParameter( 1, 0L )
							.getSingleResult();
			assertEquals("Gavin", y.name);
			assertNull(y.x);
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
