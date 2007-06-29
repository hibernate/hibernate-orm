package org.hibernate.test.sql.hand.custom;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.sql.hand.Employment;
import org.hibernate.test.sql.hand.Organization;
import org.hibernate.test.sql.hand.Person;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * Abstract test case defining tests for the support for user-supplied (aka
 * custom) insert, update, delete SQL.
 *
 * @author Steve Ebersole
 */
public abstract class CustomSQLTestSupport extends DatabaseSpecificFunctionalTestCase {

	public CustomSQLTestSupport(String name) {
		super( name );
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void testHandSQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization( "IFA" );
		Organization jboss = new Organization( "JBoss" );
		Person gavin = new Person( "Gavin" );
		Employment emp = new Employment( gavin, jboss, "AU" );
		Serializable orgId = s.save( jboss );
		s.save( ifa );
		s.save( gavin );
		s.save( emp );
		t.commit();

		t = s.beginTransaction();
		Person christian = new Person( "Christian" );
		s.save( christian );
		Employment emp2 = new Employment( christian, jboss, "EU" );
		s.save( emp2 );
		t.commit();
		s.close();

		getSessions().evict( Organization.class );
		getSessions().evict( Person.class );
		getSessions().evict( Employment.class );

		s = openSession();
		t = s.beginTransaction();
		jboss = ( Organization ) s.get( Organization.class, orgId );
		assertEquals( jboss.getEmployments().size(), 2 );
		assertEquals( jboss.getName(), "JBOSS" );
		emp = ( Employment ) jboss.getEmployments().iterator().next();
		gavin = emp.getEmployee();
		assertEquals( gavin.getName(), "GAVIN" );
		assertEquals( s.getCurrentLockMode( gavin ), LockMode.UPGRADE );
		emp.setEndDate( new Date() );
		Employment emp3 = new Employment( gavin, jboss, "US" );
		s.save( emp3 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator iter = s.getNamedQuery( "allOrganizationsWithEmployees" ).list().iterator();
		assertTrue( iter.hasNext() );
		Organization o = ( Organization ) iter.next();
		assertEquals( o.getEmployments().size(), 3 );
		Iterator iter2 = o.getEmployments().iterator();
		while ( iter2.hasNext() ) {
			Employment e = ( Employment ) iter2.next();
			s.delete( e );
		}
		iter2 = o.getEmployments().iterator();
		while ( iter2.hasNext() ) {
			Employment e = ( Employment ) iter2.next();
			s.delete( e.getEmployee() );
		}
		s.delete( o );
		assertFalse( iter.hasNext() );
		s.delete( ifa );
		t.commit();
		s.close();
	}

}
