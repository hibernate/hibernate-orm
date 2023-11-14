/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.CollectionStatistics;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-15086")
@Jpa(
		annotatedClasses = {
				CachedQueryShallowCollectionNestedJoinFetchTest.Manager.class,
				CachedQueryShallowCollectionNestedJoinFetchTest.Employee.class,
				CachedQueryShallowCollectionNestedJoinFetchTest.Car.class
		},
		generateStatistics = true,
		properties = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "auto")
		}
)
public class CachedQueryShallowCollectionNestedJoinFetchTest {

	public final static String HQL = "select e from Manager e left join fetch e.associates a left join fetch a.car";

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
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.createQuery( "delete from Employee" ).executeUpdate();
					em.createQuery( "delete from Car" ).executeUpdate();
					em.createQuery( "delete from Manager" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCacheableQuery(EntityManagerFactoryScope scope) {

		Statistics stats = getStatistics( scope );
		CacheImplementor cache = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getCache();
		stats.clear();

		// First time the query is executed, query and results are cached.
		scope.inTransaction(
				em -> {
					List<Manager> managers = getManagers( em );

					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );
				}
		);

		stats.clear();

		// Second time the query is executed, list of entities are read from query cache

		scope.inTransaction(
				em -> {
					List<Manager> managers = getManagers( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					final CollectionStatistics collectionStats = stats.getCollectionStatistics( Manager.class.getName() + ".associates" );
					assertEquals( 10, collectionStats.getCacheHitCount() );
					assertEquals( 0, collectionStats.getCacheMissCount() );
					assertEquals( 0, collectionStats.getCachePutCount() );
				}
		);

		// NOTE: JPACache.evictAll() only evicts entity regions;
		//       it does not evict the collection regions or query cache region
		cache.evictCollectionData();
		stats.clear();

		scope.inTransaction(
				em -> {
					List<Manager> managers = getManagers( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );

					final CollectionStatistics collectionStats = stats.getCollectionStatistics( Manager.class.getName() + ".associates" );
					assertEquals( 0, collectionStats.getCacheHitCount() );
					assertEquals( 10, collectionStats.getCacheMissCount() );
					assertEquals( 10, collectionStats.getCachePutCount() );
				}
		);


		stats.clear();

		// this time call clear the entity regions and the query cache region
		scope.inTransaction(
				em -> {
					cache.evictCollectionData();
					em.unwrap( SessionImplementor.class )
							.getFactory()
							.getCache()
							.evictQueryRegions();

					List<Manager> managers = getManagers( em );

					// query is no longer found in the cache
					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );

					final CollectionStatistics collectionStats = stats.getCollectionStatistics( Manager.class.getName() + ".associates" );
					assertEquals( 0, collectionStats.getCacheHitCount() );
					assertEquals( 0, collectionStats.getCacheMissCount() );
					assertEquals( 10, collectionStats.getCachePutCount() );
				}
		);

		stats.clear();

		// Check that the join fetched to-one association is initialized even if managers are already part of the PC

		scope.inTransaction(
				em -> {
					em.createQuery( "select m from Manager m" ).getResultList();
					List<Manager> managers = getManagers( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					final CollectionStatistics collectionStats = stats.getCollectionStatistics( Manager.class.getName() + ".associates" );
					assertEquals( 10, collectionStats.getCacheHitCount() );
					assertEquals( 0, collectionStats.getCacheMissCount() );
					assertEquals( 0, collectionStats.getCachePutCount() );
				}
		);

		stats.clear();

		// Check that the join fetched to-one association is initialized even if managers are already part of the PC

		scope.inTransaction(
				em -> {
					em.createQuery( "select m from Manager m join fetch m.associates" ).getResultList();
					List<Manager> managers = getManagers( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );

					final CollectionStatistics collectionStats = stats.getCollectionStatistics( Manager.class.getName() + ".associates" );
					assertEquals( 0, collectionStats.getCacheHitCount() );
					assertEquals( 0, collectionStats.getCacheMissCount() );
					assertEquals( 0, collectionStats.getCachePutCount() );
				}
		);
	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	private static List<Manager> getManagers(EntityManager em) {
		TypedQuery<Manager> query = em.createQuery(
						HQL,
						Manager.class
				)
				.setHint( HINT_CACHEABLE, true );
		List<Manager> managers = query.getResultList();
		assertEquals( 10, managers.size() );
		for ( Manager manager : managers ) {
			assertTrue( Hibernate.isInitialized( manager ) );
			assertEquals( "Manager" + manager.getId(), manager.getName() );
			assertTrue( Hibernate.isInitialized( manager.getAssociates() ) );
			assertEquals( 1, manager.getAssociates().size() );
			for ( Employee associate : manager.getAssociates() ) {
				assertEquals( "John" + associate.getId(), associate.getName() );
				assertTrue( Hibernate.isInitialized( associate.getCar() ) );
				assertEquals( "John" + associate.getId() + "'s car", associate.getCar().getName() );
			}
		}
		return managers;
	}

	private static void assertThatAnSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 1, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	private static void assertThatNoSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 0, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	@Entity(name = "Manager")
	public static class Manager {
		@Id
		Integer id;
		String name;
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		Integer id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Manager manager;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		Car car;

		public Employee() {
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
			this.car = new Car( id, name + "'s car" );
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

		public Car getCar() {
			return car;
		}
	}

	@Entity(name = "Car")
	public static class Car {
		@Id
		Integer id;
		String name;

		public Car() {
		}

		public Car(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

}
