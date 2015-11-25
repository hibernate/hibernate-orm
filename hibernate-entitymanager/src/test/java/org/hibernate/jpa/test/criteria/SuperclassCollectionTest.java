/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Janario Oliveira
 */
public class SuperclassCollectionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				PersonBase.class, Person.class, Address.class,
				OtherSubclass.class
		};
	}

	@Test
	public void testPerson() {
		String address = "super-address";
		String localAddress = "local-address";

		PersonBase person = createPerson( new Person(), address, localAddress );

		assertAddress( person, address, localAddress );
	}

	@Test
	public void testOtherSubclass() {
		String address = "other-super-address";
		String localAddress = "other-local-address";
		PersonBase person = createPerson( new OtherSubclass(), address, localAddress );

		assertAddress( person, address, localAddress );
	}

	private void assertAddress(PersonBase person, String address, String localAddress) {
		List<Object> results = find( person.getClass(), person.id, "addresses" );
		assertEquals( 1, results.size() );

		assertEquals( person.addresses.get( 0 ).id, ( (Address) results.get( 0 ) ).id );
		assertEquals( address, ( (Address) results.get( 0 ) ).name );


		results = find( person.getClass(), person.id, "localAddresses" );
		assertEquals( 1, results.size() );

		assertEquals( person.getLocalAddresses().get( 0 ).id, ( (Address) results.get( 0 ) ).id );
		assertEquals( localAddress, ( (Address) results.get( 0 ) ).name );

		getOrCreateEntityManager().close();
	}

	private PersonBase createPerson(PersonBase person, String address, String localAddress) {
		EntityManager em = createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		person.addresses.add( new Address( address ) );
		person.getLocalAddresses().add( new Address( localAddress ) );
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
	public abstract static class PersonBase {
		@Id
		@GeneratedValue
		Integer id;
		@OneToMany(cascade = CascadeType.ALL)
		List<Address> addresses = new ArrayList<Address>();

		protected abstract List<Address> getLocalAddresses();
	}

	@Entity(name="Person")
	public static class Person extends PersonBase {
		@OneToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "person_localaddress")
		List<Address> localAddresses = new ArrayList<Address>();

		@Override
		public List<Address> getLocalAddresses() {
			return localAddresses;
		}
	}

	@Entity(name="OtherSubclass")
	public static class OtherSubclass extends PersonBase {
		@OneToMany(cascade = CascadeType.ALL)
		@JoinTable(name = "other_person_localaddress")
		List<Address> localAddresses = new ArrayList<Address>();

		@Override
		public List<Address> getLocalAddresses() {
			return localAddresses;
		}
	}
}
