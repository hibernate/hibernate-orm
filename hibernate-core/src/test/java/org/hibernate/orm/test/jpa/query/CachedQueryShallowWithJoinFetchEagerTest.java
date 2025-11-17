/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-15086")
@Jpa(
		annotatedClasses = {
				CachedQueryShallowWithJoinFetchEagerTest.Manager.class,
				CachedQueryShallowWithJoinFetchEagerTest.Employee.class
		},
		generateStatistics = true,
		properties = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "shallow")
		}
)
public class CachedQueryShallowWithJoinFetchEagerTest {

	public final static String HQL = "select e from Employee e join fetch e.manager";

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					for ( int i = 0; i < 10; i++ ) {
						Manager manager = new Manager( i, "Manager" + i );
						for ( int j = 0; j < 1; j++ ) {
							manager.addAssociate( new Employee( i * 10 + j, "John" + ( i * 10 + j ) ) );
						}
						em.persist( manager );
					}
				}
		);
		Statistics stats = getStatistics( scope );

		assertEquals( 0, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );
		assertEquals( 0, stats.getSecondLevelCacheMissCount() );
		assertEquals( 20, stats.getSecondLevelCachePutCount() );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCacheableQuery(EntityManagerFactoryScope scope) {

		Statistics stats = getStatistics( scope );
		stats.clear();

		// First time the query is executed, query and results are cached.
		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);

		stats.clear();

		// Second time the query is executed, list of entities are read from query cache

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 20, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);

		// Evict all managers and assert that no query is executed
		scope.getEntityManagerFactory().getCache().evict( Manager.class );

		stats.clear();

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );
				}
		);

		// this time call clear the Employees
		scope.getEntityManagerFactory().getCache().evict( Employee.class );

		stats.clear();

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );
				}
		);

		// this time call clear the Employees and Managers
		scope.getEntityManagerFactory().getCache().evict( Manager.class );
		scope.getEntityManagerFactory().getCache().evict( Employee.class );

		stats.clear();

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					// 10 cache misses for Employees, but 20 cache puts because Managers are put back in due to eager loading
					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 20, stats.getSecondLevelCachePutCount() );
				}
		);

	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	private static List<Employee> getEmployees(EntityManager em) {
		TypedQuery<Employee> query = em.createQuery(
						HQL,
						Employee.class
				)
				.setHint( HINT_CACHEABLE, true );
		List<Employee> employees = query.getResultList();
		assertEquals( 10, employees.size() );
		for ( Employee employee : employees ) {
			assertTrue( Hibernate.isInitialized( employee ) );
			assertNotNull( employee.getManager() );
			assertTrue( Hibernate.isInitialized( employee.getManager() ) );
			assertEquals( "John" + employee.getId(), employee.getName() );
		}
		return employees;
	}

	private static void assertThatAnSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 1, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	private static void assertThatNoSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 0, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	@Entity(name = "Employee")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Employee {
		@Id
		Integer id;
		String name;
		@ManyToOne(fetch = FetchType.EAGER)
		Manager manager;

		public Employee() {
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Manager getManager() {
			return manager;
		}
	}

	@Entity(name = "Manager")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Manager {
		@Id
		Integer id;
		String name;
		@OneToMany(mappedBy = "manager", cascade = CascadeType.PERSIST)
		Set<Employee> associates = new HashSet<>();

		public Manager() {
		}

		public Manager(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Employee> getAssociates() {
			return associates;
		}

		public void addAssociate(Employee employee) {
			employee.manager = this;
			associates.add( employee );
		}
	}

}
