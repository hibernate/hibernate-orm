/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = { MultiLoadIdTest.Person.class },
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
				@Setting( name = AvailableSettings.STATEMENT_INSPECTOR, value = "org.hibernate.testing.jdbc.SQLStatementInspector")
		}
)
public class MultiLoadIdTest {

	@BeforeAll
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					Person person1 = new Person();
					person1.setId( 1L );
					person1.setName( "John Doe Sr." );
					entityManager.persist( person1 );

					Person person2 = new Person();
					person2.setId( 2L );
					person2.setName( "John Doe" );
					entityManager.persist( person2 );

					Person person3 = new Person();
					person3.setId( 3L );
					person3.setName( "John Doe Jr." );
					entityManager.persist( person3 );
				}
		);
	}

	@Test
	public void testSessionCheck(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					//tag::pc-by-multiple-ids-example[]
					Session session = entityManager.unwrap( Session.class );

					List<Person> persons = session
							.byMultipleIds( Person.class )
							.multiLoad( 1L, 2L, 3L );

					assertEquals( 3, persons.size() );

					List<Person> samePersons = session
							.byMultipleIds( Person.class )
							.enableSessionCheck( true )
							.multiLoad( 1L, 2L, 3L );

					assertEquals( persons, samePersons );
					//end::pc-by-multiple-ids-example[]
				}
		);
	}

	@Test
	public void testSecondLevelCacheCheck(EntityManagerFactoryScope scope) {
		//tag::pc-by-multiple-ids-second-level-cache-example[]
		SessionFactory sessionFactory = scope.getEntityManagerFactory().unwrap(SessionFactory.class);
		Statistics statistics = sessionFactory.getStatistics();

		sessionFactory.getCache().evictAll();
		statistics.clear();
		final SQLStatementInspector sqlStatementInspector = scope.getStatementInspector( SQLStatementInspector.class );
		sqlStatementInspector.clear();

		assertEquals(0, statistics.getQueryExecutionCount());

		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap(Session.class);

					List<Person> persons = session
							.byMultipleIds(Person.class)
							.multiLoad(1L, 2L, 3L);

					assertEquals(3, persons.size());
				}
		);

		assertEquals(0, statistics.getSecondLevelCacheHitCount());
		assertEquals(3, statistics.getSecondLevelCachePutCount());
		assertEquals(1, sqlStatementInspector.getSqlQueries().size());

		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap(Session.class);
					sqlStatementInspector.clear();

					List<Person> persons = session.byMultipleIds(Person.class)
						.with(CacheMode.NORMAL)
						.multiLoad(1L, 2L, 3L);

					assertEquals(3, persons.size());
				}
		);

		assertEquals(3, statistics.getSecondLevelCacheHitCount());
		assertEquals(0, sqlStatementInspector.getSqlQueries().size());
		//end::pc-by-multiple-ids-second-level-cache-example[]
	}

	@Entity(name = "Person")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {

		@Id
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
