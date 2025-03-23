/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.util.ArrayList;
import java.util.List;
import javax.cache.configuration.MutableConfiguration;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				NonStrictReadWriteCacheTest.Person.class,
				NonStrictReadWriteCacheTest.Phone.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "jcache"),
		}
)
public class NonStrictReadWriteCacheTest {
	private final Logger log = Logger.getLogger( NonStrictReadWriteCacheTest.class );

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		JCacheHelper.locateStandardCacheManager().createCache(
				"hibernate.test.org.hibernate.userguide.caching.NonStrictReadWriteCacheTest$Person",
				new MutableConfiguration<>()
		);
		JCacheHelper.locateStandardCacheManager().createCache(
				"hibernate.test.org.hibernate.userguide.caching.NonStrictReadWriteCacheTest$Phone",
				new MutableConfiguration<>()
		);
		JCacheHelper.locateStandardCacheManager().createCache(
				"hibernate.test.org.hibernate.userguide.caching.NonStrictReadWriteCacheTest$Person.phones",
				new MutableConfiguration<>()
		);
	}

	@Test
	public void testCache(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			entityManager.persist(person);
			Phone home = new Phone("123-456-7890");
			Phone office = new Phone("098-765-4321");
			person.addPhone(home);
			person.addPhone(office);
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.getPhones().size();
		});
		scope.inTransaction( entityManager -> {
			log.info("Log collection from cache");
			//tag::caching-collection-example[]
			Person person = entityManager.find(Person.class, 1L);
			person.getPhones().size();
			//end::caching-collection-example[]
		});
		scope.inTransaction( entityManager -> {
			log.info("Load from cache");
			entityManager.find(Person.class, 1L).getPhones().size();
		});
	}


	@Entity(name = "Person")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		//tag::caching-collection-mapping-example[]
		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
		private List<Phone> phones = new ArrayList<>();
		//end::caching-collection-mapping-example[]

		@Version
		private int version;

		public Person() {}

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

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add(phone);
			phone.setPerson(this);
		}
	}

	//tag::caching-entity-mapping-example[]
	@Entity(name = "Phone")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		private String mobile;

		@ManyToOne
		private Person person;

		@Version
		private int version;

		//Getters and setters are omitted for brevity

	//end::caching-entity-mapping-example[]

		public Phone() {}

		public Phone(String mobile) {
			this.mobile = mobile;
		}

		public Long getId() {
			return id;
		}

		public String getMobile() {
			return mobile;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	//tag::caching-entity-mapping-example[]
	}
	//end::caching-entity-mapping-example[]
}
