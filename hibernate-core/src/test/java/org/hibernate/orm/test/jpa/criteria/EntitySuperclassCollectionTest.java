/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Janario Oliveira
 * @author Gail Badner
 */
@Jpa(annotatedClasses = {
		EntitySuperclassCollectionTest.PersonBase.class,
		EntitySuperclassCollectionTest.Person.class,
		EntitySuperclassCollectionTest.Address.class}
)
public class EntitySuperclassCollectionTest {

	@Test
	@JiraKey(value = "HHH-10556")
	public void testPerson(EntityManagerFactoryScope scope) {
		String address = "super-address";

		PersonBase person = createPerson( scope, new Person(), address );

		assertAddress( scope, person, address );
	}

	private void assertAddress(EntityManagerFactoryScope scope, PersonBase person, String address) {
		List<Object> results = find(
				scope,
				person.getClass(),
				person.id,
				"addresses"
		);
		assertEquals( 1, results.size() );

		assertEquals(
				person.addresses.get( 0 ).id,
				((Address) results.get( 0 )).id
		);
		assertEquals( address, ((Address) results.get( 0 )).name );
	}

	private PersonBase createPerson(EntityManagerFactoryScope scope, PersonBase person, String address) {
		PersonBase personBase;

		person.addresses.add( new Address( address ) );
		personBase = scope.fromTransaction( entityManager -> entityManager.merge( person ) );
		return personBase;
	}

	private List<Object> find(EntityManagerFactoryScope scope, Class<?> clazz, int id, String path) {
		return scope.fromEntityManager( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object> cq = cb.createQuery();
			Root<?> root = cq.from( clazz );

			cq.select( root.get( path ) )
					.where( cb.equal( root.get( "id" ), id ) );

			TypedQuery<Object> query = entityManager.createQuery( cq );
			return query.getResultList();
		} );
	}

	@Entity(name = "Address")
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

	@Entity(name = "PersonBase")
	public abstract static class PersonBase {
		@Id
		@GeneratedValue
		Integer id;
		@OneToMany(cascade = CascadeType.ALL)
		List<Address> addresses = new ArrayList<>();
	}

	@Entity(name = "Person")
	public static class Person extends PersonBase {
	}
}
