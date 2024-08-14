package org.hibernate.orm.test.entitygraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.assertj.ManagedAssert;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.assertj.core.api.AbstractListAssert;

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
@SessionFactory
@JiraKey("HHH-18489")
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@EnhancementOptions(lazyLoading = true)
public class LoadAndFetchGraphAssociationNotExplicitlySpecifiedTest {

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 0; i < 3; ++i ) {
				RootEntity root = new RootEntity( i * 100 );

				long j = i * 100;
				root.setLazyOneToOneOwned( new ContainedEntity( ++j ) );
				root.setLazyManyToOneOwned( new ContainedEntity( ++j ) );
				root.setLazyOneToManyOwned( List.of( new ContainedEntity( ++j ) ) );
				root.setLazyManyToManyOwned( List.of( new ContainedEntity( ++j ) ) );
				root.setEagerOneToOneOwned( new ContainedEntity( ++j ) );
				root.setEagerManyToOneOwned( new ContainedEntity( ++j ) );
				root.setEagerOneToManyOwned( List.of( new ContainedEntity( ++j ) ) );
				root.setEagerManyToManyOwned( List.of( new ContainedEntity( ++j ) ) );

				session.persist( root );

				ContainedEntity contained;

				contained = new ContainedEntity( ++j );
				root.setLazyOneToOneUnowned( contained );
				contained.setInverseSideOfLazyOneToOneUnowned( root );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setLazyOneToManyUnowned( List.of( contained ) );
				contained.setInverseSideOfLazyOneToManyUnowned( root );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setLazyOneToManyUnowned( List.of( contained ) );
				contained.setInverseSideOfLazyManyToManyUnowned( List.of( root ) );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setEagerOneToOneUnowned( contained );
				contained.setInverseSideOfEagerOneToOneUnowned( root );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setEagerOneToManyUnowned( List.of( contained ) );
				contained.setInverseSideOfEagerOneToManyUnowned( root );
				session.persist( contained );

				contained = new ContainedEntity( ++j );
				root.setEagerOneToManyUnowned( List.of( contained ) );
				contained.setInverseSideOfEagerManyToManyUnowned( List.of( root ) );
				session.persist( contained );
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "update ContainedEntity set"
					+ " inverseSideOfLazyOneToOneUnowned = null"
					+ ", inverseSideOfLazyOneToManyUnowned = null"
					+ ", inverseSideOfEagerOneToOneUnowned = null"
					+ ", inverseSideOfEagerOneToManyUnowned = null" ).executeUpdate();
			session.createNativeQuery( "delete from RootEntity_lazyManyToManyUnowned" ).executeUpdate();
			session.createNativeQuery( "delete from RootEntity_eagerManyToManyUnowned" ).executeUpdate();
			session.createMutationQuery( "delete RootEntity" ).executeUpdate();
			session.createMutationQuery( "delete ContainedEntity" ).executeUpdate();
		} );
	}

	// Arguments for the parameterized test below
	List<Arguments> queryWithFetchGraph() {
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

	@ParameterizedTest
	@MethodSource
	void queryWithFetchGraph(GraphSemantic graphSemantic, String propertySpecifiedInGraph, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
			assertSoftly( softly -> { // "softly" is used to report all failures instead of just the first one
				var resultListAssert = softly.assertThat( resultList );

				for ( String propertyName : RootEntity.LAZY_PROPERTY_NAMES ) {
					boolean expectInitialized = propertyName.equals( propertySpecifiedInGraph );
					assertAssociationInitialized( resultListAssert, propertyName, expectInitialized );
				}
				for ( String propertyName : RootEntity.EAGER_PROPERTY_NAMES ) {
					boolean expectInitialized = propertyName.equals( propertySpecifiedInGraph )
							// Under LOAD semantics, or when not using graphs,
							// eager properties also get loaded (even if not specified in the graph).
							|| GraphSemantic.LOAD.equals( graphSemantic ) || graphSemantic == null;
					assertAssociationInitialized( resultListAssert, propertyName, expectInitialized );
				}
			} );
		} );
	}

	private void assertAssociationInitialized(AbstractListAssert<?, ?, ?, ?> resultListAssert,
			String propertyName, boolean expectInitialized) {
		resultListAssert.allSatisfy( loaded -> assertThat( loaded ).extracting( propertyName, ManagedAssert.factory() )
				.as( "Managed object held in attribute '" + propertyName + "' of '" + loaded + "'" )
				.isInitialized( expectInitialized ) );
	}

	@Entity(name = "RootEntity")
	static class RootEntity {

		public static final Set<String> LAZY_PROPERTY_NAMES = Set.of(
				"lazyOneToOneOwned", "lazyManyToOneOwned", "lazyOneToManyOwned", "lazyManyToManyOwned",
				"lazyOneToOneUnowned", "lazyOneToManyUnowned", "lazyManyToManyUnowned"
		);
		public static final Set<String> EAGER_PROPERTY_NAMES = Set.of(
				"eagerOneToOneOwned", "eagerManyToOneOwned", "eagerOneToManyOwned", "eagerManyToManyOwned",
				"eagerOneToOneUnowned", "eagerOneToManyUnowned", "eagerManyToManyUnowned"
		);

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private ContainedEntity lazyOneToOneOwned;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private ContainedEntity lazyManyToOneOwned;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinTable(name = "RootEntity_lazyOneToManyOwned")
		private List<ContainedEntity> lazyOneToManyOwned;

		@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinTable(name = "RootEntity_lazyManyToManyOwned")
		private List<ContainedEntity> lazyManyToManyOwned;

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		private ContainedEntity eagerOneToOneOwned;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		private ContainedEntity eagerManyToOneOwned;

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		@JoinTable(name = "RootEntity_eagerOneToManyOwned")
		private List<ContainedEntity> eagerOneToManyOwned;

		@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		@JoinTable(name = "RootEntity_eagerManyToManyOwned")
		private List<ContainedEntity> eagerManyToManyOwned;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "inverseSideOfLazyOneToOneUnowned")
		private ContainedEntity lazyOneToOneUnowned;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "inverseSideOfLazyOneToManyUnowned")
		private List<ContainedEntity> lazyOneToManyUnowned;

		@ManyToMany(fetch = FetchType.LAZY, mappedBy = "inverseSideOfLazyManyToManyUnowned")
		private List<ContainedEntity> lazyManyToManyUnowned;

		@OneToOne(fetch = FetchType.EAGER, mappedBy = "inverseSideOfEagerOneToOneUnowned")
		private ContainedEntity eagerOneToOneUnowned;

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "inverseSideOfEagerOneToManyUnowned")
		private List<ContainedEntity> eagerOneToManyUnowned;

		@ManyToMany(fetch = FetchType.EAGER, mappedBy = "inverseSideOfEagerManyToManyUnowned")
		private List<ContainedEntity> eagerManyToManyUnowned;

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

		public List<ContainedEntity> getLazyOneToManyOwned() {
			return lazyOneToManyOwned;
		}

		public void setLazyOneToManyOwned(List<ContainedEntity> lazyOneToManyOwned) {
			this.lazyOneToManyOwned = lazyOneToManyOwned;
		}

		public List<ContainedEntity> getLazyManyToManyOwned() {
			return lazyManyToManyOwned;
		}

		public void setLazyManyToManyOwned(List<ContainedEntity> lazyManyToManyOwned) {
			this.lazyManyToManyOwned = lazyManyToManyOwned;
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

		public List<ContainedEntity> getEagerOneToManyOwned() {
			return eagerOneToManyOwned;
		}

		public void setEagerOneToManyOwned(List<ContainedEntity> eagerOneToManyOwned) {
			this.eagerOneToManyOwned = eagerOneToManyOwned;
		}

		public List<ContainedEntity> getEagerManyToManyOwned() {
			return eagerManyToManyOwned;
		}

		public void setEagerManyToManyOwned(List<ContainedEntity> eagerManyToManyOwned) {
			this.eagerManyToManyOwned = eagerManyToManyOwned;
		}

		public ContainedEntity getLazyOneToOneUnowned() {
			return lazyOneToOneUnowned;
		}

		public void setLazyOneToOneUnowned(ContainedEntity lazyOneToOneUnowned) {
			this.lazyOneToOneUnowned = lazyOneToOneUnowned;
		}

		public List<ContainedEntity> getLazyOneToManyUnowned() {
			return lazyOneToManyUnowned;
		}

		public void setLazyOneToManyUnowned(List<ContainedEntity> lazyOneToManyUnowned) {
			this.lazyOneToManyUnowned = lazyOneToManyUnowned;
		}

		public List<ContainedEntity> getLazyManyToManyUnowned() {
			return lazyManyToManyUnowned;
		}

		public void setLazyManyToManyUnowned(List<ContainedEntity> lazyManyToManyUnowned) {
			this.lazyManyToManyUnowned = lazyManyToManyUnowned;
		}

		public ContainedEntity getEagerOneToOneUnowned() {
			return eagerOneToOneUnowned;
		}

		public void setEagerOneToOneUnowned(ContainedEntity eagerOneToOneUnowned) {
			this.eagerOneToOneUnowned = eagerOneToOneUnowned;
		}

		public List<ContainedEntity> getEagerOneToManyUnowned() {
			return eagerOneToManyUnowned;
		}

		public void setEagerOneToManyUnowned(List<ContainedEntity> eagerOneToManyUnowned) {
			this.eagerOneToManyUnowned = eagerOneToManyUnowned;
		}

		public List<ContainedEntity> getEagerManyToManyUnowned() {
			return eagerManyToManyUnowned;
		}

		public void setEagerManyToManyUnowned(List<ContainedEntity> eagerManyToManyUnowned) {
			this.eagerManyToManyUnowned = eagerManyToManyUnowned;
		}
	}

	@Entity(name = "ContainedEntity")
	static class ContainedEntity {

		@Id
		private Long id;

		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfLazyOneToOneUnowned;

		@ManyToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfLazyOneToManyUnowned;

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "RootEntity_lazyManyToManyUnowned",
				// Default column name is too long for some DBs, and in fact we don't care about it
				joinColumns = @JoinColumn(name = "invLazManyToManyUnowned_id"))
		private List<RootEntity> inverseSideOfLazyManyToManyUnowned;

		@OneToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfEagerOneToOneUnowned;

		@ManyToOne(fetch = FetchType.LAZY)
		private RootEntity inverseSideOfEagerOneToManyUnowned;

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "RootEntity_eagerManyToManyUnowned",
				// Default column name is too long for some DBs, and in fact we don't care about it
				joinColumns = @JoinColumn(name = "invEagManyToManyUnowned_id"))
		private List<RootEntity> inverseSideOfEagerManyToManyUnowned;

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

		public RootEntity getInverseSideOfLazyOneToManyUnowned() {
			return inverseSideOfLazyOneToManyUnowned;
		}

		public void setInverseSideOfLazyOneToManyUnowned(RootEntity inverseSideOfLazyOneToManyUnowned) {
			this.inverseSideOfLazyOneToManyUnowned = inverseSideOfLazyOneToManyUnowned;
		}

		public List<RootEntity> getInverseSideOfLazyManyToManyUnowned() {
			return inverseSideOfLazyManyToManyUnowned;
		}

		public void setInverseSideOfLazyManyToManyUnowned(List<RootEntity> inverseSideOfLazyManyToManyUnowned) {
			this.inverseSideOfLazyManyToManyUnowned = inverseSideOfLazyManyToManyUnowned;
		}

		public RootEntity getInverseSideOfEagerOneToOneUnowned() {
			return inverseSideOfEagerOneToOneUnowned;
		}

		public void setInverseSideOfEagerOneToOneUnowned(RootEntity inverseSideOfEagerOneToOneUnowned) {
			this.inverseSideOfEagerOneToOneUnowned = inverseSideOfEagerOneToOneUnowned;
		}

		public RootEntity getInverseSideOfEagerOneToManyUnowned() {
			return inverseSideOfEagerOneToManyUnowned;
		}

		public void setInverseSideOfEagerOneToManyUnowned(RootEntity inverseSideOfEagerOneToManyUnowned) {
			this.inverseSideOfEagerOneToManyUnowned = inverseSideOfEagerOneToManyUnowned;
		}

		public List<RootEntity> getInverseSideOfEagerManyToManyUnowned() {
			return inverseSideOfEagerManyToManyUnowned;
		}

		public void setInverseSideOfEagerManyToManyUnowned(List<RootEntity> inverseSideOfEagerManyToManyUnowned) {
			this.inverseSideOfEagerManyToManyUnowned = inverseSideOfEagerManyToManyUnowned;
		}
	}
}
