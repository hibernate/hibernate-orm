/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Hibernate;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author baranyit@gmail.com
 */
@DomainModel(annotatedClasses = {
		FetchGraphCollectionOrderByAndCriteriaJoinTest.Level1.class,
		FetchGraphCollectionOrderByAndCriteriaJoinTest.Level2.class,
		FetchGraphCollectionOrderByAndCriteriaJoinTest.Level3.class,
})
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-19207" )
public class FetchGraphCollectionOrderByAndCriteriaJoinTest {

	@Test
	public void testJoinAndFilter(SessionFactoryScope scope) {
		executeTest( scope, true, true );
	}

	@Test
	public void testNotJoinAndNotFilter(SessionFactoryScope scope) {
		executeTest( scope, false, false );
	}

	/**
	 * This case describes the problem of using a fetch graph with a collection that has an <code>@OrderBy</code> clause
	 * and a criteria join without any usage.
	 * <p>
	 * This test case is expected to fail because the <code>@OrderBy</code> is not applied to the collection in the
	 * generated SQL query.
	 * <p>
	 * The issue can also be solved by optimizing the criteria definition like in the test case
	 * <code>testJoinAndFilter</code> or <code>testNotJoinAndNotFilter</code>, but there are some program code
	 * structures where it is not possible to do it, or makes the source code more complex and less readable.
	 * <p>
	 * The required and the logical behaviour should be that the <code>@OrderBy</code> clause is applied to the
	 * collection as in the other test cases. If this problem occurs very difficult to find out the reason because
	 * this behaviour is not documented and the source code looks correct.
	 */
	@Test
	public void testJoinAndNotFilter(SessionFactoryScope scope) {
		executeTest( scope, true, false );
	}


	private void executeTest(SessionFactoryScope scope, boolean directJoin, boolean filterOnJoin) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
			JpaCriteriaQuery<Level1> criteriaQuery = builder.createQuery( Level1.class );
			JpaRoot<Level1> root = criteriaQuery.from( Level1.class );

			List<Predicate> predicates = new ArrayList<>();
			predicates.add(
					builder.equal( root.get( "id" ), 1L )
			);

			if ( directJoin || filterOnJoin ) {
				// Directly add the join to the level2 and level3 entities
				JpaJoin<Object, Object> join = root.join( "children", JoinType.INNER )
						.join( "children", JoinType.LEFT );

				if ( filterOnJoin ) {
					predicates.add(
							builder.gt( join.get( "id" ), 1L )
					);
				}
			}

			// Add all defined predicates to the criteria query
			criteriaQuery.where( builder.and( predicates.toArray(new Predicate[0]) ) );

			// Set some default root ordering (not required for the test case)
			criteriaQuery.orderBy( builder.asc( root.get( "id" ) ) );

			// Create the TypedQuery with entity graph
			RootGraphImplementor<?> graph = session.getEntityGraph( "level1_loadAll" );
			Query<Level1> query = session
					.createQuery( criteriaQuery )
					.setHint( org.hibernate.jpa.SpecHints.HINT_SPEC_FETCH_GRAPH, graph );

			// Parse the result as stream, but the problem occurs also with getResultList()
			query.getResultStream().forEach( level1 -> {

				// Check ordering of Level2 entities
				Long ordinalLevel2 = 0L;
				assertThat( level1.getChildren() ).matches( Hibernate::isInitialized );
				for ( Level2 level2 : level1.getChildren() ) {
					System.out.println( "Level2: " + level2.getOrdinal() );
					assertThat( level2.getOrdinal() ).isGreaterThan( ordinalLevel2 );
					ordinalLevel2 = level2.getOrdinal();

					// Check ordering of Level3 entities
					Long ordinalLevel3 = 0L;
					assertThat( level2.getChildren() ).matches( Hibernate::isInitialized );
					for ( Level3 level3 : level2.getChildren() ) {
						System.out.println( "Level3: " + level3.getOrdinal() );
						assertThat( level3.getOrdinal() ).isGreaterThan( ordinalLevel3 );
						ordinalLevel3 = level3.getOrdinal();
					}
				}
			} );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			final Iterator<Long> randomOrdinals = new Random().longs( 100, 999 )
					.distinct().limit( 200 ).boxed().iterator();

			for ( long l1 = 1; l1 <= 5; l1++ ) {
				final Level1 root = new Level1( l1 );

				for ( long l2 = 1; l2 <= 5; l2++ ) {
					final long l2Id = (l1 * 10) + l2;
					final Level2 child2 = new Level2( root, l2Id, randomOrdinals.next() );

					for ( long l3 = 1; l3 <= 5; l3++ ) {
						final long l3Id = (l2Id * 10) + l3;
						new Level3( child2, l3Id, randomOrdinals.next() );
					}
				}
				session.persist( root );
			}
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Level1")
	@NamedEntityGraphs({
			@NamedEntityGraph(
					name = "level1_loadAll",
					attributeNodes = {
							@NamedAttributeNode(value = "children", subgraph = "subgraph.children")
					},
					subgraphs = {
							@NamedSubgraph(
									name = "subgraph.children",
									attributeNodes = {
											@NamedAttributeNode(value = "children")
									}
							)
					}
			)
	})
	static class Level1 {
		@Id
		private Long id;

		@OneToMany(fetch = FetchType.LAZY,
				mappedBy = "parent",
				cascade = CascadeType.PERSIST)
		@OrderBy("ordinal")
		private Set<Level2> children = new LinkedHashSet<>();

		public Level1() {
		}

		public Level1(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Level2> getChildren() {
			return children;
		}

		@Override
		public String toString() {
			return "Level1 #" + id;
		}
	}

	@Entity(name = "Level2")
	static class Level2 {
		@Id
		Long id;

		Long ordinal;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Level1 parent;

		@OneToMany(fetch = FetchType.LAZY,
				mappedBy = "parent",
				cascade = CascadeType.PERSIST)
		@OrderBy("ordinal")
		private Set<Level3> children = new LinkedHashSet<>();

		public Level2() {
		}

		public Level2(Level1 parent, Long id, Long ordinal) {
			this.parent = parent;
			this.id = id;
			this.ordinal = ordinal;
			parent.getChildren().add( this );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Level1 getParent() {
			return parent;
		}

		public void setParent(Level1 parent) {
			this.parent = parent;
		}

		public Set<Level3> getChildren() {
			return children;
		}

		public Long getOrdinal() {
			return ordinal;
		}

		@Override
		public String toString() {
			return "Level1 #" + id + " $" + ordinal;
		}
	}

	@Entity(name = "Level3")
	static class Level3 {
		@Id
		Long id;

		Long ordinal;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Level2 parent;

		public Level3() {
		}

		public Level3(Level2 parent, Long id, Long ordinal) {
			this.parent = parent;
			this.id = id;
			this.ordinal = ordinal;
			parent.getChildren().add( this );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Level2 getParent() {
			return parent;
		}

		public void setParent(Level2 parent) {
			this.parent = parent;
		}

		public Long getOrdinal() {
			return ordinal;
		}

		@Override
		public String toString() {
			return "Level3 #" + id + " $" + ordinal;
		}
	}
}
