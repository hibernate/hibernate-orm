/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(annotatedClasses =
		{ManyToOneImplicitJoinTableRestrictionTest.X.class,
		ManyToOneImplicitJoinTableRestrictionTest.Y.class})
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Sybase doesn't have support for upserts")
class ManyToOneImplicitJoinTableRestrictionTest {
	@JiraKey("HHH-19555") @Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			X x = new X();
			Y y = new Y();
			x.id = -1;
			y.x = x;
			s.persist( x );
			s.persist( y );
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			y.name = "Gavin";
			assertNull(y.x);
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNull(y.x);
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResult();
			assertEquals( -1L, id );
		} );
		scope.inTransaction( s -> {
			Y y =
					s.createQuery( "from Y where id = ?1", Y.class )
							.setParameter( 1, 0L )
							.getSingleResult();
			assertEquals("Gavin", y.name);
			assertNull(y.x);
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResult();
			assertEquals( -1L, id );
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			X x = new X();
			x.id = 1;
			s.persist( x );
			y.x = x;
			// uses a SQL merge to update the join table
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNotNull(y.x);
			assertEquals( 1L, y.x.id );
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResult();
			assertEquals( 1L, id );
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
			y.x = null;
			// uses a SQL merge to update the join table
		} );
		scope.inTransaction( s -> {
			Y y = s.find( Y.class, 0L );
			assertEquals("Gavin", y.name);
			assertNull(y.x);
			var id = s.createNativeQuery( "select x_id from Y_X", long.class ).getSingleResultOrNull();
			assertNull( id );
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
	@SQLRestriction("id>0")
	static class X {
		@Id
		long id;
	}
}
