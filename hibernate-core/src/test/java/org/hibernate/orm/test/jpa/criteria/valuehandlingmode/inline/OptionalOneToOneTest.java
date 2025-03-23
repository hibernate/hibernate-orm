/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Jpa(
		annotatedClasses = { OptionalOneToOneTest.Person.class, OptionalOneToOneTest.PersonAddress.class }
		,properties = @Setting( name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class OptionalOneToOneTest {

	@Test
	public void testBidirQueryEntityProperty(EntityManagerFactoryScope scope) {

		PersonAddress personAddress = scope.fromTransaction(
				session -> {
					PersonAddress address = new PersonAddress();
					Person person = new Person();
					address.setPerson( person );
					person.setPersonAddress( address );

					session.persist( person );
					session.persist( address );
					return address;
				}
		);

		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			criteria.where( criteriaBuilder.equal( root.get( "personAddress" ), personAddress ) );

			session.createQuery( criteria ).getSingleResult();
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id @GeneratedValue(generator = "fk")
		@GenericGenerator(strategy = "foreign", name = "fk", parameters = @Parameter(name="property", value="personAddress"))
		private Integer id;

		@PrimaryKeyJoinColumn
		@OneToOne(optional=true)
		private PersonAddress personAddress;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public PersonAddress getPersonAddress() {
			return personAddress;
		}

		public void setPersonAddress(PersonAddress personAddress) {
			this.personAddress = personAddress;
		}
	}


	@Entity(name = "PersonAddress")
	public static class PersonAddress {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne(mappedBy="personAddress")
		private Person person;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}
}
