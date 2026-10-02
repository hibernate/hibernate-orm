/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
	annotatedClasses = {
		MappedSuperclassTest.GrandPappy.class,
		MappedSuperclassTest.Child.class,
		MappedSuperclassTest.SubClass.class
	}
)
public class MappedSuperclassTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFindParent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child1 = new SubClass( 1L );
					child1.state = 1;
					entityManager.persist( child1 );

					Child child2 = new Child( 2L );
					child2.state = 0;
					entityManager.persist( child2 );
				}
		);
		scope.inTransaction(
				entityManager -> {
					List<Child> children = entityManager.createQuery( "select c from Child c", Child.class )
							.getResultList();
					assertThat( children.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testFindGrandChild(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Child child1 = new SubClass( 1L );
			child1.state = 1;
			entityManager.persist( child1 );

			Child child2 = new Child( 2L );
			child2.state = 0;
			entityManager.persist( child2 );

			GrandPappy grandPappy1 = new GrandPappy( 1L );
			grandPappy1.grandChildren.add( child1 );
			grandPappy1.grandChildren.add( child2 );
			entityManager.persist( grandPappy1 );
		} );
		scope.inTransaction( entityManager -> {
			GrandPappy grandPappy = entityManager.find( GrandPappy.class, 1L );
			assertThat( grandPappy.grandChildren.size() ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "GrandPappy")
	public static class GrandPappy {
		@Id
		private Long id;

		@OneToMany
		Set<Child> grandChildren = new HashSet<>();

		public GrandPappy() {
		}

		public GrandPappy(long id) {
			this.id = id;
		}
	}

	@Entity(name = "Child")
	public static class Child extends Intermediate {
		@Id
		private Long id;

		public Child() {
		}

		public Child(long id) {
			this.id = id;
		}
	}

	@Entity(name = "SubClass")
	public static class SubClass extends Child {
		public SubClass() {
		}

		public SubClass(long id) {
			super( id );
		}
	}

	@MappedSuperclass
	public static class Intermediate extends Parent {
	}

	@MappedSuperclass
	@SQLRestriction("state = 0")
	public static class Parent {
		public Parent() {
		}

		int state;
	}
}
