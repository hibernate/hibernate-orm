/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				LoadGraphMergeTest.Parent.class,
				LoadGraphMergeTest.Child.class,
				LoadGraphMergeTest.GrandChild.class,
		}
)
@JiraKey( "HHH-15271" )
public class LoadGraphMergeTest {

	private static final Long PARENT_ID_1 = 1L;
	private static final Long PARENT_ID_2 = 2L;

	@BeforeAll
	public static void init(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					GrandChild grandChild = new GrandChild( 1L, "grand child 1" );
					Child child = new Child( 1L, grandChild );
					Parent parent = new Parent( PARENT_ID_1, child );
					entityManager.persist( parent );

					GrandChild grandChild2 = new GrandChild( 2L, "grand child 2" );
					Child child2 = new Child( 2L, grandChild2 );
					Parent parent2 = new Parent( PARENT_ID_2, child2 );
					entityManager.persist( parent2 );
				}
		);
	}

	@Test
	public void testGrandChildHasNotBeenInitializedByMerge(EntityManagerFactoryScope scope) {
		Parent parent = scope.fromTransaction( entityManager ->
				entityManager.find( LoadGraphMergeTest_.Parent_._parent_child, PARENT_ID_1 )
		);

		Parent parent2 = scope.fromTransaction( entityManager ->
				entityManager.find( LoadGraphMergeTest_.Parent_._parent_child, PARENT_ID_2)
		);

		scope.inTransaction( entityManager -> {
			assertTrue( Hibernate.isInitialized( parent.getChild() ) );
			assertFalse( Hibernate.isInitialized( parent.getChild().getGrandChild() ) );

			Session session = entityManager.unwrap( Session.class );

			Parent mergedParent = session.merge( parent, LoadGraphMergeTest_.Parent_._parent_child );

			Child child = mergedParent.getChild();
			assertTrue( Hibernate.isInitialized( child ) );
			assertFalse( Hibernate.isInitialized( child.getGrandChild() ),
					"Merge has initialized `parent.child` lazy association" );

			assertTrue( Hibernate.isInitialized( parent2.getChild() ) );
			assertFalse( Hibernate.isInitialized( parent2.getChild().getGrandChild() ) );

			Parent mergedParent2 = session.merge( parent2 );

			Child child2 = mergedParent2.getChild();
			assertTrue( Hibernate.isInitialized( child2 ) );
			assertTrue( Hibernate.isInitialized( child2.getGrandChild() ) );
		} );
	}

	@Test
	public void testChildHasNotBeenInitializedByMerge(EntityManagerFactoryScope scope) {
		Parent parent = scope.fromTransaction( entityManager ->
				entityManager.find(
						Parent.class,
						PARENT_ID_1 )
		);

		scope.inTransaction( entityManager -> {
			Child child1 = parent.getChild();
			assertFalse( Hibernate.isInitialized( child1 ) );

			Session session = entityManager.unwrap( Session.class );
			Parent mergedParent = session.merge( parent, LoadGraphMergeTest_.Parent_._parent );

			Child child = mergedParent.getChild();
			assertFalse( Hibernate.isInitialized( child ),
					"Merge has initialized `parent.child` lazy association" );
		} );
	}

	@Entity(name = "Parent")
	@NamedEntityGraph(
			name = "parent.child",
			attributeNodes = @NamedAttributeNode("child")
	)
	@NamedEntityGraph(
			name = "parent"
	)
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Child child;

		public Parent() {
		}

		public Parent(Long id, Child child) {
			this.id = id;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private GrandChild grandChild;

		public Child() {
		}

		public Child(Long id, GrandChild grandChild) {
			this.id = id;
			this.grandChild = grandChild;
		}


		public Long getId() {
			return id;
		}

		public GrandChild getGrandChild() {
			return grandChild;
		}
	}

	@Entity(name = "GrandChild")
	public static class GrandChild {

		@Id
		private Long id;

		private String name;

		public GrandChild() {
		}

		public GrandChild(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

	}

}
