/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class OneToOneCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Passport.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "htest" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	protected String getCacheConcurrencyStrategy() {
		return AccessType.READ_WRITE.getExternalName();
	}

	@Test
	public void testOneToOneFromSecondLevelCache() {
		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person();
		s.persist( person );
		Passport passport = new Passport();
		person.passport = passport;
		passport.person = person;
		s.persist( passport );
		s.getTransaction().commit();
		s.close();

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		s = openSession();
		s.getTransaction().begin();
		person = s.get( Person.class, person.id );
		s.getTransaction().commit();
		s.close();

		// person and person#passport should have been read from the cache.
		assertEquals( 1, statistics.getSecondLevelCacheStatistics( "htest." + Person.class.getName() ).getHitCount() );
		assertEquals( 1, statistics.getSecondLevelCacheStatistics( "htest." + Passport.class.getName() ).getHitCount() );

		assertEquals( 0, statistics.getEntityStatistics( Person.class.getName() ).getLoadCount() );
		assertEquals( 0, statistics.getEntityStatistics( Passport.class.getName() ).getLoadCount() );
		// no statements should have been prepared because no entities should have been loaded from
		// the database; also person#passport#person should not have been loaded by unique ID if
		// stashed in PersistenceContext by EntityUniqueKey when person was originally assembled.
		assertEquals( 0, statistics.getPrepareStatementCount() );

		s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from " + Person.class.getName() ).executeUpdate();
		s.createQuery( "delete from " + Passport.class.getName() ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testOneToOneWithoutSecondLevelCache() {
		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person();
		s.persist( person );
		Passport passport = new Passport();
		person.passport = passport;
		passport.person = person;
		s.persist( passport );
		s.getTransaction().commit();
		s.close();

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		s = openSession();
		s.getTransaction().begin();
		s.setCacheMode( CacheMode.IGNORE );
		person = s.get( Person.class, person.id );
		s.getTransaction().commit();
		s.close();

		// person and person#passport should NOT have been read from the cache;
		assertEquals( 0, statistics.getSecondLevelCacheStatistics( "htest." + Person.class.getName() ).getHitCount() );
		assertEquals( 0, statistics.getSecondLevelCacheStatistics( "htest." + Passport.class.getName() ).getHitCount() );
		// they should have been loaded from the database.
		assertEquals( 1, statistics.getEntityStatistics( Person.class.getName() ).getLoadCount() );
		assertEquals( 1, statistics.getEntityStatistics( Passport.class.getName() ).getLoadCount() );
		// only 2 statements should have been prepared; one to load person, and one to load person#passport
		// person#passport#person should not have been loaded by unique ID if stashed in PersistenceContext by
		// EntityUniqueKey when person was originally hydrated.
		assertEquals( 2, statistics.getPrepareStatementCount() );

		s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from " + Person.class.getName() ).executeUpdate();
		s.createQuery( "delete from " + Passport.class.getName() ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Entity
	@Table(name = "person")
	@Cacheable
	static class Person {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne
		@Fetch(FetchMode.SELECT)
		private Passport passport;
	}

	@Entity
	@Table(name = "passport")
	@Cacheable
	static class Passport {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne(mappedBy = "passport")
		@Fetch(FetchMode.SELECT)
		private Person person;
	}
}
