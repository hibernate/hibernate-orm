package org.hibernate.test.sql.hand.custom;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.util.ArrayHelper;
import org.hibernate.test.sql.hand.Employment;
import org.hibernate.test.sql.hand.Organization;
import org.hibernate.test.sql.hand.Person;
import org.hibernate.test.sql.hand.TextHolder;
import org.hibernate.test.sql.hand.ImageHolder;
import org.hibernate.testing.junit.functional.DatabaseSpecificFunctionalTestCase;

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

	public void testTextProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		String description = buildLongString( 15000, 'a' );
		TextHolder holder = new TextHolder( description );
		s.save( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = ( TextHolder ) s.get(  TextHolder.class, holder.getId() );
		assertEquals( description, holder.getDescription() );
		description = buildLongString( 15000, 'b' );
		holder.setDescription( description );
		s.save( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = ( TextHolder ) s.get(  TextHolder.class, holder.getId() );
		assertEquals( description, holder.getDescription() );
		s.delete( holder );
		t.commit();
		s.close();
	}

	public void testImageProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		byte[] photo = buildLongByteArray( 15000, true );
		ImageHolder holder = new ImageHolder( photo );
		s.save( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = ( ImageHolder ) s.get(  ImageHolder.class, holder.getId() );
		assertTrue( ArrayHelper.isEquals( photo, holder.getPhoto() ) );
		photo = buildLongByteArray( 15000, false );
		holder.setPhoto( photo );
		s.save( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = ( ImageHolder ) s.get(  ImageHolder.class, holder.getId() );
		assertTrue( ArrayHelper.isEquals( photo, holder.getPhoto() ) );
		s.delete( holder );
		t.commit();
		s.close();
	}

	private String buildLongString(int size, char baseChar) {
		StringBuffer buff = new StringBuffer();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}

	private byte[] buildLongByteArray(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? ( byte ) 1 : ( byte ) 0;
	}
}
