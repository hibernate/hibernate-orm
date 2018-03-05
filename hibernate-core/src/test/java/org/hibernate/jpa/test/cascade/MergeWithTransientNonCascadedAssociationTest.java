/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class MergeWithTransientNonCascadedAssociationTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Test
	public void testMergeWithTransientNonCascadedAssociation() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Person person = new Person();
		em.persist( person );
		em.getTransaction().commit();
		em.close();

		person.address = new Address();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge( person );
		try {
			em.flush();
			fail( "Expecting IllegalStateException" );
		}
		catch (IllegalStateException ise) {
			// expected...
			em.getTransaction().rollback();
		}
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		person.address = null;
		em.unwrap( Session.class ).lock( person, LockMode.NONE );
		em.unwrap( Session.class ).delete( person );
		em.getTransaction().commit();
		em.close();
	}

	@Entity( name = "Person" )
	public static class Person {
		@Id
		@GeneratedValue( generator = "increment" )
		@GenericGenerator( name = "increment", strategy = "increment" )
		private Integer id;
		@ManyToOne
		private Address address;

		public Person() {
		}
	}

	@Entity( name = "Address" )
	public static class Address {
		@Id
		@GeneratedValue( generator = "increment_1" )
		@GenericGenerator( name = "increment_1", strategy = "increment" )
		private Integer id;
	}
}
