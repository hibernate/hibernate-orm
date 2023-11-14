/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-15086")
@Jpa(
		annotatedClasses = {
				CachedQueryShallowMultiselectTest.Manager.class,
				CachedQueryShallowMultiselectTest.Employee.class
		},
		generateStatistics = true,
		properties = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "auto")
		}
)
public class CachedQueryShallowMultiselectTest {

	public final static String HQL = "select e1,e2 from Employee e1 join Employee e2 on e1=e2";

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
		assertEquals( 10, stats.getSecondLevelCachePutCount() );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.createQuery( "delete from Employee" ).executeUpdate();
					em.createQuery( "delete from Manager" ).executeUpdate();
				}
		);
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

					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);

	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	private static List<Employee> getEmployees(EntityManager em) {
		TypedQuery<Tuple> query = em.createQuery(
						HQL,
						Tuple.class
				)
				.setHint( HINT_CACHEABLE, true );
		List<Tuple> tuples = query.getResultList();
		assertEquals( 10, tuples.size() );
		List<Employee> employees = new ArrayList<>( 10 );
		for ( Tuple tuple : tuples ) {
			Employee e1 = tuple.get( 0, Employee.class );
			Employee e2 = tuple.get( 1, Employee.class );
			assertSame( e1, e2 );
			assertEquals( "John" + e1.getId(), e1.getName() );
			employees.add( e1 );
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
		@ManyToOne(fetch = FetchType.LAZY)
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
