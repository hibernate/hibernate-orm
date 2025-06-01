/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses =
		{OnDeleteJoinedInheritanceTest.A.class,
		OnDeleteJoinedInheritanceTest.B.class,
		OnDeleteJoinedInheritanceTest.C.class},
		useCollectingStatementInspector = true)
//@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
class OnDeleteJoinedInheritanceTest {
	@Test void test(EntityManagerFactoryScope scope) {
		var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( em -> {
			B b = new B();
			b.id = 1;
			em.persist( b );
			C c = new C();
			c.id = 2;
			em.persist( c );
		} );
		inspector.assertExecutedCount( 4 );
		inspector.clear();

		scope.inTransaction( em -> {
			A b = em.find( A.class, 1L );
			A c = em.getReference( A.class, 2L );
			inspector.assertExecutedCount( 1 );
			em.remove( b );
			em.remove( c );
		} );
		inspector.assertExecutedCount( scope.getDialect().supportsCascadeDelete() ? 4 : 6 );

		scope.inTransaction( em -> {
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from B", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from C", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A", Integer.class )
							.getSingleResultOrNull() );
		});
	}

	@Entity(name = "A")
	@Inheritance(strategy = InheritanceType.JOINED)
	static class A {
		@Id
		long id;
		boolean a;
	}

	@Entity(name = "B")
	@OnDelete(action = OnDeleteAction.CASCADE)
	static class B extends A {
		long b;
	}

	@Entity(name = "C")
	@OnDelete(action = OnDeleteAction.CASCADE)
	static class C extends A {
		int c;
	}
}
