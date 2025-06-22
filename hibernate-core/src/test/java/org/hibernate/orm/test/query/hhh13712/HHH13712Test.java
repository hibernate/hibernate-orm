/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh13712;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;


@JiraKey(value = "HHH-13712")
@Jpa(
		annotatedClasses = { HHH13712Test.Super.class, HHH13712Test.SubObject.class, HHH13712Test.SomeOther.class }
)
public class HHH13712Test {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			SomeOther a_1 = new SomeOther( 1L );
			SomeOther a_2 = new SomeOther( 2L );
			SomeOther a_3 = new SomeOther( 3L );
			SubObject b_5 = new SubObject( 5L, a_1 );
			SubObject b_6 = new SubObject( 6L, a_2 );
			SubObject b_7 = new SubObject( 7L, a_3 );

			em.merge( a_1 );
			em.merge( a_2 );
			em.merge( a_3 );
			em.merge( b_5 );
			em.merge( b_6 );
			em.merge( b_7 );
		} );
	}

	@Test
	public void testJoinSuperclassAssociationOnly(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			List<Integer> actual = em.createQuery( "SELECT 1 FROM SubObject sub LEFT JOIN sub.parent p", Integer.class )
					.getResultList();
			assertEquals( 3, actual.size() );
		} );
	}

	@Test
	public void testJoinSuperclassAssociation(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			long actual = em.createQuery(
					"SELECT COUNT(sub) FROM SubObject sub LEFT JOIN sub.parent p WHERE p.id = 1",
					Long.class
			).getSingleResult();
			assertEquals( 1L, actual );
		} );
	}

	@Test
	public void testCountParentIds(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			long actual = em.createQuery( "SELECT COUNT(distinct sub.parent.id) FROM SubObject sub", Long.class )
					.getSingleResult();
			assertEquals( 3L, actual );
		} );
	}

	@Entity(name = "Super")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Super {

		@Id
		@Column
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(nullable = false)
		SomeOther parent;

	}

	@Entity(name = "SubObject")
	public static class SubObject extends Super {

		SubObject() {
		}

		SubObject(Long id, SomeOther parent) {
			this.id = id;
			this.parent = parent;
		}

	}

	@Entity(name = "SomeOther")
	public static class SomeOther {

		@Id
		@Column
		Long id;

		SomeOther() {
		}

		SomeOther(Long id) {
			this.id = id;
		}
	}

}
