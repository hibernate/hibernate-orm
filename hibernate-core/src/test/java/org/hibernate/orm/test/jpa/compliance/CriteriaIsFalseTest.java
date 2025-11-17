/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				CriteriaIsFalseTest.Person.class,
				CriteriaIsFalseTest.Address.class
		}
)
public class CriteriaIsFalseTest {

	@Test
	public void testIsFalse(EntityManagerFactoryScope scope) {

		Address validAddress = new Address( 1, "Lollard street London", true );
		Address invalidAddress = new Address( 2, "Oxfort street London", false );
		Person personWithValidAddress = new Person( 1, "Luigi", validAddress );
		Person personWithInvalidAddredd = new Person( 2, "Andrea", invalidAddress );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( validAddress );
					entityManager.persist( invalidAddress );
					entityManager.persist( personWithValidAddress );
					entityManager.persist( personWithInvalidAddredd );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> personRoot = query.from( Person.class );
					query.select( personRoot.get( "id" ) );
					query.where( criteriaBuilder.isFalse( personRoot.get( "address" ).get( "valid" ) ) );

					final List<Integer> ids = entityManager.createQuery( query ).getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( personWithInvalidAddredd.getId(), ids.get( 0 ) );
				}
		);

	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private Address address;

		public Person() {
		}

		public Person(Integer id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Address getAddress() {
			return address;
		}
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {

		@Id
		private Integer id;

		private String street;

		private boolean valid;

		public Address() {
		}

		public Address(Integer id, String street, boolean valid) {
			this.id = id;
			this.street = street;
			this.valid = valid;
		}
	}
}
