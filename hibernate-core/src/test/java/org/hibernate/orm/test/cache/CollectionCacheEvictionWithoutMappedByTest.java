/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 */
public class CollectionCacheEvictionWithoutMappedByTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, People.class};
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
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, true );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, true );
		cfg.setProperty( Environment.USE_QUERY_CACHE, true );
	}

	private People createPeople() {
		Session session = openSession();
		session.beginTransaction();
		People people = new People();
		people.people.add( new Person() );
		people.people.add( new Person() );
		session.persist( people );

		session.getTransaction().commit();
		session.close();
		return people;
	}

	private People initCache(int id) {
		Session session = openSession();
		People people = session.get( People.class, id );
		//should add in cache
		assertEquals( 2, people.people.size() );
		session.close();
		return people;
	}

	@Test
	public void testCollectionCacheEvictionInsert() {
		People people = createPeople();
		people = initCache( people.id );

		Session session = openSession();
		session.beginTransaction();

		people = session.get( People.class, people.id );
		Person person = new Person();
		session.persist( person );
		people.people.add( person );

		session.getTransaction().commit();
		session.close();

		session = openSession();

		people = session.get( People.class, people.id );
		assertEquals( 3, people.people.size() );

		session.close();
	}

	@Test
	public void testCollectionCacheEvictionRemove() {
		People people = createPeople();
		people = initCache( people.id );

		Session session = openSession();
		session.beginTransaction();

		people = session.get( People.class, people.id );
		Person person = people.people.remove( 0 );
		session.remove( person );

		session.getTransaction().commit();
		session.close();

		session = openSession();

		people = session.get( People.class, people.id );
		assertEquals( 1, people.people.size() );

		session.close();
	}

	@Test
	public void testCollectionCacheEvictionUpdate() {
		People people1 = createPeople();
		people1 = initCache( people1.id );
		People people2 = createPeople();
		people2 = initCache( people2.id );


		Session session = openSession();
		session.beginTransaction();

		people1 = session.get( People.class, people1.id );
		people2 = session.get( People.class, people2.id );

		Person person1 = people1.people.remove( 0 );
		Person person2 = people1.people.remove( 0 );
		Person person3 = people2.people.remove( 0 );
		session.flush();//avoid: Unique index or primary key violation
		people1.people.add( person3 );
		people2.people.add( person2 );
		people2.people.add( person1 );

		session.getTransaction().commit();
		session.close();

		session = openSession();

		people1 = session.get( People.class, people1.id );
		people2 = session.get( People.class, people2.id );
		assertEquals( 1, people1.people.size() );
		assertEquals( 3, people2.people.size() );

		session.close();
	}

	@Entity(name = "People")
	@Table(name = "people_group")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class People {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(cascade = CascadeType.ALL)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Person> people = new ArrayList<Person>();
	}

	@Entity(name = "Person")
	@Table(name = "person")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {
		@Id
		@GeneratedValue
		private Integer id;

		protected Person() {
		}
	}
}
