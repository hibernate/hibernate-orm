//$Id: JoinTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.join;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class JoinTest extends FunctionalTestCase {
	
	public JoinTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "join/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( JoinTest.class );
	}
	
	public void testSequentialSelects() {
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
		assertEquals( s.createQuery("from Person p where p.class is null").list().size(), 1 );
		assertEquals( s.createQuery("from Person p where p.class = Customer").list().size(), 1 );
		assertTrue(s.createQuery("from Customer c").list().size()==1);
		s.clear();

		List customers = s.createQuery("from Customer c left join fetch c.salesperson").list();
		for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
			Customer c = (Customer) iter.next();
			assertTrue( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();
		
		customers = s.createQuery("from Customer").list();
		for ( Iterator iter = customers.iterator(); iter.hasNext(); ) {
			Customer c = (Customer) iter.next();
			assertFalse( Hibernate.isInitialized( c.getSalesperson() ) );
			assertEquals( c.getSalesperson().getName(), "Mark" );
		}
		assertEquals( customers.size(), 1 );
		s.clear();
		

		mark = (Employee) s.get( Employee.class, new Long( mark.getId() ) );
		joe = (Customer) s.get( Customer.class, new Long( joe.getId() ) );
		
 		mark.setZip("30306");
		assertEquals( s.createQuery("from Person p where p.zip = '30306'").list().size(), 1 );
		s.delete(mark);
		s.delete(joe);
		s.delete(yomomma);
		assertTrue( s.createQuery("from Person").list().isEmpty() );
		t.commit();
		s.close();
	}

	public void testSequentialSelectsOptionalData() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		User jesus = new User();
		jesus.setName("Jesus Olvera y Martinez");
		jesus.setSex('M');

		s.save(jesus);

		assertEquals( s.createQuery("from java.io.Serializable").list().size(), 0 );
		
		assertEquals( s.createQuery("from Person").list().size(), 1 );
		assertEquals( s.createQuery("from Person p where p.class is null").list().size(), 0 );
		assertEquals( s.createQuery("from Person p where p.class = User").list().size(), 1 );
		assertTrue(s.createQuery("from User u").list().size()==1);
		s.clear();

		// Remove the optional row from the join table and requery the User obj
		s.connection().prepareStatement("delete from t_user").execute();
		s.clear();

		jesus = (User) s.get( Person.class, new Long( jesus.getId() ) );
		s.clear();

		// Cleanup the test data
		s.delete(jesus);

		assertTrue( s.createQuery("from Person").list().isEmpty() );
		t.commit();
		s.close();
	}

}

