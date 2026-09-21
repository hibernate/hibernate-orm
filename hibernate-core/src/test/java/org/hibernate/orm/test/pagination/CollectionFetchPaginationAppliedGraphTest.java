/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		CollectionFetchPaginationAppliedGraphTest.Parent.class,
		CollectionFetchPaginationAppliedGraphTest.Child.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20911")
public class CollectionFetchPaginationAppliedGraphTest {

	private static final int PARENT_COUNT = 5;
	private static final int PAGE_SIZE = 2;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 0; i < PARENT_COUNT; i++ ) {
				final Parent parent = new Parent( i, "Parent " + i );
				final Child child = new Child( i, "Child " + i );
				parent.addChild( child );
				session.persist( parent );
				session.persist( child );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void appliedGraphOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Parent> parents = selectParents( session, ChildrenLeftJoin.NONE )
					.setEntityGraph( createChildrenFetchGraph( session ), GraphSemantic.FETCH )
					.setMaxResults( PAGE_SIZE )
					.list();

			assertEquals( PAGE_SIZE, parents.size() );
			assertChildrenFetched( parents );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-20911",
			reason = "The limit is stripped from the SQL and no in-memory slice is applied")
	void appliedGraphWithChildrenLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Parent> parents = selectParents( session, ChildrenLeftJoin.UNREFERENCED )
					.setEntityGraph( createChildrenFetchGraph( session ), GraphSemantic.FETCH )
					.setMaxResults( PAGE_SIZE )
					.list();

			assertEquals( PAGE_SIZE, parents.size() );
			assertChildrenFetched( parents );
		} );
	}

	@Test
	void appliedGraphWithChildrenLeftJoinReferencedInWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Parent> parents = selectParents( session, ChildrenLeftJoin.REFERENCED_IN_WHERE )
					.setEntityGraph( createChildrenFetchGraph( session ), GraphSemantic.FETCH )
					.setMaxResults( PAGE_SIZE )
					.list();

			assertEquals( PAGE_SIZE, parents.size() );
			assertChildrenFetched( parents );
		} );
	}

	@Test
	void appliedGraphWithChildrenLeftJoinNoPagination(SessionFactoryScope scope) {
		final CollectingStatementObserver sql = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			sql.clear();

			final List<Parent> parents = selectParents( session, ChildrenLeftJoin.UNREFERENCED )
					.setEntityGraph( createChildrenFetchGraph( session ), GraphSemantic.FETCH )
					.list();

			assertEquals( PARENT_COUNT, parents.size() );
			assertChildrenFetched( parents );
			// Without pagination the graph fetch keeps reusing the existing join
			assertEquals( 1, sql.getSqlQueries().get( 0 ).toLowerCase().split( "graph_child", -1 ).length - 1 );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-20911",
			reason = "The limit is stripped from the SQL and no in-memory slice is applied")
	void appliedGraphWithChildrenLeftJoinHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Parent> parents = session.createSelectionQuery(
							"select p from Parent p left join p.children c order by p.id",
							Parent.class
					)
					.setEntityGraph( createChildrenFetchGraph( session ), GraphSemantic.FETCH )
					.setMaxResults( PAGE_SIZE )
					.list();

			assertEquals( PAGE_SIZE, parents.size() );
			assertChildrenFetched( parents );
		} );
	}

	private static void assertChildrenFetched(List<Parent> parents) {
		for ( Parent parent : parents ) {
			assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
			assertEquals( 1, parent.getChildren().size() );
		}
	}

	private SelectionQuery<Parent> selectParents(org.hibernate.Session session, ChildrenLeftJoin childrenLeftJoin) {
		final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		final CriteriaQuery<Parent> criteriaQuery = criteriaBuilder.createQuery( Parent.class );
		final Root<Parent> root = criteriaQuery.from( Parent.class );
		if ( childrenLeftJoin != ChildrenLeftJoin.NONE ) {
			final Join<Parent, Child> children = root.join( "children", JoinType.LEFT );
			if ( childrenLeftJoin == ChildrenLeftJoin.REFERENCED_IN_WHERE ) {
				criteriaQuery.where( criteriaBuilder.isNotNull( children.get( "name" ) ) );
			}
		}
		criteriaQuery.select( root ).orderBy( criteriaBuilder.asc( root.get( "id" ) ) );

		return session.createQuery( criteriaQuery );
	}

	private static EntityGraph<Parent> createChildrenFetchGraph(org.hibernate.Session session) {
		final EntityGraph<Parent> graph = session.createEntityGraph( Parent.class );
		graph.addAttributeNode( "children" );
		return graph;
	}

	private enum ChildrenLeftJoin {
		NONE,
		UNREFERENCED,
		REFERENCED_IN_WHERE
	}

	@Entity(name = "Parent")
	@Table(name = "graph_parent")
	public static class Parent {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "parent")
		private Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "Child")
	@Table(name = "graph_child")
	public static class Child {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}
}
