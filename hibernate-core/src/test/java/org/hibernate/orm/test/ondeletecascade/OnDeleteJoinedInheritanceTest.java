/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ondeletecascade;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.Assert.assertEquals;

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
							.getSingleResult() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from C", Integer.class )
							.getSingleResult() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A", Integer.class )
							.getSingleResult() );
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
