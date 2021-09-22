/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-12855")
public class QueryPlanCacheStatisticsTest extends BaseEntityManagerFunctionalTestCase {

	private Statistics statistics;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		SessionFactory sessionFactory = entityManagerFactory().unwrap( SessionFactory.class );
		statistics = sessionFactory.getStatistics();

		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( long i = 1; i <= 5; i++ ) {
				if ( i % 3 == 0 ) {
					entityManager.flush();
				}
				Employee employee = new Employee();
				employee.setName( String.format( "Employee: %d", i ) );
				entityManager.persist( employee );
			}
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-14632" )
	public void testCreateNativeQueryHitCount() {
		statistics.clear();

		doInJPA( this::entityManagerFactory, entityManager -> {

			List<Employee> employees = entityManager.createNativeQuery(
				"select * from employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//First time, we get a cache miss, so the query is compiled
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//The hit count should be 0 as we don't need to go to the cache after we already compiled the query
			assertEquals( 0, statistics.getQueryPlanCacheHitCount() );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			List<Employee> employees = entityManager.createNativeQuery(
				"select * from employee e", Employee.class )
			.getResultList();

			assertEquals( 5, employees.size() );

			//The miss count is still 1, as now we got the query plan from the cache
			assertEquals( 1, statistics.getQueryPlanCacheMissCount() );
			//And the cache hit count increases.
			assertEquals( 1, statistics.getQueryPlanCacheHitCount() );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

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
