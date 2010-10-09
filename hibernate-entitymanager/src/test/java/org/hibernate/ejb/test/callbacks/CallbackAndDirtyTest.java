//$Id$
package org.hibernate.ejb.test.callbacks;

import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class CallbackAndDirtyTest extends TestCase {

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
		mark = manager.find( Employee.class, new Long( ids[0] ) );
		joe = (Customer) manager.find( Customer.class, new Long( ids[1] ) );
		yomomma = manager.find( Person.class, new Long( ids[2] ) );

		mark.setZip( "30306" );
		assertEquals( 1, manager.createQuery( "select p from Person p where p.zip = '30306'" ).getResultList().size() );
		manager.remove( mark );
		manager.remove( joe );
		manager.remove( yomomma );
		assertTrue( manager.createQuery( "select p from Person p" ).getResultList().isEmpty() );
		manager.getTransaction().commit();
		manager.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Customer.class,
				Employee.class,
				Person.class
		};
	}
}
