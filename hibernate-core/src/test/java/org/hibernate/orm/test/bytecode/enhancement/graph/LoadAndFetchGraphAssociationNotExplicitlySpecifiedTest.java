/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.graph.GraphSemantic.FETCH;

/**
 * Checks that associations that are **not** explicitly specified in a fetch/load graph
 * are correctly initialized (or not) according to the graph semantics,
 * for several association topologies.
 */
@DomainModel(
		annotatedClasses = {
				LoadAndFetchGraphAssociationNotExplicitlySpecifiedTest.RootEntity.class,
				LoadAndFetchGraphAssociationNotExplicitlySpecifiedTest.ContainedEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-18489")
@BytecodeEnhanced()
@EnhancementOptions(lazyLoading = true)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = ""))
public class LoadAndFetchGraphAssociationNotExplicitlySpecifiedTest {

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 0; i < 3; ++i ) {
				RootEntity root = new RootEntity( i * 100 );

				long j = i * 100;
				root.setLazyOneToOneOwned( new ContainedEntity( ++j ) );
				root.setLazyManyToOneOwned( new ContainedEntity( ++j ) );
				root.setEagerOneToOneOwned( new ContainedEntity( ++j ) );
				root.setEagerManyToOneOwned( new ContainedEntity( ++j ) );

				session.persist( root );

				ContainedEntity contained;

				contained = new ContainedEntity( ++j );
				root.setLazyOneToOneUnowned( contained );
				contained.setInverseSideOfLazyOneToOneUnowned( root );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setEagerOneToOneUnowned( contained );
				contained.setInverseSideOfEagerOneToOneUnowned( root );
				session.persist( contained );
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	// Arguments for the parameterized test below
	List<Arguments> queryWithEntityGraph() {
		List<Arguments> args = new ArrayList<>();
		for ( GraphSemantic graphSemantic : GraphSemantic.values() ) {
			for ( String propertyName : RootEntity.LAZY_PROPERTY_NAMES ) {
				args.add( Arguments.of( graphSemantic, propertyName ) );
			}
			for ( String propertyName : RootEntity.EAGER_PROPERTY_NAMES ) {
				args.add( Arguments.of( graphSemantic, propertyName ) );
			}
		}
		// Also test without a graph, for reference
		args.add( Arguments.of( null, null ) );
		return args;
	}

	@Test
	public void testWithFetchGraph(SessionFactoryScope scope) {
		String propertySpecifiedInGraph = "eagerOneToOneOwned";
		scope.inTransaction( session -> {
			var sqlStatementInspector = scope.getCollectingStatementInspector();
			sqlStatementInspector.clear();
			var query = session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
					.setFetchSize( 100 )
					// Selecting multiple entities to make sure we don't have side effects (e.g. some context shared across entity instances)
					.setParameter( "ids", List.of( 0L, 100L, 200L ) );

			var graph = session.createEntityGraph( RootEntity.class );
			graph.addAttributeNode( propertySpecifiedInGraph );
			query.applyGraph( graph, FETCH );

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
			for ( String propertyName : RootEntity.LAZY_PROPERTY_NAMES ) {
				var expectInitialized = propertyName.equals( propertySpecifiedInGraph );
				assertAssociationInitialized( resultList, propertyName, expectInitialized, sqlStatementInspector );
			}
			for ( String propertyName : RootEntity.EAGER_PROPERTY_NAMES ) {
				var expectInitialized = propertyName.equals( propertySpecifiedInGraph );
				assertAssociationInitialized( resultList, propertyName, expectInitialized, sqlStatementInspector );
			}
		} );
	}

	@ParameterizedTest
	@MethodSource
	public void queryWithEntityGraph(GraphSemantic graphSemantic, String propertySpecifiedInGraph, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var sqlStatementInspector = scope.getCollectingStatementInspector();
			sqlStatementInspector.clear();
			var query = session.createQuery( "select e from RootEntity e where id in (:ids)", RootEntity.class )
					.setFetchSize( 100 )
					// Selecting multiple entities to make sure we don't have side effects (e.g. some context shared across entity instances)
					.setParameter( "ids", List.of( 0L, 100L, 200L ) );

			if ( graphSemantic != null ) {
				var graph = session.createEntityGraph( RootEntity.class );
				graph.addAttributeNode( propertySpecifiedInGraph );
				query.applyGraph( graph, graphSemantic );
			} // else just run the query without a graph

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
			for ( String propertyName : RootEntity.LAZY_PROPERTY_NAMES ) {
				var expectInitialized = propertyName.equals( propertySpecifiedInGraph );
				assertAssociationInitialized( resultList, propertyName, expectInitialized, sqlStatementInspector );
			}
			for ( String propertyName : RootEntity.EAGER_PROPERTY_NAMES ) {
				var expectInitialized = propertyName.equals( propertySpecifiedInGraph )
						// Under LOAD semantics, or when not using graphs,
						// eager properties also get loaded (even if not specified in the graph).
						|| GraphSemantic.LOAD.equals( graphSemantic ) || graphSemantic == null;
				assertAssociationInitialized( resultList, propertyName, expectInitialized, sqlStatementInspector );
			}
		} );
	}

	private void assertAssociationInitialized(
			List<RootEntity> resultList,
			String propertyName,
			boolean expectInitialized,
			SQLStatementInspector sqlStatementInspector) {
		for ( var rootEntity : resultList ) {
			sqlStatementInspector.clear();
			if ( propertyName.endsWith( "Unowned" ) ) {
				final Supplier<ContainedEntity> supplier;
				switch ( propertyName ) {
					case ( "lazyOneToOneUnowned" ):
						supplier = () -> rootEntity.getLazyOneToOneUnowned();
						break;
					case ( "eagerOneToOneUnowned" ):
						supplier = () -> rootEntity.getEagerOneToOneUnowned();
						break;
					default:
						supplier = null;
						fail( "unknown association property name : " + propertyName );
				}
				assertUnownedAssociationLazyness(
						supplier,
						rootEntity,
						propertyName,
						expectInitialized,
						sqlStatementInspector
				);
			}
			else {
				final Supplier<ContainedEntity> supplier;
				switch ( propertyName ) {
					case "lazyOneToOneOwned":
						supplier = () -> rootEntity.getLazyOneToOneOwned();
						break;
					case "lazyManyToOneOwned":
						supplier = () -> rootEntity.getLazyManyToOneOwned();
						break;
					case "eagerOneToOneOwned":
						supplier = () -> rootEntity.getEagerOneToOneOwned();
						break;
					case "eagerManyToOneOwned":
						supplier = () -> rootEntity.getEagerManyToOneOwned();
						break;
					default:
						supplier = null;
						fail( "unknown association property name : " + propertyName );
				}
				assertOwnedAssociationLazyness(
						supplier,
						propertyName,
						expectInitialized,
						sqlStatementInspector
				);
			}
		}
	}

	private static void assertUnownedAssociationLazyness(
			Supplier<ContainedEntity> associationSupplier,
			RootEntity rootEntity,
			String associationName,
			boolean expectInitialized,
			SQLStatementInspector sqlStatementInspector) {
		// for an unowned lazy association the value is null and accessing the association triggers its initialization
		assertThat( Hibernate.isPropertyInitialized( rootEntity, associationName ) )
				.as( associationName + " association expected to be initialized ? expected is :" + expectInitialized + " but it's not " )
				.isEqualTo( expectInitialized );
		if ( !expectInitialized ) {
			var containedEntity = associationSupplier.get();
			sqlStatementInspector.assertExecutedCount( 1 );
			assertThat( Hibernate.isInitialized( containedEntity ) );
			sqlStatementInspector.clear();

			assertThat( containedEntity ).isNotNull();
			associationSupplier.get().getName();
			sqlStatementInspector.assertExecutedCount( 0 );
		}
	}

	private static void assertOwnedAssociationLazyness(
			Supplier<ContainedEntity> associationSupplier,
			String associationName,
			boolean expectInitialized,
			SQLStatementInspector sqlStatementInspector) {
		// for an owned lazy association the value is an enhanced proxy, Hibernate.isPropertyInitialized( rootEntity, "lazyManyToOneOwned" ) returns true.
		// accessing the association does not trigger its initialization
		assertThat( Hibernate.isInitialized( associationSupplier.get() ) )
				.as( associationName + " association expected to be initialized ? expected is :" + expectInitialized + " but it's not " )
				.isEqualTo( expectInitialized );
		if ( !expectInitialized ) {
			var containedEntity = associationSupplier.get();
			sqlStatementInspector.assertExecutedCount( 0 );

			containedEntity.getName();
			sqlStatementInspector.assertExecutedCount( 1 );
			assertThat( Hibernate.isInitialized( containedEntity ) ).isTrue();
			sqlStatementInspector.clear();

			assertThat( containedEntity ).isNotNull();
			associationSupplier.get().getName();
			sqlStatementInspector.assertExecutedCount( 0 );
		}
	}

	@Entity(name = "RootEntity")
	static class RootEntity {

		public static final Set<String> LAZY_PROPERTY_NAMES = Set.of(
				"lazyOneToOneOwned", "lazyManyToOneOwned", "lazyOneToOneUnowned"
		);

		public static final Set<String> EAGER_PROPERTY_NAMES = Set.of(
				"eagerOneToOneOwned", "eagerManyToOneOwned", "eagerOneToOneUnowned"

		);

		@Id
		private Long id;

		private String name;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private ContainedEntity lazyOneToOneOwned;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private ContainedEntity lazyManyToOneOwned;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "inverseSideOfLazyOneToOneUnowned")
		private ContainedEntity lazyOneToOneUnowned;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		private ContainedEntity eagerOneToOneOwned;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		private ContainedEntity eagerManyToOneOwned;

		@OneToOne(fetch = FetchType.EAGER, mappedBy = "inverseSideOfEagerOneToOneUnowned")
		private ContainedEntity eagerOneToOneUnowned;

		public RootEntity() {
		}

		public RootEntity(Long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "RootEntity#" + id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ContainedEntity getLazyOneToOneOwned() {
			return lazyOneToOneOwned;
		}

		public void setLazyOneToOneOwned(ContainedEntity lazyOneToOneOwned) {
			this.lazyOneToOneOwned = lazyOneToOneOwned;
		}

		public ContainedEntity getLazyManyToOneOwned() {
			return lazyManyToOneOwned;
		}

		public void setLazyManyToOneOwned(ContainedEntity lazyManyToOneOwned) {
			this.lazyManyToOneOwned = lazyManyToOneOwned;
		}

		public ContainedEntity getEagerOneToOneOwned() {
			return eagerOneToOneOwned;
		}

		public void setEagerOneToOneOwned(ContainedEntity eagerOneToOneOwned) {
			this.eagerOneToOneOwned = eagerOneToOneOwned;
		}

		public ContainedEntity getEagerManyToOneOwned() {
			return eagerManyToOneOwned;
		}

		public void setEagerManyToOneOwned(ContainedEntity eagerManyToOneOwned) {
			this.eagerManyToOneOwned = eagerManyToOneOwned;
		}

		public ContainedEntity getLazyOneToOneUnowned() {
			return lazyOneToOneUnowned;
		}

		public void setLazyOneToOneUnowned(ContainedEntity lazyOneToOneUnowned) {
			this.lazyOneToOneUnowned = lazyOneToOneUnowned;
		}

		public ContainedEntity getEagerOneToOneUnowned() {
			return eagerOneToOneUnowned;
		}

		public void setEagerOneToOneUnowned(ContainedEntity eagerOneToOneUnowned) {
			this.eagerOneToOneUnowned = eagerOneToOneUnowned;
		}
	}

	@Entity(name = "ContainedEntity")
	static class ContainedEntity {

		@Id
		private Long id;

		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfLazyOneToOneUnowned;

		@OneToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfEagerOneToOneUnowned;

		public ContainedEntity() {
		}

		public ContainedEntity(Long id) {
			this.id = id;
			this.name = "Name #" + id;
		}

		@Override
		public String toString() {
			return "ContainedEntity#" + id;
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

		public RootEntity getInverseSideOfLazyOneToOneUnowned() {
			return inverseSideOfLazyOneToOneUnowned;
		}

		public void setInverseSideOfLazyOneToOneUnowned(RootEntity inverseSideOfLazyOneToOneUnowned) {
			this.inverseSideOfLazyOneToOneUnowned = inverseSideOfLazyOneToOneUnowned;
		}

		public RootEntity getInverseSideOfEagerOneToOneUnowned() {
			return inverseSideOfEagerOneToOneUnowned;
		}

		public void setInverseSideOfEagerOneToOneUnowned(RootEntity inverseSideOfEagerOneToOneUnowned) {
			this.inverseSideOfEagerOneToOneUnowned = inverseSideOfEagerOneToOneUnowned;
		}
	}
}
