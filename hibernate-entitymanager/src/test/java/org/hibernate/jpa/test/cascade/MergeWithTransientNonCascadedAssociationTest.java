/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
		@GeneratedValue( generator = "increment" )
		@GenericGenerator( name = "increment", strategy = "increment" )
		private Integer id;
	}
}
