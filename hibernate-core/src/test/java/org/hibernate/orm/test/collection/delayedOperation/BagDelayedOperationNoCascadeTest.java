/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delayedOperation;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.AbstractPersistentCollection;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.collection.delayedOperation.BagDelayedOperationNoCascadeTest.Child;
import static org.hibernate.orm.test.collection.delayedOperation.BagDelayedOperationNoCascadeTest.Parent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests delayed operations that are queued for a PersistentBag. The Bag does not have
 * to be extra-lazy to queue the operations.
 *
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class
		}
)
@SessionFactory
public class BagDelayedOperationNoCascadeTest {
	private Long parentId;

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
				session -> {
					session.persist( child1 );
					session.persist( child2 );
					session.persist( parent );
				}
		);

		parentId = parent.getId();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();

		parentId = null;
	}

	@Test
	@JiraKey(value = "HHH-5855")
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
					// collection should still be uninitialized
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
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
					// collection should still be uninitialized
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
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
	@JiraKey(value = "HHH-11209")
	public void testMergeInitializedBagAndRemerge(SessionFactoryScope scope) {
		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.get( Parent.class, parentId );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					// initialize
					Hibernate.initialize( p.getChildren() );
					assertTrue( Hibernate.isInitialized( p.getChildren() ) );
					return p;
				}
		);

		Parent modifiedParent = scope.fromTransaction(
				session -> {
					Parent p = (Parent) session.merge( parent );
					Child c = new Child( "Zeke" );
					c.setParent( p );
					session.persist( c );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );
					p.getChildren().size();
					p.getChildren().add( c );
					return p;
				}
		);


		// Merge detached Parent with initialized children
		Parent p = scope.fromTransaction(
				session -> {
					Parent mergedParent = (Parent) session.merge( modifiedParent );
					// after merging, p#children will be uninitialized
					assertFalse( Hibernate.isInitialized( mergedParent.getChildren() ) );
					assertTrue( ( (AbstractPersistentCollection) mergedParent.getChildren() ).hasQueuedOperations() );
					return mergedParent;
				}
		);

		assertFalse( ( (AbstractPersistentCollection) p.getChildren() ).hasQueuedOperations() );

		// Merge detached Parent, now with uninitialized children no queued operations
		scope.inTransaction(
				session -> {
					Parent mergedParent = (Parent) session.merge( p );
					assertFalse( Hibernate.isInitialized( mergedParent.getChildren() ) );
					assertFalse( ( (AbstractPersistentCollection) mergedParent.getChildren() ).hasQueuedOperations() );

				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		// Don't need extra-lazy to delay add operations to a bag.
		@OneToMany(mappedBy = "parent")
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
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
