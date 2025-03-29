/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delayedOperation;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests delayed operations that are queued for a PersistentSet. The Set must be
 * extra lazy to queue the operations.
 *
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				SetDelayedOperationTest.Parent.class,
				SetDelayedOperationTest.Child.class
		}
)
@SessionFactory
public class SetDelayedOperationTest {
	private Long parentId;
	private Long childId1;
	private Long childId2;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		// start by cleaning up in case a test fails
		if ( parentId != null ) {
			cleanup( scope );
		}

		Parent parent = new Parent();
		Child child1 = new Child( "Sherman" );
		Child child2 = new Child( "Yogi" );
		parent.addChild( child1 );
		parent.addChild( child2 );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		parentId = parent.getId();
		childId1 = child1.getId();
		childId2 = child2.getId();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, parentId );
					parent.getChildren().clear();
					session.remove( parent );
				}
		);

		parentId = null;
	}

	@Test
	@JiraKey("HHH-5855")
	public void testSimpleAddDetached(SessionFactoryScope scope) {
		// Create 2 detached Child objects.
		Child c1 = new Child( "Darwin" );
		Child c2 = new Child( "Comet" );
		scope.inTransaction(
				session -> {
					session.persist( c1 );
					session.persist( c2 );
				}
		);

		// Now Child c is detached.

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// add detached Child c
					p.addChild( session.merge( c1 ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
				}
		);

		// Add a detached Child and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 3, p.getChildren().size() );
				}
		);

		// Add another detached Child, merge, and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					p.addChild( c2 );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					session.merge( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 4, p.getChildren().size() );
				}
		);
	}

	@Test
	@JiraKey("HHH-5855")
	public void testSimpleAddTransient(SessionFactoryScope scope) {
		// Add a transient Child and commit.
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// add transient Child
					p.addChild( new Child( "Darwin" ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 3, p.getChildren().size() );

				}
		);

		// Add another transient Child and commit again.
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// add transient Child
					p.addChild( new Child( "Comet" ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					session.merge( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 4, p.getChildren().size() );

				}
		);
	}

	@Test
	@JiraKey("HHH-5855")
	public void testSimpleAddManaged(SessionFactoryScope scope) {
		// Add 2 Child entities
		Child c1 = new Child( "Darwin" );
		Child c2 = new Child( "Comet" );
		scope.inTransaction(
				session -> {
					session.persist( c1 );
					session.persist( c2 );
				}
		);

		// Add a managed Child and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// get the first Child so it is managed; add to collection
					p.addChild( session.get( Child.class, c1.getId() ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
				}
		);
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 3, p.getChildren().size() );
				}
		);


		// Add the other managed Child, merge and commit.
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// get the second Child so it is managed; add to collection
					p.addChild( session.get( Child.class, c2.getId() ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					session.merge( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 4, p.getChildren().size() );
				}
		);
	}

	@Test
	@JiraKey("HHH-5855")
	public void testSimpleRemoveDetached(SessionFactoryScope scope) {
		// Get the 2 Child entities and detach.
		Child c1 = scope.fromTransaction(
				session -> session.get( Child.class, childId1 )
		);

		Child c2 = scope.fromTransaction(
				session -> session.get( Child.class, childId2 )
		);

		// Remove a detached entity element and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// remove a detached element and commit
					p.removeChild( c1 );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					session.merge( p );
				}
		);


		// Remove a detached entity element, merge, and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 1, p.getChildren().size() );
				}
		);

		// Remove a detached entity element, merge, and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// remove a detached element and commit
					p.removeChild( c2 );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					p = (Parent) session.merge( p );
					Hibernate.initialize( p );
				}
		);

		// Remove a detached entity element, merge, and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 0, p.getChildren().size() );
				}
		);
	}

	@Test
	@JiraKey("HHH-5855")
	public void testSimpleRemoveManaged(SessionFactoryScope scope) {
		// Remove a managed entity element and commit
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// get c1 so it is managed, then remove and commit
					p.removeChild( session.get( Child.class, childId1 ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 1, p.getChildren().size() );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// get c1 so it is managed, then remove, merge and commit
					p.removeChild( session.get( Child.class, childId2 ) );
					// collection should now be uninitialized
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					session.merge( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					assertEquals( 0, p.getChildren().size() );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
		private Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

		public void removeChild(Child child) {
			children.remove( child );
			child.setParent( null );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(nullable = false)
		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Child child = (Child) o;

			return name.equals( child.name );

		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

}
