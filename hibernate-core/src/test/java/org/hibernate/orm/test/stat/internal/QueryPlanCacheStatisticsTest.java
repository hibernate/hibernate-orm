/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.stat.internal;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import org.hibernate.cfg.Environment;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(annotatedClasses = {
		QueryPlanCacheStatisticsTest.Employee.class
})
@ServiceRegistry(settings = {
		@Setting( name = Environment.GENERATE_STATISTICS, value = "true")
})
@SessionFactory
@TestForIssue(jiraKey = "HHH-12855")
public class QueryPlanCacheStatisticsTest {

	private Statistics statistics;

	@BeforeAll
	protected void afterEntityManagerFactoryBuilt(SessionFactoryScope scope) {
		statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction( entityManager -> {
			for ( long i = 1; i <= 5; i++ ) {
				Employee employee = new Employee();
				employee.setName( String.format( "Employee: %d", i ) );
				entityManager.persist( employee );
			}
		} );
	}

	@BeforeEach
	protected void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
		statistics.clear();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		assertEquals( 0, statistics.getQueryPlanCacheMissCount() );

		scope.inTransaction( entityManager -> {
			final String FIRST_QUERY = "select e from Employee e";

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( FIRST_QUERY, 0 );

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( FIRST_QUERY, 1 );

			entityManager.createQuery( FIRST_QUERY );

			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( FIRST_QUERY, 2 );

			final String SECOND_QUERY = "select count(e) from Employee e";

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( SECOND_QUERY, 0 );

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 3, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( SECOND_QUERY, 1 );

			entityManager.createQuery( SECOND_QUERY );

			assertEquals( 4, statistics.getQueryPlanCacheHitCount() );
			assertEquals( 2, statistics.getQueryPlanCacheMissCount() );

			assertQueryStatistics( SECOND_QUERY, 2 );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13077" )
	public void testCreateQueryHitCount(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			List<Employee> employees = entityManager.createQuery(
				"select e from Employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Employee> employees = entityManager.createQuery(
				"select e from Employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Employee> employees = entityManager.createQuery(
				"select e from Employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13077" )
	public void testCreateNamedQueryHitCount(SessionFactoryScope scope) {
		// Compile the named queries
		scope.getSessionFactory().getQueryEngine().getNamedObjectRepository().checkNamedQueries( scope.getSessionFactory().getQueryEngine() );
		statistics.clear();
		scope.inTransaction( entityManager -> {

			Employee employees = entityManager.createNamedQuery(
				"find_employee_by_name", Employee.class )
			.setParameter( "name", "Employee: 1" )
			.getSingleResult();

			//The miss count is 0 because the plan was compiled when the EMF was built, and we cleared the Statistics
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			//The hit count is 1 since we got the plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			Employee employees = entityManager.createNamedQuery(
				"find_employee_by_name", Employee.class )
			.setParameter( "name", "Employee: 1" )
			.getSingleResult();

			//The miss count is still 0 because the plan was compiled when the EMF was built, and we cleared the Statistics
			assertEquals( 0, statistics.getQueryPlanCacheMissCount() );
			//The hit count is 2 since we got the plan from the cache twice
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13077" )
	public void testCreateQueryTupleHitCount(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			List<Tuple> employees = entityManager.createQuery(
				"select e.id, e.name from Employee e", Tuple.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Tuple> employees = entityManager.createQuery(
				"select e.id, e.name from Employee e", Tuple.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		scope.inTransaction( entityManager -> {

			List<Tuple> employees = entityManager.createQuery(
				"select e.id, e.name from Employee e", Tuple.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 2, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13077")
	public void testLockModeHitCount(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			TypedQuery<Employee> typedQuery = entityManager.createQuery( "select e from Employee e", Employee.class );

			List<Employee> employees = typedQuery.getResultList();

			assertEquals( 5, employees.size() );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			typedQuery.setLockMode( LockModeType.READ );

			//The hit count should still be 0 as setLockMode() shouldn't trigger a cache hit
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );

			assertNotNull( typedQuery.getLockMode() );

			//The hit count should still be 0 as getLockMode() shouldn't trigger a cache hit
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );
	}

	private void assertQueryStatistics(String hql, int hitCount) {
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
