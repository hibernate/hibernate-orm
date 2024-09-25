/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				CriteriaSelectOneToOneUnownedTest.Parent.class,
				CriteriaSelectOneToOneUnownedTest.Child.class,
		}
)
@JiraKey("HHH-18628")
public class CriteriaSelectOneToOneUnownedTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( 1l, "child" );
					Parent parent = new Parent( 1l, "parent", child );
					entityManager.persist( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCriteriaInnerJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					CriteriaQuery<Child> query = cb.createQuery( Child.class );
					Root<Parent> parent = query.from( Parent.class );
					Join<Parent, Child> child = parent.join( "child", JoinType.INNER );
					query.select( child );

					List<Child> children = entityManager.createQuery( query ).getResultList();
					assertThat( children ).isNotNull();
					assertThat( children.size() ).isEqualTo( 1 );
					Child c = children.get( 0 );
					assertThat( c ).isNotNull();
				} );
	}

	@Test
	public void testCriteriaLeftJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					CriteriaQuery<Child> query = cb.createQuery( Child.class );
					Root<Parent> parent = query.from( Parent.class );
					Join<Parent, Child> child = parent.join( "child", JoinType.LEFT );
					query.select( child );

					List<Child> children = entityManager.createQuery( query ).getResultList();
					assertThat( children ).isNotNull();
					assertThat( children.size() ).isEqualTo( 1 );
					Child c = children.get( 0 );
					assertThat( c ).isNotNull();
				} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToOne(mappedBy = "parent", cascade = CascadeType.PERSIST)
		private Child child;

		public Parent() {
		}

		public Parent(Long id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
			child.parent = this;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
