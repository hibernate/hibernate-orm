/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.discriminator;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class DiscriminatorTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "discriminator/Person.hbm.xml" };
	}

	@Test
	public void testDiscriminatorSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Employee mark = new Employee();
		mark.setName("Mark");
		mark.setTitle("internal sales");
		mark.setSex('M');
		mark.setAddress("buckhead");
		mark.setZip("30305");
		mark.setCountry("USA");

		Customer joe = new Customer();
		joe.setName("Joe");
		joe.setAddress("San Francisco");
		joe.setZip("XXXXX");
		joe.setCountry("USA");
		joe.setComments("Very demanding");
		joe.setSex('M');
		joe.setSalesperson(mark);

		Person yomomma = new Person();
		yomomma.setName("mum");
		yomomma.setSex('F');

		s.save(yomomma);
		s.save(mark);
		s.save(joe);

		assertEquals( s.createQuery("from java.io.Serializable").list().size(), 0 );

		assertEquals( s.createQuery("from Person").list().size(), 3 );
		assertEquals( s.createQuery("from Person p where p.class = Person").list().size(), 1 );
		assertEquals( s.createQuery("from Person p where p.class = Customer").list().size(), 1 );
		s.clear();

		List customers = s.createQuery("from Customer c left join fetch c.salesperson").list();
		for ( Object customer : customers ) {
			Customer c = (Customer) customer;
			assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();

		customers = s.createQuery("from Customer").list();
		for ( Object customer : customers ) {
			Customer c = (Customer) customer;
			assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();


		mark = (Employee) s.get( Employee.class, new Long( mark.getId() ) );
		joe = (Customer) s.get( Customer.class, new Long( joe.getId() ) );

 		mark.setZip("30306");
		assertEquals( s.createQuery("from Person p where p.address.zip = '30306'").list().size(), 1 );
		s.delete(mark);
		s.delete(joe);
		s.delete(yomomma);
		assertTrue( s.createQuery("from Person").list().isEmpty() );
		t.commit();
		s.close();
	}

	@Test
	public void testAccessAsIncorrectSubclass() {
		Session s = openSession();
		s.beginTransaction();
		Employee e = new Employee();
		e.setName( "Steve" );
		e.setSex( 'M' );
		e.setTitle( "grand poobah" );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Customer c = ( Customer ) s.get( Customer.class, new Long( e.getId() ) );
		s.getTransaction().commit();
		s.close();
		assertNull( c );

		s = openSession();
		s.beginTransaction();
		e = ( Employee ) s.get( Employee.class, new Long( e.getId() ) );
		c = ( Customer ) s.get( Customer.class, new Long( e.getId() ) );
		s.getTransaction().commit();
		s.close();
		assertNotNull( e );
		assertNull( c );

		s = openSession();
		s.beginTransaction();
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQuerySubclassAttribute() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Emmanuel");
		p.setSex('M');
		s.persist(p);
		Employee q = new Employee();
		q.setName("Steve");
		q.setSex('M');
		q.setTitle("Mr");
		q.setSalary( new BigDecimal(1000) );
		s.persist(q);

		List result = s.createQuery("from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertSame( result.get(0), q );

		result = s.createQuery("from Person where salary > 100 or name like 'E%'").list();
		assertEquals( result.size(), 2 );

		result = s.createCriteria(Person.class)
			.add( Property.forName("salary").gt( new BigDecimal(100) ) )
			.list();
		assertEquals( result.size(), 1 );
		assertSame( result.get(0), q );

		//TODO: make this work:
		/*result = s.createQuery("select salary from Person where salary > 100").list();
		assertEquals( result.size(), 1 );
		assertEquals( result.get(0), new BigDecimal(1000) );*/

		s.delete(p);
		s.delete(q);
		t.commit();
		s.close();
	}

	@Test
	public void testLoadSuperclassProxyPolymorphicAccess() {
		Session s = openSession();
		s.beginTransaction();
		Employee e = new Employee();
		e.setName( "Steve" );
		e.setSex( 'M' );
		e.setTitle( "grand poobah" );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// load the superclass proxy.
		Person pLoad = ( Person ) s.load( Person.class, new Long( e.getId() ) );
		assertTrue( pLoad instanceof HibernateProxy);
		Person pGet = ( Person ) s.get( Person.class, new Long( e.getId() ));
		Person pQuery = ( Person ) s.createQuery( "from Person where id = :id" )
				.setLong( "id", e.getId() )
				.uniqueResult();
		Person pCriteria = ( Person ) s.createCriteria( Person.class )
				.add( Restrictions.idEq( new Long( e.getId() ) ) )
				.uniqueResult();
		// assert that executing the queries polymorphically returns the same proxy
		assertSame( pLoad, pGet );
		assertSame( pLoad, pQuery );
		assertSame( pLoad, pCriteria );

		// assert that the proxy is not an instance of Employee
		assertFalse( pLoad instanceof Employee );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testLoadSuperclassProxyEvictPolymorphicAccess() {
		Session s = openSession();
		s.beginTransaction();
		Employee e = new Employee();
		e.setName( "Steve" );
		e.setSex( 'M' );
		e.setTitle( "grand poobah" );
		s.save( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// load the superclass proxy.
		Person pLoad = ( Person ) s.load( Person.class, new Long( e.getId() ) );
		assertTrue( pLoad instanceof HibernateProxy);
		// evict the proxy
		s.evict( pLoad );
		Employee pGet = ( Employee ) s.get( Person.class, new Long( e.getId() ));
		Employee pQuery = ( Employee ) s.createQuery( "from Person where id = :id" )
				.setLong( "id", e.getId() )
				.uniqueResult();
		Employee pCriteria = ( Employee ) s.createCriteria( Person.class )
				.add( Restrictions.idEq( new Long( e.getId() ) ) )
				.uniqueResult();
		// assert that executing the queries polymorphically returns the same Employee instance
		assertSame( pGet, pQuery );
		assertSame( pGet, pCriteria );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( e );
		s.getTransaction().commit();
		s.close();
	}
}
