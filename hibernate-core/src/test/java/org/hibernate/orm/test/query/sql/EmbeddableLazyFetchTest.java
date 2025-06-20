/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import java.util.Collections;

import org.hibernate.graph.spi.RootGraphImplementor;

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
		annotatedClasses = EmbeddableLazyFetchTest.Person.class
)
@SessionFactory
@JiraKey(value = "HHH-15778")
public class EmbeddableLazyFetchTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address( "Milan", "Italy", "Italy", "20133" );
					Person person = new Person( 1, address );

					session.persist( person );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();;
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RootGraphImplementor<Person> graph = session.createEntityGraph( Person.class );

					Person person = session.find(
							Person.class, 1,
							Collections.singletonMap( "jakarta.persistence.fetchgraph", graph )
					);

					assertThat( person ).isNotNull();
					assertThat( person.address.city ).isEqualTo( "Milan" );
					assertThat( person.address.state ).isEqualTo( "Italy" );
					assertThat( person.address.country ).isEqualTo( "Italy" );
					assertThat( person.address.postcode ).isEqualTo( "20133" );
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
