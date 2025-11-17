/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stat.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = QueryPlanCacheStatisticsTest.Employee.class)
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-12855")
public class QueryPlanCacheStatisticsTest {
	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			for ( long i = 1; i <= 5; i++ ) {
				Employee employee = new Employee();
				employee.setName( String.format( "Employee: %d", i ) );
				entityManager.persist( employee );
			}
		} );

		factoryScope.getSessionFactory().getStatistics().clear();
		factoryScope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( entityManager -> {
			final String FIRST_QUERY = "select e from Employee e";

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( FIRST_QUERY, 0, statistics );

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( FIRST_QUERY, 1, statistics );

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( FIRST_QUERY, 2, statistics );

			final String SECOND_QUERY = "select count(e) from Employee e";

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( SECOND_QUERY, 0, statistics );

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 3, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( SECOND_QUERY, 1, statistics );

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 4, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertQueryStatistics( SECOND_QUERY, 2, statistics );
		} );
	}

	@Test
	@JiraKey("HHH-13077")
	public void testCreateQueryHitCount(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			Query<Employee> query = entityManager.createQuery(
					"select e from Employee e", Employee.class );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			List<Employee> employees = query.getResultList();
			assertEquals( 5, employees.size() );

			//The miss count is 2 because the previous cache miss was for the HqlInterpretation and this is for the plan
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Employee> employees = entityManager.createQuery(
				"select e from Employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 2, as now we got the query plan from the cache
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Employee> employees = entityManager.createQuery(
				"select e from Employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 2, as now we got the query plan from the cache
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 4, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@JiraKey("HHH-14632")
	public void testCreateNativeQueryHitCount(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			List<Employee> employees = entityManager.createNativeQuery(
				"select * from employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {
			List<Employee> employees = entityManager.createNativeQuery(
				"select * from employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {
			List<Employee> employees = entityManager.createNativeQuery(
				"select * from employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@JiraKey("HHH-13077")
	public void testCreateNamedQueryHitCount(SessionFactoryScope scope) {
		// Compile the named queries
		scope.getSessionFactory().getQueryEngine().getNamedObjectRepository().checkNamedQueries( scope.getSessionFactory().getQueryEngine() );

		var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( entityManager -> {
			Query<Employee> query = entityManager.createNamedQuery(
							"find_employee_by_name", Employee.class )
					.setParameter( "name", "Employee: 1" );

			//The miss count is 0 because the plan was compiled when the EMF was built, and we cleared the Statistics
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			//The hit count is 1 since we got the plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );

			Employee employees = query.getSingleResult();

			//The miss count is 1 because the previous cache hit was for the HqlInterpretation and this is for the plan
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {
			Employee employees = entityManager.createNamedQuery(
				"find_employee_by_name", Employee.class )
			.setParameter( "name", "Employee: 1" )
			.getSingleResult();

			//The miss count is still 1 because the plan was compiled when the EMF was built, and we cleared the Statistics
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count is 3 since we got the plan from the cache twice,
			//but the second time we have a hit for the HqlInterpretation and one for the plan
			assertEquals( 3, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@JiraKey("HHH-13077")
	public void testCreateQueryTupleHitCount(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			Query<Tuple> query = entityManager.createQuery(
					"select e.id, e.name from Employee e", Tuple.class );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			List<Tuple> employees = query.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is 2 because the previous cache miss was for the HqlInterpretation and this is for the plan
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {
			List<Tuple> employees = entityManager.createQuery(
				"select e.id, e.name from Employee e", Tuple.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 2, as now we got the query plan from the cache
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {
			List<Tuple> employees = entityManager.createQuery(
				"select e.id, e.name from Employee e", Tuple.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 2, as now we got the query plan from the cache
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 4, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@JiraKey("HHH-13077")
	public void testLockModeHitCount(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			TypedQuery<Employee> typedQuery = entityManager.createQuery( "select e from Employee e", Employee.class );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			List<Employee> employees = typedQuery.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is 2 because the previous cache miss was for the HqlInterpretation and this is for the plan
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			typedQuery.setLockMode( LockModeType.READ );

			//The hit count should still be 0 as setLockMode() shouldn't trigger a cache hit
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			Assertions.assertNotNull( typedQuery.getLockMode() );

			//The hit count should still be 0 as getLockMode() shouldn't trigger a cache hit
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@JiraKey("HHH-16782")
	public void testCriteriaQuery(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			HibernateCriteriaBuilder cb = entityManager.getCriteriaBuilder();
			JpaCriteriaQuery<Employee> cq = cb.createQuery( Employee.class );
			cq.from( Employee.class );
			entityManager.setProperty( AvailableSettings.CRITERIA_COPY_TREE, true );
			TypedQuery<Employee> typedQuery = entityManager.createQuery( cq );

			// Criteria query does not need parsing, so no miss or hit at this point
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 0, statistics.getQueryExecutionCount() );

			List<Employee> employees = typedQuery.getResultList();
			assertEquals( 5, employees.size() );

			// The miss count is 0 because the query plan is not even considered for query plan caching
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryExecutionCount() );

			typedQuery.getResultList();

			// The miss count is 0 because the query plan is not even considered for query plan caching
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryExecutionCount() );
		} );
	}

	@Test
	@JiraKey("HHH-16782")
	public void testCriteriaQueryCache(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			HibernateCriteriaBuilder cb = entityManager.getCriteriaBuilder();
			JpaCriteriaQuery<Employee> cq = cb.createQuery( Employee.class );
			cq.from( Employee.class );
			TypedQuery<Employee> typedQuery = entityManager.createQuery( cq );
			typedQuery.setHint( AvailableHints.HINT_QUERY_PLAN_CACHEABLE, true );

			// Criteria query does not need parsing, so no miss or hit at this point
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 0, statistics.getQueryExecutionCount() );

			List<Employee> employees = typedQuery.getResultList();
			assertEquals( 5, employees.size() );

			// The miss count is 1 because the query plan is resolved once
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryExecutionCount() );

			typedQuery.getResultList();

			// The hit count should increase on second access though
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryExecutionCount() );
		} );
	}

	@Test
	@JiraKey("HHH-16782")
	public void testCriteriaQueryNoCopyTree(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			HibernateCriteriaBuilder cb = entityManager.getCriteriaBuilder();
			JpaCriteriaQuery<Employee> cq = cb.createQuery( Employee.class );
			cq.from( Employee.class );
			entityManager.setProperty( AvailableSettings.CRITERIA_COPY_TREE, false );
			TypedQuery<Employee> typedQuery = entityManager.createQuery( cq );

			// Criteria query does not need parsing, so no miss or hit at this point
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 0, statistics.getQueryExecutionCount() );

			List<Employee> employees = typedQuery.getResultList();
			assertEquals( 5, employees.size() );

			// The miss count is 1 because the query plan is resolved once
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryExecutionCount() );

			typedQuery.getResultList();

			// The hit count should increase on second access though
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryExecutionCount() );
		} );
	}

	@Test
	@JiraKey("HHH-16782")
	public void testDisableQueryPlanCache(SessionFactoryScope scope) {
		var statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			TypedQuery<Employee> typedQuery = entityManager.createQuery( "select e from Employee e", Employee.class );
			typedQuery.setHint( AvailableHints.HINT_QUERY_PLAN_CACHEABLE, false );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			List<Employee> employees = typedQuery.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1 and hit count still 0 because plan is not even considered for caching
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryExecutionCount() );

			typedQuery.getResultList();

			//The miss count is still 1 and hit count still 0 because plan is not even considered for caching
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryExecutionCount() );
		} );
	}

	private void assertQueryStatistics(String hql, int hitCount, StatisticsImplementor statistics) {
		QueryStatistics queryStatistics = statistics.getQueryStatistics( hql );

		assertEquals( hitCount, queryStatistics.getPlanCacheHitCount() );
		assertEquals( 1, queryStatistics.getPlanCacheMissCount() );

		assertTrue( queryStatistics.getPlanCompilationTotalMicroseconds() > 0 );
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	@NamedQuery(
		name = "find_employee_by_name",
		query = "select e from Employee e where e.name = :name"
	)
	public static class Employee {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

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
	}

}
