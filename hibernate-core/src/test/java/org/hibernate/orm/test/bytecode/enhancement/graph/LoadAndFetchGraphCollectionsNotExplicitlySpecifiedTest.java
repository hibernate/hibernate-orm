/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.graph;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LoadAndFetchGraphCollectionsNotExplicitlySpecifiedTest.RootEntity.class,
				LoadAndFetchGraphCollectionsNotExplicitlySpecifiedTest.ContainedEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18489")
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@EnhancementOptions(lazyLoading = true)
public class LoadAndFetchGraphCollectionsNotExplicitlySpecifiedTest {

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 0; i < 3; ++i ) {
				var root = new RootEntity( i * 100 );
				long j = i * 100;
				session.persist( root );

				var contained = new ContainedEntity( ++j );
				session.persist( contained );

				root.addEagerContainedEntity( contained );

				var contained2 = new ContainedEntity( ++j );
				session.persist( contained2 );

				root.addLazyContainedEntity( contained2 );
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void queryWithFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
					.setFetchSize( 100 )
					// Selecting multiple entities to make sure we don't have side effects (e.g. some context shared across entity instances)
					.setParameter( "ids", List.of( 0L, 100L, 200L ) );

			var graph = session.createEntityGraph( RootEntity.class );
			graph.addAttributeNode( "lazyContainedEntities" );
			query.applyGraph( graph, GraphSemantic.FETCH );

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
			for ( var rootEntity : resultList ) {
				// GraphSemantic.FETCH, so eagerContainedEntities is lazy because it's not present in the EntityGraph
				assertThat( Hibernate.isInitialized( rootEntity.getEagerContainedEntities() )).isFalse();
				assertThat( Hibernate.isInitialized( rootEntity.getLazyContainedEntities() ) ).isTrue();
			}
		} );
	}

	@Test
	void queryWithLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var query = session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
					.setFetchSize( 100 )
					// Selecting multiple entities to make sure we don't have side effects (e.g. some context shared across entity instances)
					.setParameter( "ids", List.of( 0L, 100L, 200L ) );

			var graph = session.createEntityGraph( RootEntity.class );
			graph.addAttributeNode( "lazyContainedEntities" );
			query.applyGraph( graph, GraphSemantic.LOAD );

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
			for ( var rootEntity : resultList ) {
				// GraphSemantic.LOAD, eagerContainedEntities maintains is eagerness
				assertThat( Hibernate.isInitialized( rootEntity.getEagerContainedEntities() )).isTrue();
				assertThat( Hibernate.isInitialized( rootEntity.getLazyContainedEntities() ) ).isTrue();
			}
		} );
	}

	@Test
	void queryWithNoEntityGraph(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			var query = session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
					.setFetchSize( 100 )
					// Selecting multiple entities to make sure we don't have side effects (e.g. some context shared across entity instances)
					.setParameter( "ids", List.of( 0L, 100L, 200L ) );

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
			for ( var rootEntity : resultList ) {
				assertThat( Hibernate.isInitialized( rootEntity.getEagerContainedEntities() ) ).isTrue();
				assertThat( Hibernate.isInitialized( rootEntity.getLazyContainedEntities() ) ).isFalse();
			}
		} );
	}

	@Entity(name = "RootEntity")
	static class RootEntity {

		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		@JoinColumn(name = "eager_id")
		private List<ContainedEntity> eagerContainedEntities;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn(name = "lazy_id")
		private List<ContainedEntity> lazyContainedEntities;

		public RootEntity() {
		}

		public RootEntity(Long id) {
			this.id = id;
			this.name = "Name #" + id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<ContainedEntity> getEagerContainedEntities() {
			return eagerContainedEntities;
		}

		public void setEagerContainedEntities(List<ContainedEntity> eagerContainedEntities) {
			this.eagerContainedEntities = eagerContainedEntities;
		}

		public List<ContainedEntity> getLazyContainedEntities() {
			return lazyContainedEntities;
		}

		public void setLazyContainedEntities(List<ContainedEntity> lazyContainedEntities) {
			this.lazyContainedEntities = lazyContainedEntities;
		}

		public void addEagerContainedEntity(ContainedEntity containedEntity) {
			if ( eagerContainedEntities == null ) {
				eagerContainedEntities = new ArrayList<>();
			}
			eagerContainedEntities.add( containedEntity );
		}

		public void addLazyContainedEntity(ContainedEntity containedEntity) {
			if ( lazyContainedEntities == null ) {
				lazyContainedEntities = new ArrayList<>();
			}
			lazyContainedEntities.add( containedEntity );
		}

		@Override
		public String toString() {
			return "RootEntity#" + id;
		}
	}

	@Entity(name = "ContainedEntity")
	static class ContainedEntity {

		@Id
		private Long id;

		private String name;

		public ContainedEntity() {
		}

		public ContainedEntity(Long id) {
			this.id = id;
			this.name = "Name #" + id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "ContainedEntity#" + id;
		}
	}
}
