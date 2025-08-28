/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JoinFormula;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
public class JoinFormulaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Country.class,
			User.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::associations-JoinFormula-persistence-example[]
		Country US = new Country();
		US.setId(1);
		US.setName("United States");

		Country Romania = new Country();
		Romania.setId(40);
		Romania.setName("Romania");

		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.persist(US);
			entityManager.persist(Romania);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			User user1 = new User();
			user1.setId(1L);
			user1.setFirstName("John");
			user1.setLastName("Doe");
			user1.setPhoneNumber("+1-234-5678");
			entityManager.persist(user1);

			User user2 = new User();
			user2.setId(2L);
			user2.setFirstName("Vlad");
			user2.setLastName("Mihalcea");
			user2.setPhoneNumber("+40-123-4567");
			entityManager.persist(user2);
		});
		//end::associations-JoinFormula-persistence-example[]

		//tag::associations-JoinFormula-fetching-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			log.info("Fetch User entities");

			User john = entityManager.find(User.class, 1L);
			assertEquals(US, john.getCountry());

			User vlad = entityManager.find(User.class, 2L);
			assertEquals(Romania, vlad.getCountry());
		});
		//end::associations-JoinFormula-fetching-example[]
	}

	//tag::associations-JoinFormula-example[]
	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String phoneNumber;

		@ManyToOne
		@JoinFormula("REGEXP_REPLACE(phoneNumber, '\\+(\\d+)-.*', '\\1')::int")
		private Country country;

		//Getters and setters omitted for brevity

	//end::associations-JoinFormula-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		public Country getCountry() {
			return country;
		}

	//tag::associations-JoinFormula-example[]
	}
	//end::associations-JoinFormula-example[]

	//tag::associations-JoinFormula-example[]

	@Entity(name = "Country")
	@Table(name = "countries")
	public static class Country {

		@Id
		private Integer id;

		private String name;

		//Getters and setters, equals and hashCode methods omitted for brevity

	//end::associations-JoinFormula-example[]

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Country)) {
				return false;
			}
			Country country = (Country) o;
			return Objects.equals(getId(), country.getId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getId());
		}
	//tag::associations-JoinFormula-example[]
	}
	//end::associations-JoinFormula-example[]
}
