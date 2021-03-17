/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.pc;

import java.util.List;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLoadIdTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, "jcache" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString() );
		sqlStatementInterceptor = new SQLStatementInterceptor( settings );
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			Person person1 = new Person();
			person1.setId( 1L );
			person1.setName("John Doe Sr.");

			entityManager.persist( person1 );

			Person person2 = new Person();
			person2.setId( 2L );
			person2.setName("John Doe");

			entityManager.persist( person2 );

			Person person3 = new Person();
			person3.setId( 3L );
			person3.setName("John Doe Jr.");

			entityManager.persist( person3 );
		} );
	}

	@Test
	public void testSessionCheck() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
	}

	@Test
	public void testSecondLevelCacheCheck() {
		//tag::pc-by-multiple-ids-second-level-cache-example[]
		SessionFactory sessionFactory = entityManagerFactory().unwrap( SessionFactory.class );
		Statistics statistics = sessionFactory.getStatistics();

		sessionFactory.getCache().evictAll();
		statistics.clear();
		sqlStatementInterceptor.clear();

		assertEquals( 0, statistics.getQueryExecutionCount() );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );

			List<Person> persons = session
				.byMultipleIds( Person.class )
				.multiLoad( 1L, 2L, 3L );

			assertEquals( 3, persons.size() );
		} );

		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 3, statistics.getSecondLevelCachePutCount() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			sqlStatementInterceptor.clear();

			List<Person> persons = session.byMultipleIds( Person.class )
				.with( CacheMode.NORMAL )
				.multiLoad( 1L, 2L, 3L );

			assertEquals( 3, persons.size() );

		} );

		assertEquals( 3, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0, sqlStatementInterceptor.getSqlQueries().size() );
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
