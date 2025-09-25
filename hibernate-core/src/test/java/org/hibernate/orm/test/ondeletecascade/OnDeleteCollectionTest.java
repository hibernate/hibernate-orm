/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ondeletecascade;

import org.hibernate.Hibernate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses =
		{OnDeleteCollectionTest.A.class},
		useCollectingStatementInspector = true)
class OnDeleteCollectionTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( em -> {
			A a = new A();
			a.id = 2;
			a.bs.add( "b" );
			em.persist( a );
		} );
		inspector.assertExecutedCount( 2 );
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
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A_bs", Integer.class )
							.getSingleResult() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A", Integer.class )
							.getSingleResult() );
		});
	}

	@Entity(name = "A")
	static class A {
		@Id
		long id;
		boolean a;
		@ElementCollection
		@OnDelete(action = OnDeleteAction.CASCADE)
		Set<String> bs = new HashSet<>();
	}
}
