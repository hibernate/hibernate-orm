/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Petr Abrahamczik
 */
@TestForIssue(jiraKey = "HHH-14514")
public class TransactionalConcurrencyCollectionCacheEvictionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Person.class, Phone.class };
	}

	@Before
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@After
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		Person person = new Person();
		person.id = 1;
		s.save( person );

		Phone phone = new Phone();
		phone.id = 1;
		phone.person = person;
		s.save( phone );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "delete from " + Phone.class.getName() ).executeUpdate();
		s.createQuery( "delete from " + Person.class.getName() ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCollectionCacheEvictionInsert() {
		Session s = openSession();
		s.beginTransaction();

		Person person = (Person) s.get( Person.class, 1 );

		assertEquals( 1, person.phones.size() );

		Phone phone = new Phone();
		phone.id = 2;
		phone.person = person;
		s.save( phone );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		person = (Person) s.get( Person.class, 1 );
		// fails if cache is not evicted
		assertEquals( 2, person.phones.size() );

		s.close();
	}

	@Test
	public void testCollectionCacheEvictionRemove() {
		Session s = openSession();
		s.beginTransaction();

		Person person = (Person) s.get( Person.class, 1 );

		assertEquals( 1, person.phones.size() );

		s.delete( person.phones.get( 0 ) );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		person = (Person) s.get( Person.class, 1 );
		// fails if cache is not evicted
		try {
			assertEquals( 0, person.phones.size() );
		}
		catch (ObjectNotFoundException e) {
			fail( "Cached element not found" );
		}
		s.close();
	}

	@Entity
	@Table(name = "person")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Person {

		@Id
		private int id;

		@Version
		private int version;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
		@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
		List<Phone> phones = new ArrayList<Phone>();
	}

	@Entity
	@Table(name = "phone")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Phone {

		@Id
		private int id;

		@Version
		private int version;

		@ManyToOne(fetch = FetchType.LAZY)
		Person person;
	}
}
