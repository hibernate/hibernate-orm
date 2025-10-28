/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Janario Oliveira
 * @author Gail Badner
 */
@Jpa(annotatedClasses = {
		SuperclassCollectionTest.PersonBaseBase.class,
		SuperclassCollectionTest.Person.class,
		SuperclassCollectionTest.OtherPerson.class,
		SuperclassCollectionTest.Address.class,
		SuperclassCollectionTest.OtherSubclass.class
})
public class SuperclassCollectionTest {

	@Test
	public void testPerson(EntityManagerFactoryScope scope) {
		String address = "super-address";
		String localAddress = "local-address";

		PersonBaseBase person = createPerson( scope, new Person(), address, localAddress );

		assertAddress( scope, person, address, localAddress );
	}

	@Test
	public void testOtherSubclass(EntityManagerFactoryScope scope) {
		String address = "other-super-address";
		String localAddress = "other-local-address";
		PersonBaseBase person = createPerson( scope, new OtherSubclass(), address, localAddress );

		assertAddress( scope, person, address, localAddress );
	}

	@Test
	@JiraKey( value = "HHH-10556")
	public void testOtherPerson(EntityManagerFactoryScope scope) {
		String address = "other-person-super-address";
		String localAddress = "other-person-local-address";

		PersonBaseBase person = createPerson( scope, new OtherPerson(), address, localAddress );

		assertAddress( scope, person, address, localAddress );
	}

	private void assertAddress(EntityManagerFactoryScope scope, PersonBaseBase person, String address, String localAddress) {
		List<Object> results = find( scope, person.getClass(), person.id, "addresses" );
		assertEquals( 1, results.size() );

		assertEquals( person.addresses.get( 0 ).id, ( (Address) results.get( 0 ) ).id );
		assertEquals( address, ( (Address) results.get( 0 ) ).name );


		results = find( scope, person.getClass(), person.id, "localAddresses" );
		assertEquals( 1, results.size() );

		assertEquals( person.getLocalAddresses().get( 0 ).id, ( (Address) results.get( 0 ) ).id );
		assertEquals( localAddress, ( (Address) results.get( 0 ) ).name );

	}

	private PersonBaseBase createPerson(EntityManagerFactoryScope scope, PersonBaseBase person, String address, String localAddress) {
		PersonBaseBase personBaseBase;

		person.addresses.add( new Address( address ) );
		person.getLocalAddresses().add( new Address( localAddress ) );
		personBaseBase = scope.fromTransaction( entityManager -> entityManager.merge( person ) );
		return personBaseBase;
	}

	private List<Object> find(EntityManagerFactoryScope scope, Class<?> clazz, int id, String path) {
		return scope.fromEntityManager(  entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object> cq = cb.createQuery();
			Root<?> root = cq.from( clazz );

			cq.select( root.get( path ) )
					.where( cb.equal( root.get( "id" ), id ) );

			TypedQuery<Object> query = entityManager.createQuery( cq );
			return query.getResultList();
		} );
	}

	@Entity(name="Address")
	public static class Address {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		protected Address() {
		}

		public Address(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	public abstract static class PersonBaseBase {
		@Id
		@GeneratedValue
		Integer id;
		@OneToMany(cascade = CascadeType.ALL)
		List<Address> addresses = new ArrayList<>();

		protected abstract List<Address> getLocalAddresses();
	}

	@MappedSuperclass
	public abstract static class PersonBase extends PersonBaseBase {
	}

	@Entity(name="Person")
	public static class Person extends PersonBase {
		@OneToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "person_localaddress")
		List<Address> localAddresses = new ArrayList<>();

		@Override
		public List<Address> getLocalAddresses() {
			return localAddresses;
		}
	}

	@MappedSuperclass
	public static class OtherPersonBase extends Person {
	}

	@Entity(name="OtherPerson")
	public static class OtherPerson extends OtherPersonBase {
	}

	@Entity(name="OtherSubclass")
	public static class OtherSubclass extends PersonBaseBase {
		@OneToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "other_person_localaddress")
		List<Address> localAddresses = new ArrayList<>();

		@Override
		public List<Address> getLocalAddresses() {
			return localAddresses;
		}
	}
}
