/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.TransientPropertyValueException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Real ORM-event integration tests for the metadata cascade walker.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = {
		CascadeWalkerIntegrationTest.Parent.class,
		CascadeWalkerIntegrationTest.Child.class,
		CascadeWalkerIntegrationTest.CheckOwner.class,
		CascadeWalkerIntegrationTest.CycleA.class,
		CascadeWalkerIntegrationTest.CycleB.class
} )
@SessionFactory
class CascadeWalkerIntegrationTest {
	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void ordinaryLockEventsRemainFunctional(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Parent( 1 ) ) );

		scope.inTransaction( session -> {
			final var parent = session.find( Parent.class, 1 );
			session.lock( parent, LockMode.PESSIMISTIC_READ );

			assertThat( session.getCurrentLockMode( parent ) ).isEqualTo( LockMode.PESSIMISTIC_READ );
		} );
	}

	@Test
	void persistCascadesToTheCollection(SessionFactoryScope scope) {
		final var parent = parentWithChild( 1, 11, "initial" );

		scope.inTransaction( session -> session.persist( parent ) );

		assertCounts( scope, 1L, 1L );
	}

	@Test
	void persistOnFlushFindsAChildAddedToAManagedParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new Parent( 1 );
			session.persist( parent );
			session.flush();

			parent.addChild( new Child( 11, "added-before-flush" ) );
			session.flush();
		} );

		assertCounts( scope, 1L, 1L );
	}

	@Test
	void mergeCascadesDetachedCollectionChanges(SessionFactoryScope scope) {
		final var parent = parentWithChild( 1, 11, "initial" );
		scope.inTransaction( session -> session.persist( parent ) );
		parent.children.get( 0 ).name = "merged";
		parent.addChild( new Child( 12, "new-on-merge" ) );

		scope.inTransaction( session -> session.merge( parent ) );

		scope.inTransaction( session -> {
			final var merged = session.find( Parent.class, 1 );
			assertThat( merged.children ).extracting( child -> child.name )
					.containsExactlyInAnyOrder( "merged", "new-on-merge" );
		} );
	}

	@Test
	void removeCascadesToCollectionElements(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( parentWithChild( 1, 11, "initial" ) ) );

		scope.inTransaction( session -> {
			final var parent = session.find( Parent.class, 1 );
			Hibernate.initialize( parent.children );
			session.remove( parent );
		} );

		assertCounts( scope, 0L, 0L );
	}

	@Test
	void refreshCascadesToInitializedCollectionElements(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( parentWithChild( 1, 11, "database" ) ) );

		scope.inTransaction( session -> {
			final var parent = session.find( Parent.class, 1 );
			Hibernate.initialize( parent.children );
			final var child = parent.children.get( 0 );
			child.name = "in-memory";

			session.refresh( parent );

			assertThat( child.name ).isEqualTo( "database" );
		} );
	}

	@Test
	void evictCascadesToInitializedCollectionElements(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( parentWithChild( 1, 11, "initial" ) ) );

		scope.inTransaction( session -> {
			final var parent = session.find( Parent.class, 1 );
			Hibernate.initialize( parent.children );
			final var child = parent.children.get( 0 );

			session.evict( parent );

			assertThat( session.contains( parent ) ).isFalse();
			assertThat( session.contains( child ) ).isFalse();
		} );
	}

	@Test
	void checkOnFlushRejectsANoncascadedTransientToOne(SessionFactoryScope scope) {
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var owner = new CheckOwner( 1, new Child( 11, "transient" ) );
			session.persist( owner );
			session.flush();
		} ) ).isInstanceOf( IllegalStateException.class )
				.hasCauseInstanceOf( TransientPropertyValueException.class )
				.hasMessageContaining( "references an unsaved transient instance" );
	}

	@Test
	void actionContextsRetainResponsibilityForBidirectionalCycles(
			SessionFactoryScope scope) {
		final var a = new CycleA( 1 );
		final var b = new CycleB( 2, "initial" );
		a.b = b;
		b.a = a;

		scope.inTransaction( session -> session.persist( a ) );
		b.name = "merged";
		scope.inTransaction( session -> session.merge( a ) );

		scope.inTransaction( session -> {
			assertThat( session.find( CycleA.class, 1 ).b.name ).isEqualTo( "merged" );
			assertThat( session.createSelectionQuery( "select count(*) from CascadeCycleA", Long.class )
					.getSingleResult() ).isEqualTo( 1L );
			assertThat( session.createSelectionQuery( "select count(*) from CascadeCycleB", Long.class )
					.getSingleResult() ).isEqualTo( 1L );
		} );
	}

	private static void assertCounts(SessionFactoryScope scope, long parents, long children) {
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery( "select count(*) from CascadeParent", Long.class )
					.getSingleResult() ).isEqualTo( parents );
			assertThat( session.createSelectionQuery( "select count(*) from CascadeChild", Long.class )
					.getSingleResult() ).isEqualTo( children );
		} );
	}

	private static Parent parentWithChild(int parentId, int childId, String childName) {
		final var parent = new Parent( parentId );
		parent.addChild( new Child( childId, childName ) );
		return parent;
	}

	@Entity(name = "CascadeParent")
	@Table(name = "cascade_parent")
	static class Parent {
		@Id
		int id;

		@OneToMany(
				mappedBy = "parent",
				fetch = FetchType.LAZY,
				cascade = CascadeType.ALL,
				orphanRemoval = true
		)
		List<Child> children = new ArrayList<>();

		Parent() {
		}

		Parent(int id) {
			this.id = id;
		}

		void addChild(Child child) {
			children.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "CascadeChild")
	@Table(name = "cascade_child")
	static class Child {
		@Id
		int id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		Parent parent;

		Child() {
		}

		Child(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "CascadeCheckOwner")
	@Table(name = "cascade_check_owner")
	static class CheckOwner {
		@Id
		int id;

		@ManyToOne(fetch = FetchType.LAZY)
		Child child;

		CheckOwner() {
		}

		CheckOwner(int id, Child child) {
			this.id = id;
			this.child = child;
		}
	}

	@Entity(name = "CascadeCycleA")
	@Table(name = "cascade_cycle_a")
	static class CycleA {
		@Id
		int id;

		@OneToOne(mappedBy = "a", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		CycleB b;

		CycleA() {
		}

		CycleA(int id) {
			this.id = id;
		}
	}

	@Entity(name = "CascadeCycleB")
	@Table(name = "cascade_cycle_b")
	static class CycleB {
		@Id
		int id;

		String name;

		@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		CycleA a;

		CycleB() {
		}

		CycleB(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
