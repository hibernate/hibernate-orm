/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

@DomainModel(
		annotatedClasses = {
				MergeCollectionWithRemovedElementTest.Parent.class,
				MergeCollectionWithRemovedElementTest.Child.class,
				MergeCollectionWithRemovedElementTest.GrandChild.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-8114")
public class MergeCollectionWithRemovedElementTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p1 = new Parent( 1L, "p1" );
					Child c1 = new Child( 2L );
					Child c2 = new Child( 3L );
					GrandChild gc1 = new GrandChild( 4L );
					GrandChild gc2 = new GrandChild( 5L );
					GrandChild gc3 = new GrandChild( 6L );
					GrandChild gc4 = new GrandChild( 7L );
					p1.addChild( c1 );
					p1.addChild( c2 );
					c1.addGrandChild( gc1 );
					c1.addGrandChild( gc2 );
					c2.addGrandChild( gc3 );
					c2.addGrandChild( gc4 );
					session.persist( p1 );
					session.persist( gc1 );
					session.persist( gc2 );
					session.persist( gc3 );
					session.persist( gc4 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from GrandChild" ).executeUpdate();
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMergeDetachedAfterFlush(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Load all data
					Parent parent = session.find( Parent.class, 1L);
					for ( Child u : parent.children ) {
						Hibernate.initialize( u.grandChildren );
					}
					// Detach all data
					session.clear();
					// Load the parent into the PC
					session.find( Parent.class, 1L);
					// Remove a grand child and also remove it from the detached object
					Child u = parent.children.iterator().next();
					GrandChild grandChild = u.grandChildren.iterator().next();
					session.remove( grandChild );
					u.grandChildren.remove( grandChild );
					// Flush the remove of the grand child
					session.flush();
					// Clear the PC
					session.clear();
					// Merge the detached object
					session.merge( parent );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Long id;
		String name;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		Set<Child> children = new HashSet<>(0);

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addChild(Child child) {
			children.add(child);
			child.parent = this;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Parent parent;
		@OneToMany(mappedBy = "child")
		Set<GrandChild> grandChildren = new HashSet<>(0);

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}

		public void addGrandChild(GrandChild grandChild) {
			grandChildren.add(grandChild);
			grandChild.child = this;
		}
	}

	@Entity(name = "GrandChild")
	public static class GrandChild {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Child child;

		public GrandChild() {
		}

		public GrandChild(Long id) {
			this.id = id;
		}
	}
}
