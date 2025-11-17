/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = ElementCollectionUpdateTest.Person.class)
@SessionFactory
@JiraKey(value = "HHH-16297")
public class ElementCollectionUpdateTest {

	private Person thePerson;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Set<String> phones = new HashSet<>( Arrays.asList( "999-999-9999", "111-111-1111", "123-456-7890" ) );
			thePerson = new Person( 7242000, "Claude", phones );
			session.persist( thePerson );
		} );
	}

	@Test
	public void removeElementAndAddNewOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person foundPerson = session.find( Person.class, thePerson.getId() );
			Assertions.assertThat( foundPerson ).isNotNull();
			Set<String> phones = foundPerson.getPhones();
			phones.remove( "111-111-1111" );
			phones.add( "000" );
			assertThat( phones )
					.containsExactlyInAnyOrder( "999-999-9999", "123-456-7890", "000" );
		} );
		scope.inTransaction( session -> {
			Person person = session.find( Person.class, thePerson.getId() );
			assertThat( person.getPhones() )
					.containsExactlyInAnyOrder( "999-999-9999", "123-456-7890", "000" );
		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, thePerson.getId() );
			person.getPhones().remove( "123-456-7890" );
		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, thePerson.getId() );
			Set<String> phones = person.getPhones();
			assertThat( phones )
					.containsExactlyInAnyOrder( "999-999-9999", "000" );
			phones.add( "111-111-1111" );

		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, thePerson.getId() );
			Set<String> phones = person.getPhones();
			assertThat( phones )
					.containsExactlyInAnyOrder( "999-999-9999", "000", "111-111-1111" );

		} );
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	static class Person {
		@Id
		private Integer id;
		private String name;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<String> phones;

		public Person() {
		}

		public Person(Integer id, String name, Collection<String> phones) {
			this.id = id;
			this.name = name;
			this.phones = new HashSet<>( phones );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setPhones(Set<String> phones) {
			this.phones = phones;
		}

		public Set<String> getPhones() {
			return phones;
		}

	}
}
