package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selecting, joining or comparing the root entity of a derived table (subquery in the from clause)
 * or of a CTE used to fail with a {@code ClassCastException} in
 * {@code AnonymousTupleEntityValuedModelPart}, because the model part it wraps is then the entity
 * persister itself, not a to-one association.
 */
@DomainModel(annotatedClasses = {
		EntityValuedSelectionInDerivedTableAndCteTest.Movement.class,
		EntityValuedSelectionInDerivedTableAndCteTest.JoinedBase.class,
		EntityValuedSelectionInDerivedTableAndCteTest.JoinedMovement.class,
		EntityValuedSelectionInDerivedTableAndCteTest.EmbeddedIdMovement.class,
		EntityValuedSelectionInDerivedTableAndCteTest.Port.class
})
@SessionFactory
@JiraKey("HHH-20646")
@JiraKey("HHH-20225")
public class EntityValuedSelectionInDerivedTableAndCteTest {

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 5; id++ ) {
				final String tower = id <= 3 ? "A" : "B";
				session.persist( new Movement( id, "m" + id, tower ) );
				session.persist( new JoinedMovement( id, "m" + id, tower ) );
				session.persist( new EmbeddedIdMovement( new MovementKey( id, "R" ), "m" + id, tower ) );
			}
			session.persist( new Port( 1L, "A" ) );
			session.persist( new Port( 2L, "B" ) );
		} );
	}

	@AfterAll
	public void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	// HQL

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20646")
	public void hqlSelectEntityFromDerivedRoot(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"select d.e from (select m as e from " + entityName + " m where m.id > 3) d order by d.e.id",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m4", "m5" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20646")
	public void hqlSelectAttributeOfEntityFromDerivedRoot(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<String> result = session.createQuery(
					"select d.e.name from (select m as e from " + entityName + " m where m.id > 3) d order by 1",
					String.class
			).getResultList();
			assertThat( result ).containsExactly( "m4", "m5" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20646")
	public void hqlJoinEntityOnDerivedJoin(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"select m from " + entityName + " m"
							+ " join (select p as e from " + entityName + " p order by p.id offset 1 rows fetch first 2 rows only) d"
							+ " on m = d.e order by m.id",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20646")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void hqlSelectEntityFromLateralDerivedJoin(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// The query from the issue description
			final List<Named> result = session.createQuery(
					"select c.task from Port p"
							+ " join lateral (select t as task from " + entityName + " t where t.tower = p.tower order by t.id limit 2) c"
							+ " on true order by p.id, c.task.id",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m1", "m2", "m4", "m5" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20225")
	public void hqlSelectEntityFromCte(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// The queries from the issue description
			final List<Named> entities = session.createQuery(
					"with cte as (select e as theEntity from " + entityName + " e where e.id < 3)"
							+ " select theEntity from cte order by theEntity.id",
					Named.class
			).getResultList();
			assertThat( entities ).extracting( Named::getName ).containsExactly( "m1", "m2" );

			final List<String> names = session.createQuery(
					"with cte as (select e as theEntity from " + entityName + " e where e.id < 3)"
							+ " select theEntity.name from cte order by 1",
					String.class
			).getResultList();
			assertThat( names ).containsExactly( "m1", "m2" );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"Movement", "JoinedMovement"})
	@JiraKey("HHH-20225")
	public void hqlJoinEntityOnCte(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"with cte as (select e as theEntity from " + entityName + " e where e.id < 3)"
							+ " select m from " + entityName + " m join cte c on c.theEntity = m order by m.id",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m1", "m2" );
		} );
	}

	// Criteria: the paging query shape that cuts the page in a derived join, then joins the entity back to it

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20646")
	public void criteriaJoinEntityOnDerivedJoin(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaSubQuery<Named> page = pageSubquery( cb, query, entityClass );
			final JpaRoot<? extends Named> root = query.from( entityClass );
			final JpaDerivedJoin<Named> pageJoin = root.join( page );
			pageJoin.on( cb.equal( root, pageJoin.get( "pageMovement" ) ) );
			query.select( root ).orderBy( cb.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20646")
	public void criteriaJoinEntityOnDerivedJoinReversedOperands(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaSubQuery<Named> page = pageSubquery( cb, query, entityClass );
			final JpaRoot<? extends Named> root = query.from( entityClass );
			final JpaDerivedJoin<Named> pageJoin = root.join( page );
			pageJoin.on( cb.equal( pageJoin.get( "pageMovement" ), root ) );
			query.select( root ).orderBy( cb.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20646")
	public void criteriaJoinIdOnDerivedJoinEntityId(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaSubQuery<Named> page = pageSubquery( cb, query, entityClass );
			final JpaRoot<? extends Named> root = query.from( entityClass );
			final JpaDerivedJoin<Named> pageJoin = root.join( page );
			pageJoin.on( cb.equal( root.get( "id" ), pageJoin.get( "pageMovement" ).get( "id" ) ) );
			query.select( root ).orderBy( cb.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20646")
	public void criteriaSelectEntityFromDerivedRoot(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaSubQuery<Named> page = pageSubquery( cb, query, entityClass );
			final JpaDerivedRoot<Named> derivedRoot = query.from( page );
			query.select( derivedRoot.get( "pageMovement" ) )
					.orderBy( cb.asc( derivedRoot.get( "pageMovement" ).get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20646")
	public void criteriaInSubqueryOverDerivedRoot(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaRoot<? extends Named> root = query.from( entityClass );
			final JpaSubQuery<Named> in = query.subquery( Named.class );
			final JpaSubQuery<Named> page = pageSubquery( cb, in, entityClass );
			final JpaDerivedRoot<Named> derivedRoot = in.from( page );
			in.select( derivedRoot.get( "pageMovement" ) );
			query.select( root ).where( root.in( in ) ).orderBy( cb.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20225")
	public void criteriaJoinEntityOnCte(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaCteCriteria<Named> cte = query.with( pageQuery( cb, entityClass ) );
			final JpaRoot<? extends Named> root = query.from( entityClass );
			final JpaJoin<?, Named> cteJoin = root.join( cte );
			cteJoin.on( cb.equal( root, cteJoin.get( "pageMovement" ) ) );
			query.select( root ).orderBy( cb.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = {Movement.class, JoinedMovement.class})
	@JiraKey("HHH-20225")
	public void criteriaSelectEntityFromCteRoot(Class<? extends Named> entityClass, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Named> query = cb.createQuery( Named.class );
			final JpaCteCriteria<Named> cte = query.with( pageQuery( cb, entityClass ) );
			final JpaRoot<Named> cteRoot = query.from( cte );
			query.select( cteRoot.get( "pageMovement" ) )
					.orderBy( cb.asc( cteRoot.get( "pageMovement" ).get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@Test
	@JiraKey("HHH-20646")
	public void criteriaJoinOtherEntityOnAttributeOfDerivedJoinEntity(SessionFactoryScope scope) {
		// The derived table selects an entity which is not the one it is joined to
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Port> query = cb.createQuery( Port.class );
			final JpaSubQuery<Movement> page = pageSubquery( cb, query, Movement.class );
			final JpaRoot<Port> root = query.from( Port.class );
			final JpaDerivedJoin<Movement> pageJoin = root.join( page );
			pageJoin.on( cb.equal( root.get( "tower" ), pageJoin.get( "pageMovement" ).get( "tower" ) ) );
			query.select( root ).distinct( true );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Port::getTower ).containsExactly( "A" );
		} );
	}

	// Entity with a composite identifier

	@Test
	@JiraKey("HHH-20646")
	public void hqlJoinEmbeddedIdEntityOnDerivedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"select m from EmbeddedIdMovement m"
							+ " join (select p as e from EmbeddedIdMovement p order by p.id.num offset 1 rows fetch first 2 rows only) d"
							+ " on m = d.e order by m.id.num",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	@Test
	@JiraKey("HHH-20646")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void hqlSelectEmbeddedIdEntityFromLateralDerivedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"select c.task from Port p"
							+ " join lateral (select t as task from EmbeddedIdMovement t where t.tower = p.tower order by t.id.num limit 2) c"
							+ " on true order by p.id, c.task.id.num",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m1", "m2", "m4", "m5" );
		} );
	}

	@Test
	@JiraKey("HHH-20225")
	public void hqlSelectEmbeddedIdEntityFromCte(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Named> result = session.createQuery(
					"with cte as (select e as theEntity from EmbeddedIdMovement e where e.id.num < 3)"
							+ " select theEntity from cte order by theEntity.id.num",
					Named.class
			).getResultList();
			assertThat( result ).extracting( Named::getName ).containsExactly( "m1", "m2" );
		} );
	}

	@Test
	@JiraKey("HHH-20646")
	public void criteriaJoinEmbeddedIdEntityOnDerivedJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<EmbeddedIdMovement> query = cb.createQuery( EmbeddedIdMovement.class );
			final JpaSubQuery<EmbeddedIdMovement> page = query.subquery( EmbeddedIdMovement.class );
			final Root<EmbeddedIdMovement> pageRoot = page.from( EmbeddedIdMovement.class );
			pageRoot.alias( "pageMovement" );
			page.select( pageRoot )
					.orderBy( cb.asc( pageRoot.get( "id" ).get( "num" ) ) )
					.offset( 1 )
					.fetch( 2 );
			final JpaRoot<EmbeddedIdMovement> root = query.from( EmbeddedIdMovement.class );
			final JpaDerivedJoin<EmbeddedIdMovement> pageJoin = root.join( page );
			pageJoin.on( cb.equal( root, pageJoin.get( "pageMovement" ) ) );
			query.select( root ).orderBy( cb.asc( root.get( "id" ).get( "num" ) ) );
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( Named::getName ).containsExactly( "m2", "m3" );
		} );
	}

	/**
	 * A subquery selecting the root entity, aliased {@code pageMovement}, restricted to the
	 * second and third rows by id.
	 */
	@SuppressWarnings("unchecked")
	private static <T> JpaSubQuery<T> pageSubquery(
			HibernateCriteriaBuilder cb,
			jakarta.persistence.criteria.AbstractQuery<?> parent,
			Class<? extends T> entityClass) {
		final JpaSubQuery<T> page = (JpaSubQuery<T>) parent.subquery( entityClass );
		final Root<? extends T> pageRoot = page.from( entityClass );
		pageRoot.alias( "pageMovement" );
		page.select( (Root<T>) pageRoot )
				.orderBy( cb.asc( pageRoot.get( "id" ) ) )
				.offset( 1 )
				.fetch( 2 );
		return page;
	}

	@SuppressWarnings("unchecked")
	private static <T> JpaCriteriaQuery<T> pageQuery(HibernateCriteriaBuilder cb, Class<? extends T> entityClass) {
		final JpaCriteriaQuery<T> page = (JpaCriteriaQuery<T>) cb.createQuery( entityClass );
		final JpaRoot<? extends T> pageRoot = page.from( entityClass );
		pageRoot.alias( "pageMovement" );
		page.select( (Root<T>) pageRoot )
				.orderBy( cb.asc( pageRoot.get( "id" ) ) )
				.offset( 1 )
				.fetch( 2 );
		return page;
	}

	public interface Named {
		String getName();
	}

	@Entity(name = "Movement")
	public static class Movement implements Named {
		@Id
		private Long id;
		private String name;
		private String tower;

		public Movement() {
		}

		public Movement(Long id, String name, String tower) {
			this.id = id;
			this.name = name;
			this.tower = tower;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	@Entity(name = "JoinedBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class JoinedBase implements Named {
		@Id
		private Long id;
		private String name;

		public JoinedBase() {
		}

		public JoinedBase(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	@Entity(name = "JoinedMovement")
	public static class JoinedMovement extends JoinedBase {
		private String tower;

		public JoinedMovement() {
		}

		public JoinedMovement(Long id, String name, String tower) {
			super( id, name );
			this.tower = tower;
		}
	}

	@Embeddable
	public record MovementKey(Long num, String region) {
	}

	@Entity(name = "EmbeddedIdMovement")
	public static class EmbeddedIdMovement implements Named {
		@EmbeddedId
		private MovementKey id;
		private String name;
		private String tower;

		public EmbeddedIdMovement() {
		}

		public EmbeddedIdMovement(MovementKey id, String name, String tower) {
			this.id = id;
			this.name = name;
			this.tower = tower;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	@Entity(name = "Port")
	public static class Port {
		@Id
		private Long id;
		private String tower;

		public Port() {
		}

		public Port(Long id, String tower) {
			this.id = id;
			this.tower = tower;
		}

		public String getTower() {
			return tower;
		}
	}
}
