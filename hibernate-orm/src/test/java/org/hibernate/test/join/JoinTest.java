/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.AbstractWork;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class JoinTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "join/Person.hbm.xml" };
	}

	@Test
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

	@Test
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
		doWork(s);
		s.clear();

		jesus = (User) s.get( Person.class, new Long( jesus.getId() ) );
		s.clear();

		// Cleanup the test data
		s.delete(jesus);

		assertTrue( s.createQuery("from Person").list().isEmpty() );
		t.commit();
		s.close();
	}
	
	private void doWork(final Session s) {
		s.doWork(
				new AbstractWork() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( "delete from t_user" );
						ps.execute();
					}
				}
		);
	}
	
	@Test
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		final double HEIGHT_INCHES = 73;
		final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
		Person p = new Person();
		p.setName("Emmanuel");
		p.setSex('M');
		p.setHeightInches(HEIGHT_INCHES);
		s.persist(p);
		final double PASSWORD_EXPIRY_WEEKS = 4;
		final double PASSWORD_EXPIRY_DAYS = PASSWORD_EXPIRY_WEEKS * 7d;
		User u = new User();
		u.setName("Steve");
		u.setSex('M');
		u.setPasswordExpiryDays(PASSWORD_EXPIRY_DAYS);
		s.persist(u);
		s.flush();
		
		// Test value conversion during insert
		// Oracle returns BigDecimaal while other dialects return Double;
		// casting to Number so it works on all dialects
		Number heightViaSql = (Number)s.createSQLQuery("select height_centimeters from person where name='Emmanuel'").uniqueResult();
		assertEquals(HEIGHT_CENTIMETERS, heightViaSql.doubleValue(), 0.01d);
		Number expiryViaSql = (Number)s.createSQLQuery("select pwd_expiry_weeks from t_user where person_id=?")
			.setLong(0, u.getId())
			.uniqueResult();
		assertEquals(PASSWORD_EXPIRY_WEEKS, expiryViaSql.doubleValue(), 0.01d);
		
		// Test projection
		Double heightViaHql = (Double)s.createQuery("select p.heightInches from Person p where p.name = 'Emmanuel'").uniqueResult();
		assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);
		Double expiryViaHql = (Double)s.createQuery("select u.passwordExpiryDays from User u where u.name = 'Steve'").uniqueResult();
		assertEquals(PASSWORD_EXPIRY_DAYS, expiryViaHql, 0.01d);
		
		// Test restriction and entity load via criteria
		p = (Person)s.createCriteria(Person.class)
			.add(Restrictions.between("heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, p.getHeightInches(), 0.01d);
		u = (User)s.createCriteria(User.class)
			.add(Restrictions.between("passwordExpiryDays", PASSWORD_EXPIRY_DAYS - 0.01d, PASSWORD_EXPIRY_DAYS + 0.01d))
			.uniqueResult();
		assertEquals(PASSWORD_EXPIRY_DAYS, u.getPasswordExpiryDays(), 0.01d);
		
		// Test predicate and entity load via HQL
		p = (Person)s.createQuery("from Person p where p.heightInches between ? and ?")
			.setDouble(0, HEIGHT_INCHES - 0.01d)
			.setDouble(1, HEIGHT_INCHES + 0.01d)
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, p.getHeightInches(), 0.01d);
		u = (User)s.createQuery("from User u where u.passwordExpiryDays between ? and ?")
			.setDouble(0, PASSWORD_EXPIRY_DAYS - 0.01d)
			.setDouble(1, PASSWORD_EXPIRY_DAYS + 0.01d)
			.uniqueResult();
		assertEquals(PASSWORD_EXPIRY_DAYS, u.getPasswordExpiryDays(), 0.01d);
		
		// Test update
		p.setHeightInches(1);
		u.setPasswordExpiryDays(7d);
		s.flush();
		heightViaSql = (Number)s.createSQLQuery("select height_centimeters from person where name='Emmanuel'").uniqueResult();
		assertEquals(2.54d, heightViaSql.doubleValue(), 0.01d);
		expiryViaSql = (Number)s.createSQLQuery("select pwd_expiry_weeks from t_user where person_id=?")
			.setLong(0, u.getId())
			.uniqueResult();
		assertEquals(1d, expiryViaSql.doubleValue(), 0.01d);
		
		s.delete(p);
		s.delete(u);
		assertTrue( s.createQuery("from Person").list().isEmpty() );		
		
		t.commit();
		s.close();
		
	}
	

}

