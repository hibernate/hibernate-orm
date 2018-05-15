/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.version;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
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
		return new String[] { "version/PersonThing.hbm.xml" };
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
		
		assertEquals( passp.getVersion(), 1 );

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete from Thing").executeUpdate();
		s.createQuery("delete from Person").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11549")
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
		gavin = (Person) s.createCriteria(Person.class).uniqueResult();
		new Thing("Laptop", gavin);
		t.commit();
		s.close();
		
		assertEquals(1, gavin.getVersion());
		assertFalse( Hibernate.isInitialized( gavin.getThings() ) );

		s = openSession();
		t = s.beginTransaction();
		gavin = (Person) s.createCriteria(Person.class).uniqueResult();
		gavin.getThings().clear();
		t.commit();
		s.close();
		
		assertEquals(2, gavin.getVersion());
		assertTrue( Hibernate.isInitialized( gavin.getThings() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete(gavin);
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
		gavin = (Person) s.createCriteria(Person.class).uniqueResult();
		new Task("Document", gavin);
		t.commit();
		s.close();
		
		assertEquals(0, gavin.getVersion());
		assertFalse( Hibernate.isInitialized( gavin.getTasks() ) );

		s = openSession();
		t = s.beginTransaction();
		gavin = (Person) s.createCriteria(Person.class).uniqueResult();
		gavin.getTasks().clear();
		t.commit();
		s.close();
		
		assertEquals(0, gavin.getVersion());
		assertTrue( Hibernate.isInitialized( gavin.getTasks() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete(gavin);
		t.commit();
		s.close();
	}

}

