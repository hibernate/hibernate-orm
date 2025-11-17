/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

@Jpa(
		annotatedClasses = {
				IllegalArgumentExceptionTest.Person.class,
				IllegalArgumentExceptionTest.Address.class
		}
)
public class IllegalArgumentExceptionTest {

	@Test
	public void testCriteriaTupleQuerySameAlias(EntityManagerFactoryScope scope) {

		final CriteriaQuery<Tuple> query = scope.getEntityManagerFactory().getCriteriaBuilder().createTupleQuery();
		final Root<Person> person = query.from( Person.class );

		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					List list = new ArrayList();
					list.add( person.get( "id" ).alias( "a" ) );
					list.add( person.get( "name" ).alias( "a" ) );

					query.multiselect( list );
				}
		);
	}

	@Test
	public void testCriteriaTupleQuerySameAlias1(EntityManagerFactoryScope scope) {

		final CriteriaQuery<Tuple> query = scope.getEntityManagerFactory().getCriteriaBuilder().createTupleQuery();
		final Root<Person> person = query.from( Person.class );

		Selection[] selection = {
				person.get( "id" ).alias( "a" ),
				person.get( "name" ).alias( "a" )
		};

		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						query.multiselect( selection )
		);
	}

	@Test
	public void testCriteriaTupleQueryNonExistingAttributeNames(EntityManagerFactoryScope scope) {

		final CriteriaQuery<Tuple> query = scope.getEntityManagerFactory().getCriteriaBuilder().createTupleQuery();
		final Root<Person> person = query.from( Person.class );

		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						query.multiselect(
								person.get( "not_existing_attribute_name" ).alias( "a1" ),
								person.get( "another_not_existing_attribute_name" ).alias( "a2" )
						)
		);
	}

	@Test
	public void testCriteriaStringQuery(EntityManagerFactoryScope scope) {

		final CriteriaQuery<String> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( String.class );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					final Root<Person> person = query.from( Person.class );
					person.get( "not_existing_attribute_name" );
				}

		);
	}

	@Test
	public void testGetStringNonExistingAttributeName(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> {
					final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
							.getCriteriaBuilder()
							.createQuery( Person.class );
					query.from( Person.class ).get( "not_existing_attribute_name" );
				}
		);
	}

	@Test
	public void testJoinANonExistingAttributeNameToAFrom(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );
		final From<Person, Person> customer = query.from( Person.class );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						customer.join( "not_existing_attribute_name" )
		);
	}

	@Test
	public void testJoinANonExistingAttributeNameToAFrom2(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );
		final From<Person, Person> customer = query.from( Person.class );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						customer.join( "not_existing_attribute_name", JoinType.INNER )
		);
	}

	@Test
	public void testJoinANonExistingAttributeNameToAJoin(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final Root<Person> customer = query.from( Person.class );
		final Join<Person, Address> address = customer.join( "address" );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						address.join( "not_existing_attribute_name" )
		);
	}

	@Test
	public void testJoinANonExistingAttributeNameToAJoin2(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final Root<Person> customer = query.from( Person.class );
		final Join<Person, Address> address = customer.join( "address" );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						address.join( "not_existing_attribute_name", JoinType.INNER )
		);
	}

	@Test
	public void fetchFetchStringIllegalArgumentExceptionTest(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final From<Person, Person> customer = query.from( Person.class );
		final Fetch f = customer.fetch( "address" );

		Assertions.assertThrows(
				IllegalArgumentException.class,
				() ->
						f.fetch( "not_existing_attribute_name" )
		);
	}

	@Test
	public void testHqlQueryWithWrongSemantic(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() ->
									entityManager.createQuery( "Seletc p" ).getResultList()
					);
				}
		);

	}

	@Test
	public void testCriteriaNullReturnType(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								CriteriaBuilder criteriaBuilder = scope.getEntityManagerFactory().getCriteriaBuilder();
								CriteriaQuery criteriaQuery = criteriaBuilder.createQuery( null );
								entityManager.createQuery( criteriaQuery ).getResultList();
							}
					);
				}
		);
	}

	@Test
	public void testNonExistingNativeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() -> {
									entityManager.createNamedQuery( "NonExisting_NativeQuery" );
								}
						)
		);
	}

	@Test
	public void testQueryWrongReturnType(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								entityManager.createQuery( "select p from Peron p", Integer.class ).getResultList();
							}
					);
				}
		);

	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@ManyToOne
		private Address address;

		Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		private Integer id;

		private String street;

		private String city;

		private String zipcode;

		Address() {
		}

		public Address(Integer id, String street, String city, String zipcode) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.zipcode = zipcode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}


		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}
}
