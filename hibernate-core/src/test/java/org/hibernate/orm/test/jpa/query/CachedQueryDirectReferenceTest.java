/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.TypedQuery;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-15086")
@Jpa(
		annotatedClasses = CachedQueryDirectReferenceTest.ImmutableEmployee.class,
		generateStatistics = true,
		properties = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, value = "true"),
		},
		settingProviders = @SettingProvider(settingName = AvailableSettings.JAKARTA_SHARED_CACHE_MODE, provider = CachedQueryDirectReferenceTest.SharedCacheModeProvider.class)
)
public class CachedQueryDirectReferenceTest {

	public final static String HQL = "select e from ImmutableEmployee e";

	public static class SharedCacheModeProvider implements SettingProvider.Provider<SharedCacheMode> {
		@Override
		public SharedCacheMode getSetting() {
			return SharedCacheMode.ALL;
		}
	}

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					for ( int i = 0; i < 10; i++ ) {
						em.persist( new ImmutableEmployee( "John" + i, 20d + i ) );
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
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCacheableQuery(EntityManagerFactoryScope scope) {
		Statistics stats = getStatistics( scope );
		stats.clear();

		// First time the query is executed, query and results are cached.
		scope.inTransaction(
				em -> {
					List<ImmutableEmployee> employees = getEmployees( em );

					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					// because the entity is immutable and USE_DIRECT_REFERENCE_CACHE_ENTRIES has been set to true, the
					// second level cache is hit for a cached references, if it does not contain any
					// reference then the query cache values are used to initialize the entity
					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					// because the entity is obtained from  reference stored in the second level cache, no setter methods has been called
					assertThatNoSetterMethodHasBeenCalled( employees );
				}
		);

		stats.clear();

		// Second time the query is executed, list of entities are read from second level cache

		scope.inTransaction(
				em -> {
					List<ImmutableEmployee> employees = getEmployees( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					// because the entity is obtained from  reference stored in the second level cache, no setter methods has been called
					assertThatNoSetterMethodHasBeenCalled( employees );
				}
		);


		// NOTE: JPACache.evictAll() only evicts entity regions;
		//       it does not evict the collection regions or query cache region
		scope.getEntityManagerFactory().getCache().evictAll();

		stats.clear();

		scope.inTransaction(
				em -> {
					List<ImmutableEmployee> employees = getEmployees( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					// the entity is
					assertThatSetterMehotdsHaveBeenCalled( employees );
				}
		);

		stats.clear();

		// this time call clear the entity regions and the query cache region
		scope.inTransaction(
				em -> {
					em.getEntityManagerFactory().getCache().evictAll();
					em.unwrap( SessionImplementor.class )
							.getFactory()
							.getCache()
							.evictQueryRegions();

					List<ImmutableEmployee> employees = getEmployees( em );


					// query is no longer found in the cache, so the query is executed
					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );

					assertThatSetterMehotdsHaveBeenCalled( employees );
				}
		);

	}

	private static void assertThatSetterMehotdsHaveBeenCalled(List<ImmutableEmployee> employees) {
		assertEquals( 1, employees.get( 0 ).callToSetter );
	}

	private static void assertThatNoSetterMethodHasBeenCalled(List<ImmutableEmployee> employees) {
		assertEquals( 0, employees.get( 0 ).callToSetter );
	}

	private static void assertThatAnSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 1, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	private static void assertThatNoSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 0, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	private static List<ImmutableEmployee> getEmployees(EntityManager em) {
		TypedQuery<ImmutableEmployee> query = em.createQuery(
						HQL,
						ImmutableEmployee.class
				)
				.setHint( HINT_CACHEABLE, true );
		List<ImmutableEmployee> employees = query.getResultList();
		assertEquals( 10, employees.size() );
		return employees;
	}

	private static void save(EntityManagerFactoryScope scope, int cardinality) {
		scope.inTransaction(
				em -> {
					for ( int i = 0; i < cardinality; i++ ) {
						em.persist( new ImmutableEmployee( "John" + i, 20d + i ) );
					}
				}
		);
	}

	@Entity(name = "ImmutableEmployee")
	@Immutable
	public static class ImmutableEmployee implements Serializable {

		private Long id;

		private String name;

		private Double salary;

		private int callToSetter;

		public ImmutableEmployee() {
		}

		public ImmutableEmployee(String name, Double salary) {
			this.name = name;
			this.salary = salary;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof org.hibernate.orm.test.jpa.query.Employee ) ) {
				return false;
			}

			ImmutableEmployee employee = (ImmutableEmployee) o;

			if ( id != null ? !id.equals( employee.id ) : employee.id != null ) {
				return false;
			}
			if ( name != null ? !name.equals( employee.name ) : employee.name != null ) {
				return false;
			}
			if ( salary != null ? !salary.equals( employee.salary ) : employee.salary != null ) {
				return false;
			}

			return true;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
			callToSetter++;
		}

		public Double getSalary() {
			return salary;
		}

		public void setSalary(Double salary) {
			this.salary = salary;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			result = 31 * result + ( salary != null ? salary.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "Employee(id = " + id + ", name = " + name + ", salary = " + salary + ")";
		}

	}

}
