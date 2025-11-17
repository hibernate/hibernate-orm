/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Customer.class,
		Employee.class,
		Person.class
})
public class CallbackAndDirtyTest {
	@Test
	public void testDirtyButNotDirty(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			Employee mark = new Employee();
			mark.setName( "Mark" );
			mark.setTitle( "internal sales" );
			mark.setSex( 'M' );
			mark.setAddress( "buckhead" );
			mark.setZip( "30305" );
			mark.setCountry( "USA" );

			Customer joe = new Customer();
			joe.setName( "Joe" );
			joe.setSex( 'M' );
			joe.setAddress( "San Francisco" );
			joe.setZip( "XXXXX" );
			joe.setCountry( "USA" );
			joe.setComments( "Very demanding" );
			joe.setSalesperson( mark );

			Person yomomma = new Person();
			yomomma.setName( "mum" );
			yomomma.setSex( 'F' );

			entityManager.persist( mark );
			entityManager.persist( joe );
			entityManager.persist( yomomma );
			long[] ids = {mark.getId(), joe.getId(), yomomma.getId()};
			entityManager.getTransaction().commit();
			entityManager.getTransaction().begin();
			assertEquals(
					3,
					entityManager.createQuery( "select p.address, p.name from Person p order by p.name" ).getResultList().size()
			);
			assertEquals( 1,
					entityManager.createQuery( "select p from Person p where p.class = Customer" ).getResultList().size() );
			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();
			List customers = entityManager.createQuery( "select c from Customer c left join fetch c.salesperson" ).getResultList();
			for ( Object customer : customers ) {
				Customer c = (Customer) customer;
				assertEquals( "Mark", c.getSalesperson().getName() );
			}
			assertEquals( 1, customers.size() );
			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();
			customers = entityManager.createQuery( "select c from Customer c" ).getResultList();
			for ( Object customer : customers ) {
				Customer c = (Customer) customer;
				assertEquals( "Mark", c.getSalesperson().getName() );
			}
			assertEquals( 1, customers.size() );
			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();
			mark = entityManager.find( Employee.class, ids[0] );
			joe = entityManager.find( Customer.class, ids[1] );
			yomomma = entityManager.find( Person.class, ids[2] );

			mark.setZip( "30306" );
			assertEquals( 1, entityManager.createQuery( "select p from Person p where p.zip = '30306'" ).getResultList().size() );
			entityManager.remove( mark );
			entityManager.remove( joe );
			entityManager.remove( yomomma );
			assertTrue( entityManager.createQuery( "select p from Person p" ).getResultList().isEmpty() );
			entityManager.getTransaction().commit();
		} );
	}

}
