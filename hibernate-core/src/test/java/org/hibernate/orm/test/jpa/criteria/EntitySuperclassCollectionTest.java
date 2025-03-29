/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 * @author Gail Badner
 */
public class EntitySuperclassCollectionTest
		extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				PersonBase.class, Person.class, Address.class
		};
	}

	@Test
	@JiraKey(value = "HHH-10556")
	public void testPerson() {
		String address = "super-address";

		PersonBase person = createPerson( new Person(), address );

		assertAddress( person, address );
	}

	private void assertAddress(PersonBase person, String address) {
		List<Object> results = find(
				person.getClass(),
				person.id,
				"addresses"
		);
		assertEquals( 1, results.size() );

		assertEquals(
				person.addresses.get( 0 ).id,
				( (Address) results.get( 0 ) ).id
		);
		assertEquals( address, ( (Address) results.get( 0 ) ).name );

		getOrCreateEntityManager().close();
	}

	private PersonBase createPerson(PersonBase person, String address) {
		EntityManager em = createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		person.addresses.add( new Address( address ) );
		person = em.merge( person );
		tx.commit();
		return person;
	}

	private List<Object> find(Class<?> clazz, int id, String path) {
		EntityManager em = createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object> cq = cb.createQuery();
		Root<?> root = cq.from( clazz );

		cq.select( root.get( path ) )
				.where( cb.equal( root.get( "id" ), id ) );

		TypedQuery<Object> query = em.createQuery( cq );
		return query.getResultList();
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
		List<Address> addresses = new ArrayList<Address>();
	}

	@Entity(name = "Person")
	public static class Person extends PersonBase {
	}
}
