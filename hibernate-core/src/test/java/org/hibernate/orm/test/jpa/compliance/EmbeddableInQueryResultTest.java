/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@Jpa(
		annotatedClasses = {
				EmbeddableInQueryResultTest.Person.class,
		}
)
@JiraKey( value = "HHH-15223")
public class EmbeddableInQueryResultTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Country italy = new Country( "Italy", "ITA" );
					Person person = new Person( 1, "Ines", italy );

					entityManager.persist( person );
				}
		);
	}

	@Test
	public void testSelectEmbeddableIsNotInTheManagedState(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Object[]> results = entityManager.createQuery( "SELECT p, p.country FROM Person p " )
							.getResultList();
					assertThat( results.size() ).isEqualTo( 1 );

					Object[] result = results.get( 0 );
					Person person = (Person) result[0];
					Country country = (Country) result[1];
					assertNotSame( country, person.getCountry() );
					country.setCode( "ITA_1" );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Person person = entityManager.find( Person.class, 1 );
					assertThat( person.getCountry().getCode() ).isEqualTo( "ITA" );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		@Embedded
		private Country country;

		public Person() {
		}

		public Person(Integer id, String name, Country country) {
			this.id = id;
			this.name = name;
			this.country = country;
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

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	}

	@Embeddable
	public static class Country {

		private String country;

		private String code;

		public Country() {
		}

		public Country(String country, String code) {
			this.country = country;
			this.code = code;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}
