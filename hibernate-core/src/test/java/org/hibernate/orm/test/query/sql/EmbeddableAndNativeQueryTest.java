/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = EmbeddableAndNativeQueryTest.Person.class
)
@SessionFactory
@JiraKey(value = "HHH-15658")
public class EmbeddableAndNativeQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address( "Milan", "Italy", "Italy", "20133" );
					Person person = new Person( 1, address );

					Address address2 = new Address( "Garbagnate", "Italy", "Italy", "20024" );
					Person person2 = new Person( 2, address2 );

					session.persist( person );
					session.persist( person2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> people = session.createNativeQuery(
							"select {p.*} from Person p",
							Person.class,
							"p"
					).list();

					assertThat( people.size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private Address address;

		public Person() {
		}

		public Person(Integer id, Address address) {
			this.id = id;
			this.address = address;
		}
	}

	@Embeddable
	public static class Address {
		private String city;
		private String state;
		private String country;

		private String postcode;

		public Address() {
		}

		public Address(String city, String state, String country, String postcode) {
			this.city = city;
			this.state = state;
			this.country = country;
			this.postcode = postcode;
		}
	}
}
