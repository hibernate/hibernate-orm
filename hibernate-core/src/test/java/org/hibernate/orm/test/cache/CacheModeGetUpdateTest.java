/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		CacheModeGetUpdateTest.Phone.class,
		CacheModeGetUpdateTest.Person.class
} )
public class CacheModeGetUpdateTest {
	private static final long PHONE_ID = 1L;
	private static final long PERSON_ID = 2L;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Phone( PHONE_ID, "123" ) );
			session.persist( new Person( PERSON_ID, "Marco" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Phone" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.GET );
			final Phone phone = session.find( Phone.class, PHONE_ID );
			final Person person = session.find( Person.class, PERSON_ID );
			phone.setPerson( person );
			person.getPhones().add( phone );
			session.persist( phone );
		} );
		// in a different transaction
		scope.inTransaction( session -> {
			final Phone phone = session.find( Phone.class, PHONE_ID );
			assertThat( phone.getPerson() ).isNotNull();
		} );
	}

	@Entity( name = "Phone" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	public static class Phone {
		@Id
		private Long id;

		@Column( name = "phone_number" )
		private String number;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn
		private Person person;

		public Phone() {
		}

		public Phone(final long id, final String number) {
			setId( id );
			setNumber( number );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		@Override
		public String toString() {
			return "Phone{" +
					"id=" + id +
					", number='" + number + '\'' +
					", person=" + person +
					'}';
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
			return Objects.equals( id, phone.id ) && Objects.equals( number, phone.number ) && Objects.equals(
					person,
					phone.person
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, number, person );
		}
	}

	@Entity( name = "Person" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany( fetch = FetchType.LAZY, mappedBy = "person" )
		private final Set<Phone> phones = new HashSet<>();

		public Person() {
		}

		public Person(final long id, final String name) {
			setId( id );
			setName( name );
		}

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "Person{" +
					"id=" + id +
					", name='" + name +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals( id, person.id ) && Objects.equals( name, person.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}

		public void setName(final String name) {
			this.name = name;
		}

		public Set<Phone> getPhones() {
			return phones;
		}
	}
}
