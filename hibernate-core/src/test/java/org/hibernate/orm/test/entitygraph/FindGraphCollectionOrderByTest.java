/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.AvailableHints;

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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		FindGraphCollectionOrderByTest.Level1.class,
		FindGraphCollectionOrderByTest.Level2.class,
		FindGraphCollectionOrderByTest.Level3.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18436" )
public class FindGraphCollectionOrderByTest {
	@Test
	public void testLoadGraphFind(SessionFactoryScope scope) {
		executeTest( scope, AvailableHints.HINT_SPEC_LOAD_GRAPH, true );
	}

	@Test
	public void testLoadGraphQuery(SessionFactoryScope scope) {
		executeTest( scope, AvailableHints.HINT_SPEC_LOAD_GRAPH, false );
	}

	@Test
	public void testFetchGraphFind(SessionFactoryScope scope) {
		executeTest( scope, AvailableHints.HINT_SPEC_FETCH_GRAPH, true );
	}

	@Test
	public void testFetchGraphQuery(SessionFactoryScope scope) {
		executeTest( scope, AvailableHints.HINT_SPEC_FETCH_GRAPH, false );
	}


	private void executeTest(SessionFactoryScope scope, String hint, boolean find) {
		scope.inTransaction( session -> {
			final RootGraphImplementor<?> graph = session.getEntityGraph( "level1_loadAll" );
			final Level1 root = find ? session.find( Level1.class, 1L, Map.of( hint, graph ) ) :
					session.createQuery( "from Level1 where id = :id", Level1.class )
							.setParameter( "id", 1L )
							.setHint( hint, graph )
							.getSingleResult();

			assertThat( root.getChildren() ).matches( Hibernate::isInitialized ).hasSize( 3 );
			long i = 1;
			for ( final Level2 child : root.getChildren() ) {
				if ( i == 2 ) {
					assertThat( child.getChildren() ).matches( Hibernate::isInitialized ).hasSize( 1 );
				}
				assertThat( child.getId() ).as( "Children not in expected order" ).isEqualTo( i++ );
			}
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Level1 root = new Level1( 1L );
			new Level2( root, 1L );
			final Level2 child2 = new Level2( root, 2L );
			new Level2( root, 3L );
			new Level3( child2, 1L );
			session.persist( root );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Level1" )
	@NamedEntityGraphs( {
			@NamedEntityGraph(
					name = "level1_loadAll",
					attributeNodes = {
							@NamedAttributeNode( value = "children", subgraph = "subgraph.children" )
					},
					subgraphs = {
							@NamedSubgraph(
									name = "subgraph.children",
									attributeNodes = {
											@NamedAttributeNode( value = "children" )
									}
							)
					}
			)
	} )
	static class Level1 {
		@Id
		private Long id;

		@OneToMany( fetch = FetchType.LAZY,
				mappedBy = "parent",
				cascade = CascadeType.PERSIST )
		@OrderBy( "id" )
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
	}

	@Entity( name = "Level2" )
	static class Level2 {
		@Id
		Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "parent_id" )
		private Level1 parent;

		@OneToMany( fetch = FetchType.LAZY,
				mappedBy = "parent",
				cascade = CascadeType.PERSIST )
		@OrderBy( "id" )
		private Set<Level3> children = new LinkedHashSet<>();

		public Level2() {
		}

		public Level2(Level1 parent, Long id) {
			this.parent = parent;
			this.id = id;
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
	}

	@Entity( name = "Level3" )
	static class Level3 {
		@Id
		Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "parent_id" )
		private Level2 parent;

		public Level3() {
		}

		public Level3(Level2 parent, Long id) {
			this.parent = parent;
			this.id = id;
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
	}
}
