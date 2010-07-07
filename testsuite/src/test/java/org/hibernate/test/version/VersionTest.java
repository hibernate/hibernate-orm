//$Id: VersionTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.version;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Max Rydahl Andersen
 */
public class VersionTest extends FunctionalTestCase {
	
	public VersionTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "version/PersonThing.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( VersionTest.class );
	}

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

