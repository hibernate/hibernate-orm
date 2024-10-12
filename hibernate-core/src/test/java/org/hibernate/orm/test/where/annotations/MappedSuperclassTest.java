/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				MappedSuperclassTest.Child.class,
				MappedSuperclassTest.SubClass.class
		}
)
public class MappedSuperclassTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from SubClass" ).executeUpdate();
					entityManager.createQuery( "delete from Child" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFindParent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child1 = new SubClass( 1L );
					child1.flag = true;
					entityManager.persist( child1 );

					Child child2 = new Child( 2L );
					child2.flag = false;
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

	public static class Intermediate extends Parent {
	}

	@MappedSuperclass
	@SQLRestriction("flag = false")
	public static class Parent {
		public Parent() {
		}

		boolean flag;
	}
}
