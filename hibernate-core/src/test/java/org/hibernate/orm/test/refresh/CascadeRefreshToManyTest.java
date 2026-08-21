/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.refresh;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
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

@DomainModel(
		annotatedClasses = {
				CascadeRefreshToManyTest.Parent.class,
				CascadeRefreshToManyTest.Child.class,
				CascadeRefreshToManyTest.ToOneOwner.class,
				CascadeRefreshToManyTest.ToOneTarget.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-18774")
public class CascadeRefreshToManyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L, "parent" );
			parent.addChild( new Child( 1L, "child 1" ) );
			parent.addChild( new Child( 2L, "child 2" ) );
			session.persist( parent );

			final ToOneTarget target = new ToOneTarget( 1L, "target" );
			session.persist( new ToOneOwner( 1L, "owner", target ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void refreshUninitializedCollectionRefreshesManagedChildren(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			final Child child = session.find( Child.class, 1L );
			assertThat( Hibernate.isInitialized( parent.getChildren() ) ).isFalse();

			session.createMutationQuery( "update RefreshChild c set c.name = 'updated child' where c.id = 1" )
					.executeUpdate();

			session.refresh( parent );

			assertThat( child.getName() ).isEqualTo( "updated child" );
		} );
	}

	@Test
	public void refreshInitializedCollectionDoesNotRefreshEachChildIndividually(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			Hibernate.initialize( parent.getChildren() );
			assertThat( parent.getChildren() ).hasSize( 2 );

			session.createMutationQuery( "update RefreshChild c set c.name = 'updated 1' where c.id = 1" )
					.executeUpdate();
			session.createMutationQuery( "update RefreshChild c set c.name = 'updated 2' where c.id = 2" )
					.executeUpdate();

			statementInspector.clear();
			session.refresh( parent );

			assertThat( parent.getChildren() )
					.extracting( Child::getName )
					.containsExactlyInAnyOrder( "updated 1", "updated 2" );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void refreshManyToOneDoesNotRefreshAssociationIndividually(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ToOneTarget target = session.find( ToOneTarget.class, 1L );
			final ToOneOwner owner = session.find( ToOneOwner.class, 1L );
			assertThat( owner.getTarget().getName() ).isEqualTo( "target" );

			session.createMutationQuery( "update RefreshToOneTarget t set t.name = 'updated target' where t.id = 1" )
					.executeUpdate();

			statementInspector.clear();
			session.refresh( owner );

			assertThat( target.getName() ).isEqualTo( "updated target" );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-13284")
	public void refreshToOneViaQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ToOneTarget target = session.find( ToOneTarget.class, 1L );
			final ToOneOwner owner = session.find( ToOneOwner.class, 1L );
			assertThat( owner.getTarget().getName() ).isEqualTo( "target" );

			session.createMutationQuery( "update RefreshToOneTarget t set t.name = 'updated target' where t.id = 1" )
					.executeUpdate();

			statementInspector.clear();
			session.createSelectionQuery( "from RefreshToOneOwner o join fetch o.target", ToOneOwner.class )
					.setCacheMode( CacheMode.REFRESH_SESSION )
					.getResultList();

			assertThat( target.getName() ).isEqualTo( "updated target" );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-13284")
	public void refreshCollectionViaQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			Hibernate.initialize( parent.getChildren() );
			assertThat( parent.getChildren() ).hasSize( 2 );

			session.createMutationQuery( "update RefreshChild c set c.name = 'updated 1' where c.id = 1" )
					.executeUpdate();
			session.createMutationQuery( "update RefreshChild c set c.name = 'updated 2' where c.id = 2" )
					.executeUpdate();

			statementInspector.clear();
			session.createSelectionQuery( "from RefreshParent p left join fetch p.children", Parent.class )
					.setCacheMode( CacheMode.REFRESH_SESSION )
					.getResultList();

			assertThat( parent.getChildren() )
					.extracting( Child::getName )
					.containsExactlyInAnyOrder( "updated 1", "updated 2" );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Entity(name = "RefreshParent")
	@Table(name = "refresh_parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "RefreshChild")
	@Table(name = "refresh_child")
	public static class Child {
		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "RefreshToOneOwner")
	@Table(name = "refresh_to_one_owner")
	public static class ToOneOwner {
		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
		private ToOneTarget target;

		public ToOneOwner() {
		}

		public ToOneOwner(Long id, String name, ToOneTarget target) {
			this.id = id;
			this.name = name;
			this.target = target;
		}

		public ToOneTarget getTarget() {
			return target;
		}
	}

	@Entity(name = "RefreshToOneTarget")
	@Table(name = "refresh_to_one_target")
	public static class ToOneTarget {
		@Id
		private Long id;

		private String name;

		public ToOneTarget() {
		}

		public ToOneTarget(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
