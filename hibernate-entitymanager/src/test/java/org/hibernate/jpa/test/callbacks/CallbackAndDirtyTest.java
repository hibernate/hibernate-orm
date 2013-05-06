/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.callbacks;

import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class CallbackAndDirtyTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testDirtyButNotDirty() throws Exception {
		EntityManager manager = getOrCreateEntityManager();
		manager.getTransaction().begin();
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

		manager.persist( mark );
		manager.persist( joe );
		manager.persist( yomomma );
		long[] ids = {mark.getId(), joe.getId(), yomomma.getId()};
		manager.getTransaction().commit();

		manager.getTransaction().begin();
		assertEquals(
				manager.createQuery( "select p.address, p.name from Person p order by p.name" ).getResultList().size(),
				3
		);
		assertEquals( manager.createQuery( "select p from Person p where p.class = Customer" ).getResultList().size(), 1 );
		manager.getTransaction().commit();

		manager.getTransaction().begin();
		List customers = manager.createQuery( "select c from Customer c left join fetch c.salesperson" ).getResultList();
		for ( Iterator iter = customers.iterator(); iter.hasNext() ; ) {
			Customer c = (Customer) iter.next();
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		manager.getTransaction().commit();

		manager.getTransaction().begin();
		customers = manager.createQuery( "select c from Customer c" ).getResultList();
		for ( Iterator iter = customers.iterator(); iter.hasNext() ; ) {
			Customer c = (Customer) iter.next();
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		manager.getTransaction().commit();

		manager.getTransaction().begin();
		mark = manager.find( Employee.class, Long.valueOf( ids[0] ) );
		joe = manager.find( Customer.class, Long.valueOf( ids[1] ) );
		yomomma = manager.find( Person.class, Long.valueOf( ids[2] ) );

		mark.setZip( "30306" );
		assertEquals( 1, manager.createQuery( "select p from Person p where p.zip = '30306'" ).getResultList().size() );
		manager.remove( mark );
		manager.remove( joe );
		manager.remove( yomomma );
		assertTrue( manager.createQuery( "select p from Person p" ).getResultList().isEmpty() );
		manager.getTransaction().commit();
		manager.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Customer.class,
				Employee.class,
				Person.class
		};
	}
}
