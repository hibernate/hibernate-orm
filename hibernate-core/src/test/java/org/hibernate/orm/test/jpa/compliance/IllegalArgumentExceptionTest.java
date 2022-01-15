/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import static org.junit.jupiter.api.Assertions.fail;

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

		try {
			List list = new ArrayList();
			list.add( person.get( "id" ).alias( "a" ) );
			list.add( person.get( "name" ).alias( "a" ) );

			query.multiselect( list );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testCriteriaTupleQuerySameAlias1(EntityManagerFactoryScope scope) {

		final CriteriaQuery<Tuple> query = scope.getEntityManagerFactory().getCriteriaBuilder().createTupleQuery();
		final Root<Person> person = query.from( Person.class );

		Selection[] selection = {
				person.get( "id" ).alias( "a" ),
				person.get( "name" ).alias( "a" )
		};

		try {
			query.multiselect( selection );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testCriteriaTupleQueryNonExistingAttributeNames(EntityManagerFactoryScope scope) {

		final CriteriaQuery<Tuple> query = scope.getEntityManagerFactory().getCriteriaBuilder().createTupleQuery();
		final Root<Person> person = query.from( Person.class );

		try {
			query.multiselect(
					person.get( "not_existing_attribute_name" ).alias( "a1" ),
					person.get( "another_not_existing_attribute_name" ).alias( "a2" )
			);
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			// expected
		}
	}

	@Test
	public void testCriteriaStringQuery(EntityManagerFactoryScope scope) {

		final CriteriaQuery<String> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( String.class );
		try {
			final Root<Person> person = query.from( Person.class );
			person.get( "not_existing_attribute_name" );

			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testGetStringNonExistingAttributeName(EntityManagerFactoryScope scope) {
		try {
			final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
					.getCriteriaBuilder()
					.createQuery( Person.class );
			query.from( Person.class ).get( "not_existing_attribute_name" );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testJoinANonExistingAttributeNameToAFrom(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );
		final From<Person, Person> customer = query.from( Person.class );
		try {
			customer.join( "not_existing_attribute_name" );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testJoinANonExistingAttributeNameToAFrom2(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );
		final From<Person, Person> customer = query.from( Person.class );
		try {
			customer.join( "not_existing_attribute_name", JoinType.INNER );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testJoinANonExistingAttributeNameToAJoin(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final Root<Person> customer = query.from( Person.class );
		final Join<Person, Address> address = customer.join( "address" );
		try {
			address.join( "not_existing_attribute_name" );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void testJoinANonExistingAttributeNameToAJoin2(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final Root<Person> customer = query.from( Person.class );
		final Join<Person, Address> address = customer.join( "address" );
		try {
			address.join( "not_existing_attribute_name", JoinType.INNER );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
	}

	@Test
	public void fetchFetchStringIllegalArgumentExceptionTest(EntityManagerFactoryScope scope) {
		final CriteriaQuery<Person> query = scope.getEntityManagerFactory()
				.getCriteriaBuilder()
				.createQuery( Person.class );

		final From<Person, Person> customer = query.from( Person.class );
		final Fetch f = customer.fetch( "address" );

		try {
			f.fetch( "not_existing_attribute_name" );
			fail( "TCK expects an IllegalArgumentException" );
		}
		catch (IllegalArgumentException iae) {
			//expected by TCK
		}
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
