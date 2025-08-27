/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.ImageHolder;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.orm.test.sql.hand.TextHolder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Abstract test case defining tests for the support for user-supplied (aka
 * custom) insert, update, delete SQL.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public abstract class CustomSQLTestSupport extends BaseCoreFunctionalTestCase {
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testHandSQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization( "IFA" );
		Organization jboss = new Organization( "JBoss" );
		Person gavin = new Person( "Gavin" );
		Employment emp = new Employment( gavin, jboss, "AU" );
		s.persist( jboss );
		Object orgId = jboss.getId();
		s.persist( ifa );
		s.persist( gavin );
		s.persist( emp );
		t.commit();

		t = s.beginTransaction();
		Person christian = new Person( "Christian" );
		s.persist( christian );
		Employment emp2 = new Employment( christian, jboss, "EU" );
		s.persist( emp2 );
		t.commit();
		s.close();

		sessionFactory().getCache().evictEntityData( Organization.class );
		sessionFactory().getCache().evictEntityData( Person.class );
		sessionFactory().getCache().evictEntityData( Employment.class );

		s = openSession();
		t = s.beginTransaction();
		jboss = ( Organization ) s.get( Organization.class, orgId );
		assertEquals( jboss.getEmployments().size(), 2 );
		assertEquals( jboss.getName(), "JBOSS" );
		emp = ( Employment ) jboss.getEmployments().iterator().next();
		gavin = emp.getEmployee();
		assertEquals( "GAVIN" , gavin.getName() );
		assertEquals( LockMode.PESSIMISTIC_WRITE , s.getCurrentLockMode( gavin ));
		emp.setEndDate( new Date() );
		Employment emp3 = new Employment( gavin, jboss, "US" );
		s.persist( emp3 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator itr = s.getNamedQuery( "allOrganizationsWithEmployees" ).list().iterator();
		assertTrue( itr.hasNext() );
		Organization o = ( Organization ) itr.next();
		assertEquals( o.getEmployments().size(), 3 );
		Iterator itr2 = o.getEmployments().iterator();
		while ( itr2.hasNext() ) {
			Employment e = ( Employment ) itr2.next();
			s.remove( e );
		}
		itr2 = o.getEmployments().iterator();
		while ( itr2.hasNext() ) {
			Employment e = ( Employment ) itr2.next();
			s.remove( e.getEmployee() );
		}
		s.remove( o );
		assertFalse( itr.hasNext() );
		s.remove( ifa );
		t.commit();
		s.close();
	}

	@Test
	public void testTextProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		String description = buildLongString( 15000, 'a' );
		TextHolder holder = new TextHolder( description );
		s.persist( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = s.get(  TextHolder.class, holder.getId() );
		assertEquals( description, holder.getDescription() );
		description = buildLongString( 15000, 'b' );
		holder.setDescription( description );
		s.persist( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = s.get( TextHolder.class, holder.getId() );
		assertEquals( description, holder.getDescription() );
		s.remove( holder );
		t.commit();
		s.close();
	}

	@Test
	public void testImageProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		// Make sure the last byte is non-zero as Sybase cuts that off
		byte[] photo = buildLongByteArray( 14999, true );
		ImageHolder holder = new ImageHolder( photo );
		s.persist( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = s.get( ImageHolder.class, holder.getId() );
		assertTrue( Arrays.equals( photo, holder.getPhoto() ) );
		photo = buildLongByteArray( 15000, false );
		holder.setPhoto( photo );
		s.persist( holder );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder = s.get( ImageHolder.class, holder.getId() );
		assertTrue( Arrays.equals( photo, holder.getPhoto() ) );
		s.remove( holder );
		t.commit();
		s.close();
	}

	private String buildLongString(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
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
