/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				IncludeAllOrNoneGraphTest.RootEntity.class,
				IncludeAllOrNoneGraphTest.ContainedEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16175")
public class IncludeAllOrNoneGraphTest {

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 0; i < 3; ++i ) {
				RootEntity entity = new RootEntity( i * 100 );
				ContainedEntity containedEager = new ContainedEntity( i * 100 + 1 );
				entity.setContainedEager( containedEager );
				containedEager.setContainingEager( entity );

				session.persist( containedEager );
				session.persist( entity );

				ContainedEntity containedLazy = new ContainedEntity( i * 100 + 2 );
				entity.getContainedLazy().add( containedLazy );

				session.persist( containedLazy );
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			scope.getSessionFactory().getSchemaManager().truncate();
		} );
	}

	@Test
	void includeAll_fetchGraph(SessionFactoryScope scope) {
		// Before HHH-16175 gets fixed, this leads to AssertionError in StandardEntityGraphTraversalStateImpl.traverse
		testLoadingWithGraph( scope, RootEntity.GRAPH_INCLUDE_ALL, GraphSemantic.FETCH, true, true );
	}

	@Test
	void includeAll_loadGraph(SessionFactoryScope scope) {
		// Before HHH-16175 gets fixed, this leads to AssertionError in StandardEntityGraphTraversalStateImpl.traverse
		testLoadingWithGraph( scope, RootEntity.GRAPH_INCLUDE_ALL, GraphSemantic.LOAD, true, true );
	}

	@Test
	void includeNone_fetchGraph(SessionFactoryScope scope) {
		testLoadingWithGraph( scope, RootEntity.GRAPH_INCLUDE_NONE, GraphSemantic.FETCH, false, false );
	}

	@Test
	void includeNone_loadGraph(SessionFactoryScope scope) {
		testLoadingWithGraph( scope, RootEntity.GRAPH_INCLUDE_NONE, GraphSemantic.LOAD, true, false );
	}

	private void testLoadingWithGraph(
			SessionFactoryScope scope, String graphName, GraphSemantic graphSemantic,
			boolean expectContainedEagerInitialized, boolean expectContainedLazyInitialized) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
								.applyGraph( session.getEntityGraph( graphName ), graphSemantic )
								.setFetchSize( 100 )
								.setParameter( "ids", List.of( 0L, 100L, 200L ) )
								.list() )
					.isNotEmpty()
					.allSatisfy( loaded -> assertThat( Hibernate.isInitialized( loaded.getContainedEager() ) )
							.as( "Hibernate::isInitialized for .getContainedEager() on %s", loaded )
							.isEqualTo( expectContainedEagerInitialized ) )
					.allSatisfy( loaded -> assertThat( Hibernate.isInitialized( loaded.getContainedLazy() ) )
							.as( "Hibernate::isInitialized for .getContainedLazy() on %s", loaded )
							.isEqualTo( expectContainedLazyInitialized ) );
		} );
	}

	@Entity(name = "RootEntity")
	@NamedEntityGraph(
			name = RootEntity.GRAPH_INCLUDE_ALL,
			includeAllAttributes = true
	)
	@NamedEntityGraph(
			name = RootEntity.GRAPH_INCLUDE_NONE
	)
	static class RootEntity {

		public static final String GRAPH_INCLUDE_ALL = "graph-include-all";
		public static final String GRAPH_INCLUDE_NONE = "graph-include-none";

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		private ContainedEntity containedEager;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private List<ContainedEntity> containedLazy = new ArrayList<>();

		public RootEntity() {
		}

		public RootEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ContainedEntity getContainedEager() {
			return containedEager;
		}

		public void setContainedEager(ContainedEntity containedEager) {
			this.containedEager = containedEager;
		}

		public List<ContainedEntity> getContainedLazy() {
			return containedLazy;
		}

		public void setContainedLazy(List<ContainedEntity> containedLazy) {
			this.containedLazy = containedLazy;
		}
	}

	@Entity(name = "ContainedEntity")
	static class ContainedEntity {

		@Id
		private Long id;

		@OneToOne(mappedBy = "containedEager")
		private RootEntity containingEager;

		public ContainedEntity() {
		}

		public ContainedEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public RootEntity getContainingEager() {
			return containingEager;
		}

		public void setContainingEager(RootEntity containingEager) {
			this.containingEager = containingEager;
		}

	}
}
