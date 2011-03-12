//$Id: ExtraLazyTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.extralazy;

import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class ExtraLazyTest extends FunctionalTestCase {

	public ExtraLazyTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "extralazy/UserGroup.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ExtraLazyTest.class );
	}

	public void testOrphanDelete() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		Document hia = new Document("HiA", "blah blah blah", gavin);
		Document hia2 = new Document("HiA2", "blah blah blah blah", gavin);
		s.persist(gavin);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.get(User.class, "gavin");
		assertEquals( 2, gavin.getDocuments().size() );
		gavin.getDocuments().remove(hia2);
		assertFalse( gavin.getDocuments().contains(hia2) );
		assertTrue( gavin.getDocuments().contains(hia) );
		assertEquals( 1, gavin.getDocuments().size() );
		assertFalse( Hibernate.isInitialized( gavin.getDocuments() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.get(User.class, "gavin");
		assertEquals( 1, gavin.getDocuments().size() );
		assertFalse( gavin.getDocuments().contains(hia2) );
		assertTrue( gavin.getDocuments().contains(hia) );
		assertFalse( Hibernate.isInitialized( gavin.getDocuments() ) );
		assertNull( s.get(Document.class, "HiA2") );
		gavin.getDocuments().clear();
		assertTrue( Hibernate.isInitialized( gavin.getDocuments() ) );
		s.delete(gavin);
		t.commit();
		s.close();
	}
	
	public void testGet() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("developers");
		g.getUsers().put("gavin", gavin);
		g.getUsers().put("turin", turin);
		s.persist(g);
		gavin.getSession().put( "foo", new SessionAttribute("foo", "foo bar baz") );
		gavin.getSession().put( "bar", new SessionAttribute("bar", "foo bar baz 2") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		gavin = (User) g.getUsers().get("gavin");
		turin = (User) g.getUsers().get("turin");
		assertNotNull(gavin);
		assertNotNull(turin);
		assertNull( g.getUsers().get("emmanuel") );
		assertFalse( Hibernate.isInitialized( g.getUsers() ) );
		assertNotNull( gavin.getSession().get("foo") );
		assertNull( turin.getSession().get("foo") );
		assertFalse( Hibernate.isInitialized( gavin.getSession() ) );
		assertFalse( Hibernate.isInitialized( turin.getSession() ) );
		s.delete(gavin);
		s.delete(turin);
		s.delete(g);
		t.commit();
		s.close();
	}
	
	public void testRemoveClear() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("developers");
		g.getUsers().put("gavin", gavin);
		g.getUsers().put("turin", turin);
		s.persist(g);
		gavin.getSession().put( "foo", new SessionAttribute("foo", "foo bar baz") );
		gavin.getSession().put( "bar", new SessionAttribute("bar", "foo bar baz 2") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		gavin = (User) g.getUsers().get("gavin");
		turin = (User) g.getUsers().get("turin");
		assertFalse( Hibernate.isInitialized( g.getUsers() ) );
		g.getUsers().clear();
		gavin.getSession().remove("foo");
		assertTrue( Hibernate.isInitialized( g.getUsers() ) );
		assertTrue( Hibernate.isInitialized( gavin.getSession() ) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		assertTrue( g.getUsers().isEmpty() );
		assertFalse( Hibernate.isInitialized( g.getUsers() ) );
		gavin = (User) s.get(User.class, "gavin");
		assertFalse( gavin.getSession().containsKey("foo") );
		assertFalse( Hibernate.isInitialized( gavin.getSession() ) );
		s.delete(gavin);
		s.delete(turin);
		s.delete(g);
		t.commit();
		s.close();
	}
	
	public void testIndexFormulaMap() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("developers");
		g.getUsers().put("gavin", gavin);
		g.getUsers().put("turin", turin);
		s.persist(g);
		gavin.getSession().put( "foo", new SessionAttribute("foo", "foo bar baz") );
		gavin.getSession().put( "bar", new SessionAttribute("bar", "foo bar baz 2") );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		assertEquals( g.getUsers().size(), 2 );
		g.getUsers().remove("turin");
		Map smap = ( (User) g.getUsers().get("gavin") ).getSession();
		assertEquals(smap.size(), 2);
		smap.remove("bar");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		assertEquals( g.getUsers().size(), 1 );
		smap = ( (User) g.getUsers().get("gavin") ).getSession();
		assertEquals(smap.size(), 1);
		gavin = (User) g.getUsers().put("gavin", turin);
		s.delete(gavin);
		assertEquals( s.createQuery("select count(*) from SessionAttribute").uniqueResult(), new Long(0) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "developers");
		assertEquals( g.getUsers().size(), 1 );
		turin = (User) g.getUsers().get("turin");
		smap = turin.getSession();
		assertEquals(smap.size(), 0);
		assertEquals( s.createQuery("select count(*) from User").uniqueResult(), new Long(1) );
		s.delete(g);
		s.delete(turin);
		assertEquals( s.createQuery("select count(*) from User").uniqueResult(), new Long(0) );
		t.commit();
		s.close();
	}
	
	public void testSQLQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		gavin.getSession().put( "foo", new SessionAttribute("foo", "foo bar baz") );
		gavin.getSession().put( "bar", new SessionAttribute("bar", "foo bar baz 2") );
		s.persist(gavin);
		s.persist(turin);
		s.flush();
		s.clear();
		List results = s.getNamedQuery("userSessionData").setParameter("uname", "%in").list();
		assertEquals( results.size(), 2 );
		gavin = (User) ( (Object[]) results.get(0) )[0];
		assertEquals( gavin.getName(), "gavin" );
		assertEquals( gavin.getSession().size(), 2 );
		s.createQuery("delete SessionAttribute").executeUpdate();
		s.createQuery("delete User").executeUpdate();
		t.commit();
		s.close();
		
	}

}

