/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.refresh;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When an entity has several {@code cascade=REFRESH} collections, a refresh must:
 * <ul>
 *     <li>only immediately (re)fetch the collections that were already initialized (HHH-12867);</li>
 *     <li>join-fetch just the <em>first</em> such collection, loading the rest through
 *         secondary selects (a single collection can be joined without producing a cartesian product).</li>
 * </ul>
 * This exercises the interaction between the two: which collection ends up being the joined one
 * depends on which collections are actually fetched, not on their declaration order.
 */
@DomainModel(
		annotatedClasses = {
				RefreshMultipleCollectionsTest.Parent.class,
				RefreshMultipleCollectionsTest.ChildA.class,
				RefreshMultipleCollectionsTest.ChildB.class,
				RefreshMultipleCollectionsTest.ChildC.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-12867")
public class RefreshMultipleCollectionsTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			parent.addA( new ChildA( 1L ) );
			parent.addA( new ChildA( 2L ) );
			parent.addB( new ChildB( 1L ) );
			parent.addB( new ChildB( 2L ) );
			parent.addC( new ChildC( 1L ) );
			parent.addC( new ChildC( 2L ) );
			session.persist( parent );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void allInitializedFetchesEachOnlyFirstJoined(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			Hibernate.initialize( parent.getaChildren() );
			Hibernate.initialize( parent.getbChildren() );
			Hibernate.initialize( parent.getcChildren() );

			inspector.clear();
			session.refresh( parent );

			// All three stay initialized...
			assertThat( Hibernate.isInitialized( parent.getaChildren() ) ).isTrue();
			assertThat( Hibernate.isInitialized( parent.getbChildren() ) ).isTrue();
			assertThat( Hibernate.isInitialized( parent.getcChildren() ) ).isTrue();
			// ...but only three statements are issued: entity + first collection joined,
			// then a secondary select for each of the two remaining collections.
			assertThat( inspector.getSqlQueries() ).hasSize( 3 );
			inspector.assertNumberOfJoins( 0, 1 );
			inspector.assertNumberOfJoins( 1, 0 );
			inspector.assertNumberOfJoins( 2, 0 );
		} );
	}

	@Test
	public void onlyMiddleCollectionInitializedIsTheOneJoined(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			// Initialize only the *second* declared collection.
			Hibernate.initialize( parent.getbChildren() );
			assertThat( Hibernate.isInitialized( parent.getaChildren() ) ).isFalse();
			assertThat( Hibernate.isInitialized( parent.getcChildren() ) ).isFalse();

			inspector.clear();
			session.refresh( parent );

			// Only 'b' was initialized, so only 'b' is fetched - and because it is the only
			// fetched collection it becomes the one that is join-fetched into the entity query.
			assertThat( Hibernate.isInitialized( parent.getbChildren() ) ).isTrue();
			assertThat( Hibernate.isInitialized( parent.getaChildren() ) ).isFalse();
			assertThat( Hibernate.isInitialized( parent.getcChildren() ) ).isFalse();
			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void subsetInitializedFetchesOnlyThoseFirstJoined(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			// Initialize the first and last declared collections, leaving the middle one lazy.
			Hibernate.initialize( parent.getaChildren() );
			Hibernate.initialize( parent.getcChildren() );

			inspector.clear();
			session.refresh( parent );

			assertThat( Hibernate.isInitialized( parent.getaChildren() ) ).isTrue();
			assertThat( Hibernate.isInitialized( parent.getcChildren() ) ).isTrue();
			// The uninitialized one is neither fetched nor initialized.
			assertThat( Hibernate.isInitialized( parent.getbChildren() ) ).isFalse();
			// Two fetched collections: first joined, the other via a secondary select.
			assertThat( inspector.getSqlQueries() ).hasSize( 2 );
			inspector.assertNumberOfJoins( 0, 1 );
			inspector.assertNumberOfJoins( 1, 0 );
		} );
	}

	@Test
	public void noneInitializedFetchesNoCollections(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );

			inspector.clear();
			session.refresh( parent );

			assertThat( Hibernate.isInitialized( parent.getaChildren() ) ).isFalse();
			assertThat( Hibernate.isInitialized( parent.getbChildren() ) ).isFalse();
			assertThat( Hibernate.isInitialized( parent.getcChildren() ) ).isFalse();
			// Only the entity row is reloaded, with no collection join.
			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "mc_parent")
	public static class Parent {
		@Id
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private List<ChildA> aChildren = new ArrayList<>();

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private List<ChildB> bChildren = new ArrayList<>();

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private List<ChildC> cChildren = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public List<ChildA> getaChildren() {
			return aChildren;
		}

		public List<ChildB> getbChildren() {
			return bChildren;
		}

		public List<ChildC> getcChildren() {
			return cChildren;
		}

		public void addA(ChildA child) {
			aChildren.add( child );
			child.parent = this;
		}

		public void addB(ChildB child) {
			bChildren.add( child );
			child.parent = this;
		}

		public void addC(ChildC child) {
			cChildren.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "ChildA")
	@Table(name = "mc_child_a")
	public static class ChildA {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public ChildA() {
		}

		public ChildA(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildB")
	@Table(name = "mc_child_b")
	public static class ChildB {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public ChildB() {
		}

		public ChildB(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildC")
	@Table(name = "mc_child_c")
	public static class ChildC {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public ChildC() {
		}

		public ChildC(Long id) {
			this.id = id;
		}
	}
}
