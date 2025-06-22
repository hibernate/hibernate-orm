/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		SubselectOneToManyTest.GrandParent.class,
		SubselectOneToManyTest.Parent.class,
		SubselectOneToManyTest.Child.class
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16213" )
public class SubselectOneToManyTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent1 = new Parent();
			final Child child1 = new Child( parent1, "a" );
			final Child child2 = new Child( parent1, "b" );
			final Child child3 = new Child( parent1, "c" );
			parent1.getChildren().addAll( List.of( child1, child2, child3 ) );
			session.persist( parent1 );
			final GrandParent grandParent = new GrandParent( "Luigi" );
			final Parent parent2 = new Parent( grandParent );
			final Child child4 = new Child( parent2, "d" );
			final Child child5 = new Child( parent2, "e" );
			parent2.getChildren().addAll( List.of( child4, child5 ) );
			session.persist( grandParent );
			session.persist( parent2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
			session.createMutationQuery( "delete from GrandParent" ).executeUpdate();
		} );
	}

	@Test
	public void testIsNull(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Parent> query = cb.createQuery( Parent.class );
			final Root<Parent> root = query.from( Parent.class );
			query.select( root ).where( cb.isNull( root.get( "grandParent" ) ) );
			final Parent parent = session.createQuery( query ).getSingleResult();
			assertThat( parent.getId() ).isEqualTo( 1L );
			assertThat( parent.getGrandParent() ).isNull();
			assertThat( parent.getChildren() ).hasSize( 3 );
			statementInspector.assertExecutedCount( 2 ); // 1 query for parent, 1 for children
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );
		} );
	}

	@Test
	public void testShouldJoin(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Parent> query = cb.createQuery( Parent.class );
			final Root<Parent> root = query.from( Parent.class );
			query.select( root ).where( cb.equal( root.get( "grandParent" ).get( "name" ), "Luigi" ) );
			final Parent parent = session.createQuery( query ).getSingleResult();
			assertThat( parent.getId() ).isEqualTo( 2L );
			assertThat( parent.getGrandParent().getName() ).isEqualTo( "Luigi" );
			assertThat( parent.getChildren() ).hasSize( 2 );
			statementInspector.assertExecutedCount( 3 ); // 1 query for parent, 1 for grandparent, 1 for children
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 0 );
		} );
	}

	@Entity( name = "GrandParent" )
	public static class GrandParent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public GrandParent() {
		}

		public GrandParent(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "grand_parent" )
		private GrandParent grandParent;

		@OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		@Fetch( value = FetchMode.SUBSELECT )
		@OrderBy( "name ASC" )
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(GrandParent grandParent) {
			this.grandParent = grandParent;
		}

		public Long getId() {
			return id;
		}

		public GrandParent getGrandParent() {
			return grandParent;
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity( name = "Child" )
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "parent_id", nullable = false )
		private Parent parent;

		private String name;

		public Child() {
		}

		public Child(Parent parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

		public String getName() {
			return name;
		}
	}
}
