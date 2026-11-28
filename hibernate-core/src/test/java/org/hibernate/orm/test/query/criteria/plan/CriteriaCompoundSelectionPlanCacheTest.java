package org.hibernate.orm.test.query.criteria.plan;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.ParameterExpression;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a Criteria query with a compound selection ({@code multiselect},
 * {@code tuple}, or {@code array}) reuses its cached query plan across executions.
 * A simple non-compound selection is included as a control.
 */
@DomainModel(annotatedClasses = CriteriaCompoundSelectionPlanCacheTest.Person.class)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"))
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-21210")
public class CriteriaCompoundSelectionPlanCacheTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long i = 1; i <= 3; i++ ) {
				final Person person = new Person();
				person.id = i;
				person.name = "Person " + i;
				person.age = 20 + (int) i * 10;
				session.persist( person );
			}
		} );
		scope.getSessionFactory().getStatistics().clear();
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMultiselectPlanCacheHit(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( session -> {
			assertEquals( 3, runMultiselectQuery( session, 25 ).size() );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertEquals( 2, runMultiselectQuery( session, 35 ).size() );

			// The second, structurally identical query must reuse the cached plan.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
		} );
	}

	@Test
	void testArrayPlanCacheHit(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( session -> {
			assertEquals( 3, runArrayQuery( session, 25 ).size() );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertEquals( 2, runArrayQuery( session, 35 ).size() );

			// The second, structurally identical query must reuse the cached plan.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
		} );
	}

	@Test
	void testTuplePlanCacheHit(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( session -> {
			assertEquals( 3, runTupleQuery( session, 25 ).size() );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertEquals( 2, runTupleQuery( session, 35 ).size() );

			// The second, structurally identical query must reuse the cached plan.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
		} );
	}

	@Test
	void testSimpleSelectionPlanCacheHit(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( session -> {
			assertEquals( 3, runSimpleSelectionQuery( session, 25 ).size() );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertEquals( 2, runSimpleSelectionQuery( session, 35 ).size() );

			// Control: a non-compound selection already reuses the cached plan.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
		} );
	}

	private static List<Tuple> runMultiselectQuery(SessionImplementor session, int minAge) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
		final JpaRoot<Person> person = query.from( Person.class );
		final ParameterExpression<Integer> minAgeParameter = cb.parameter( Integer.class );
		query.multiselect( person.get( "name" ), person.<Integer>get( "age" ) )
				.where( cb.gt( person.<Integer>get( "age" ), minAgeParameter ) );
		return session.createQuery( query )
				.setParameter( minAgeParameter, minAge )
				.getResultList();
	}

	private static List<Object[]> runArrayQuery(SessionImplementor session, int minAge) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Object[]> query = cb.createQuery( Object[].class );
		final JpaRoot<Person> person = query.from( Person.class );
		final ParameterExpression<Integer> minAgeParameter = cb.parameter( Integer.class );
		query.select( cb.array( person.get( "name" ), person.<Integer>get( "age" ) ) )
				.where( cb.gt( person.<Integer>get( "age" ), minAgeParameter ) );
		return session.createQuery( query )
				.setParameter( minAgeParameter, minAge )
				.getResultList();
	}

	private static List<Tuple> runTupleQuery(SessionImplementor session, int minAge) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
		final JpaRoot<Person> person = query.from( Person.class );
		final ParameterExpression<Integer> minAgeParameter = cb.parameter( Integer.class );
		query.select( cb.tuple( person.get( "name" ), person.<Integer>get( "age" ) ) )
				.where( cb.gt( person.<Integer>get( "age" ), minAgeParameter ) );
		return session.createQuery( query )
				.setParameter( minAgeParameter, minAge )
				.getResultList();
	}

	private static List<Person> runSimpleSelectionQuery(SessionImplementor session, int minAge) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Person> query = cb.createQuery( Person.class );
		final JpaRoot<Person> person = query.from( Person.class );
		final ParameterExpression<Integer> minAgeParameter = cb.parameter( Integer.class );
		query.select( person )
				.where( cb.gt( person.<Integer>get( "age" ), minAgeParameter ) );
		return session.createQuery( query )
				.setParameter( minAgeParameter, minAge )
				.getResultList();
	}

	@Entity(name = "CriteriaCompoundSelectionPlanCachePerson")
	@Table(name = "criteria_compound_selection_plan_cache_person")
	public static class Person {
		@Id
		Long id;

		String name;

		int age;
	}
}
