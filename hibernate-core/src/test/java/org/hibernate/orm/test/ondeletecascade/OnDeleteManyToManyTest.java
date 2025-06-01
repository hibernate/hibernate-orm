/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import org.hibernate.Hibernate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses =
		{OnDeleteManyToManyTest.A.class,
		OnDeleteManyToManyTest.B.class},
		useCollectingStatementInspector = true)
//@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
class OnDeleteManyToManyTest {
	@Test void test(EntityManagerFactoryScope scope) {
		var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( em -> {
			B b = new B();
			b.id = 1;
			em.persist( b );
			A a = new A();
			a.id = 2;
			a.bs.add( b );
			em.persist( a );
		} );
		inspector.assertExecutedCount( 3 );
		inspector.clear();

		scope.inTransaction( em -> {
			A a = em.find( A.class, 2L );
			inspector.assertExecutedCount( 1 );
			assertEquals( 1, a.bs.size() );
			inspector.assertExecutedCount( 2 );
			assertTrue( Hibernate.isInitialized( a.bs ) );
		} );
		inspector.clear();

		scope.inTransaction( em -> {
			A a = em.find( A.class, 2L );
			inspector.assertExecutedCount( 1 );
			em.remove( a );
			assertFalse( Hibernate.isInitialized( a.bs ) );
		} );
		inspector.assertExecutedCount( scope.getDialect().supportsCascadeDelete() ? 2 : 3 );

		scope.inTransaction( em -> {
			assertEquals( 1,
					em.createNativeQuery( "select count(*) from B", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A_B", Integer.class )
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
		@ManyToMany
		@OnDelete(action = OnDeleteAction.CASCADE)
		Set<B> bs = new HashSet<>();
	}

	@Entity(name = "B")
	static class B {
		@Id
		long id;
	}
}
