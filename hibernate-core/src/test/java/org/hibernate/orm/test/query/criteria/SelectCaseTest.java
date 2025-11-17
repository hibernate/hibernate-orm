/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(
		annotatedClasses = {
				SelectCaseTest.Address.class,
				SelectCaseTest.Person.class
		}
)
@JiraKey(value = "HHH-15482")
public class SelectCaseTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Address address = new Address( "RM", "Via Fori Imperiali", "Roma" );
					Person person = new Person( 1, "Roberto", 70, address );
					entityManager.persist( address );
					entityManager.persist( person );

					Address address2 = new Address( "GR", "Via Roma", "Gradoli" );
					Person person2 = new Person( 2, "Andrea", 50, address2 );
					entityManager.persist( address2 );
					entityManager.persist( person2 );
				}
		);
	}

	@Test
	public void testSelectCaseStringExpressionReturningAnIntegerValues(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Tuple> criteriaQuery = cb.createTupleQuery();
					Root<Person> personRoot = criteriaQuery.from( Person.class );

					Join<Object, Object> secondaryJoin = personRoot.join( "address" );
					criteriaQuery.multiselect(
							cb.selectCase( secondaryJoin.get( "code" ) )
									.when( "GR", personRoot.get( "age" ) )
									.otherwise( cb.nullLiteral( Integer.class ) )
									.alias( "person_age" )
					).orderBy( cb.asc( personRoot.get( "id" ) ) );

					List<Tuple> ages = entityManager.createQuery( criteriaQuery ).getResultList();
					assertThat( ages.size() ).isEqualTo( 2 );
					assertThat( ages.get( 0 ).get( "person_age" ) ).isNull();
					assertThat( ages.get( 1 ).get( "person_age" ) ).isEqualTo( 50 );
				}
		);

	}

	@Entity
	@Table(name = "PERSON_TABLE")
	public class Person {

		@Id
		private int id;

		private String name;

		private Integer age;

		@ManyToOne
		private Address address;

		public Person() {
		}

		public Person(int id, String name, Integer age, Address address) {
			this.id = id;
			this.name = name;
			this.age = age;
			this.address = address;
		}
	}

	@Entity
	@Table(name = "ADDRESS_TABLE")
	public static class Address {

		@Id
		private String code;

		private String street;

		private String city;

		public Address() {
		}

		public Address(String code, String street, String city) {
			this.code = code;
			this.street = street;
			this.city = city;
		}
	}
}
