/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;


import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Max Rydahl Andersen
 */
public class VersionTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "org/hibernate/orm/test/version/PersonThing.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Test
	public void testVersionShortCircuitFlush() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person gavin = new Person("Gavin");
		new Thing("Passport", gavin);
		s.persist(gavin);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Thing passp = (Thing) s.get(Thing.class, "Passport");
		passp.setLongDescription("blah blah blah");
		s.createQuery("from Person").list();
		s.createQuery("from Person").list();
		s.createQuery("from Person").list();
		t.commit();
		s.close();

		assertEquals( 1, passp.getVersion() );

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from Thing").executeUpdate();
		s.createQuery("delete from Person").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-11549")
	public void testMetamodelContainsHbmVersion() {
		try (Session session = openSession()) {
			session.getMetamodel().entity( Person.class ).getAttribute( "version" );
		}
	}

	@Test
	public void testCollectionVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person gavin = new Person("Gavin");
		new Thing("Passport", gavin);
		s.persist(gavin);
		t.commit();
		s.close();

		assertEquals(0, gavin.getVersion());

		s = openSession();
		t = s.beginTransaction();
		gavin = getPerson( s );
		new Thing("Laptop", gavin);
		t.commit();
		s.close();

		assertEquals(1, gavin.getVersion());
		assertFalse( Hibernate.isInitialized( gavin.getThings() ) );

		s = openSession();
		t = s.beginTransaction();
		gavin = getPerson( s );
		gavin.getThings().clear();
		t.commit();
		s.close();

		assertEquals(2, gavin.getVersion());
		assertTrue( Hibernate.isInitialized( gavin.getThings() ) );

		s = openSession();
		t = s.beginTransaction();
		s.remove(gavin);
		t.commit();
		s.close();
	}

	@Test
	public void testCollectionNoVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person gavin = new Person("Gavin");
		new Task("Code", gavin);
		s.persist(gavin);
		t.commit();
		s.close();

		assertEquals(0, gavin.getVersion());

		s = openSession();
		t = s.beginTransaction();
		gavin = getPerson( s );
		new Task("Document", gavin);
		t.commit();
		s.close();

		assertEquals(0, gavin.getVersion());
		assertFalse( Hibernate.isInitialized( gavin.getTasks() ) );

		s = openSession();
		t = s.beginTransaction();
		gavin = getPerson( s );
		gavin.getTasks().clear();
		t.commit();
		s.close();

		assertEquals(0, gavin.getVersion());
		assertTrue( Hibernate.isInitialized( gavin.getTasks() ) );

		s = openSession();
		t = s.beginTransaction();
		s.remove(gavin);
		t.commit();
		s.close();
	}

	private Person getPerson(Session s) {
		return session.createQuery( "select p from Person p", Person.class ).uniqueResult();
	}
}
