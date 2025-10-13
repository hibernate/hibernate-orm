/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Janario Oliveira
 */
@DomainModel(
		annotatedClasses = {
				CollectionCacheEvictionWithoutMappedByTest.Person.class,
				CollectionCacheEvictionWithoutMappedByTest.People.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"),
		}
)
@SessionFactory
public class CollectionCacheEvictionWithoutMappedByTest {

	@BeforeEach
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@AfterEach
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	private People createPeople(SessionFactoryScope scope) {
		return scope.fromTransaction( session -> {
			People people = new People();
			people.people.add( new Person() );
			people.people.add( new Person() );
			session.persist( people );
			return people;
		} );
	}

	private People initCache(int id, SessionFactoryScope scope) {
		return scope.fromSession( session -> {
			People people = session.get( People.class, id );
			//should add in cache
			assertEquals( 2, people.people.size() );
			return people;
		} );
	}

	@Test
	public void testCollectionCacheEvictionInsert(SessionFactoryScope scope) {
		People people = createPeople( scope );
		Integer id = people.id;
		people = initCache( id, scope );

		scope.inTransaction( session -> {
			People p = session.get( People.class, id );
			Person person = new Person();
			session.persist( person );
			p.people.add( person );

		} );

		scope.inSession( session -> {
			People p = session.get( People.class, id );
			assertEquals( 3, p.people.size() );
		} );
	}

	@Test
	public void testCollectionCacheEvictionRemove(SessionFactoryScope scope) {
		People people = createPeople( scope );
		Integer id = people.id;
		people = initCache( id, scope );

		scope.inTransaction( session -> {
			People p = session.get( People.class, id );
			Person person = p.people.remove( 0 );
			session.remove( person );
		} );

		scope.inSession( session -> {
			var p = session.get( People.class, id );
			assertEquals( 1, p.people.size() );
		} );
	}

	@Test
	public void testCollectionCacheEvictionUpdate(SessionFactoryScope scope) {
		People people1 = createPeople( scope );
		Integer p1id = people1.id;
		people1 = initCache( p1id, scope );
		People people2 = createPeople( scope );
		Integer p2id = people2.id;
		people2 = initCache( p2id, scope );


		scope.inTransaction( session -> {
			var p1 = session.get( People.class, p1id );
			var p2 = session.get( People.class, p2id );

			Person person1 = p1.people.remove( 0 );
			Person person2 = p1.people.remove( 0 );
			Person person3 = p2.people.remove( 0 );
			session.flush();//avoid: Unique index or primary key violation
			p1.people.add( person3 );
			p2.people.add( person2 );
			p2.people.add( person1 );

		} );

		scope.inSession( s -> {
			var p1 = s.get( People.class, p1id );
			var p2 = s.get( People.class, p2id );
			assertEquals( 1, p1.people.size() );
			assertEquals( 3, p2.people.size() );
		} );
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
