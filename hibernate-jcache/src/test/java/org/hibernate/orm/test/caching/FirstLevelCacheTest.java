/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


/**
 * @author Vlad Mihalcea
 */

@Jpa(
		annotatedClasses = FirstLevelCacheTest.Person.class,
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "jcache")
		}
)
public class FirstLevelCacheTest {

	@Test
	public void testCache(EntityManagerFactoryScope scope) {
		final Person aPerson = new Person();
		scope.inTransaction(entityManager -> {
			entityManager.persist(new Person());
			entityManager.persist(new Person());
			Person person = new Person();
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			List<Object> dtos = new ArrayList<>();
			//tag::caching-management-jpa-detach-example[]
			for(Person person : entityManager.createQuery("select p from Person p", Person.class)
					.getResultList()) {
				dtos.add(toDTO(person));
				entityManager.detach(person);
			}
			//end::caching-management-jpa-detach-example[]
			//tag::caching-management-clear-example[]
			entityManager.clear();

			//end::caching-management-clear-example[]

			Person person = aPerson;

			//tag::caching-management-contains-example[]
			entityManager.contains(person);

			//end::caching-management-contains-example[]
		});
		scope.inTransaction( entityManager -> {
			List<Object> dtos = new ArrayList<>();
			//tag::caching-management-native-evict-example[]
			Session session = entityManager.unwrap(Session.class);
			for(Person person : (List<Person>) session.createSelectionQuery("select p from Person p").list()) {
				dtos.add(toDTO(person));
				session.evict(person);
			}
			//end::caching-management-native-evict-example[]
			//tag::caching-management-clear-example[]
			session.clear();
			//end::caching-management-clear-example[]

			Person person = aPerson;

			//tag::caching-management-contains-example[]
			session.contains(person);
			//end::caching-management-contains-example[]
		});
	}

	private Object toDTO(Person person) {
		return person;
	}


	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
