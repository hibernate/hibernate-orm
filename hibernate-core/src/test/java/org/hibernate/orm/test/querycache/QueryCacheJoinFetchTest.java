/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12430")
@Jpa(
		annotatedClasses = {
				QueryCacheJoinFetchTest.Person.class,
				QueryCacheJoinFetchTest.Phone.class
		},
		generateStatistics = true,
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public class QueryCacheJoinFetchTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			Phone phone1 = new Phone( "123-456-7890" );
			Phone phone2 = new Phone( "321-654-0987" );

			person.addPhone( phone1 );
			person.addPhone( phone2 );
			entityManager.persist( person );
		} );

		scope.getEntityManagerFactory().getCache().evictAll();
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics().clear();

		Person person = scope.fromEntityManager( entityManager -> {
			return entityManager.createQuery(
							"select distinct p " +
									"from Person p " +
									"join fetch p.phones ph", Person.class )
					.setHint( HibernateHints.HINT_CACHEABLE, Boolean.TRUE )
					.getSingleResult();
		} );

		assertEquals( 2, person.getPhones().size() );
		assertEquals(
				0,
				scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics().getQueryCacheHitCount()
		);

		person = scope.fromEntityManager( entityManager -> {
			scope.getEntityManagerFactory().getCache().evictAll();

			return entityManager.createQuery(
							"select distinct p " +
									"from Person p " +
									"join fetch p.phones ph", Person.class )
					.setHint( HibernateHints.HINT_CACHEABLE, Boolean.TRUE )
					.getSingleResult();
		} );

		assertEquals(
				1,
				scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics().getQueryCacheHitCount()
		);

		assertEquals( 2, person.getPhones().size() );
	}

	@Entity(name = "Person")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}

		public void removePhone(Phone phone) {
			phones.remove( phone );
			phone.setPerson( null );
		}
	}

	@Entity(name = "Phone")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		@Column(name = "`number`", unique = true)
		private String number;

		@ManyToOne
		private Person person;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals( number, phone.number );
		}

		@Override
		public int hashCode() {
			return Objects.hash( number );
		}
	}
}
