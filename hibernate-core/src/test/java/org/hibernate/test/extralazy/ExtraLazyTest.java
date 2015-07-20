/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.extralazy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
public class ExtraLazyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "extralazy/UserGroup.hbm.xml","extralazy/Parent.hbm.xml","extralazy/Child.hbm.xml" };
	}

	@Test
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
	
	@Test
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
	
	@Test
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
	
	@Test
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
	
	@Test
	@RequiresDialectFeature( DialectChecks.DoubleQuoteQuoting.class )
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

	@Test
	@TestForIssue(jiraKey="HHH-4294")
	public void testMap() {
		Session session1 = openSession();
		Transaction tx1 = session1.beginTransaction();
		Parent parent = new Parent ();		
		Child child = new Child ();
		child.setFirstName("Ben");
		parent.getChildren().put(child.getFirstName(), child);
		child.setParent(parent);		
		session1.save(parent);
		tx1.commit();
		session1.close();
		// END PREPARE SECTION
		
		Session session2 = openSession();
		Parent parent2 = (Parent)session2.get(Parent.class, parent.getId());
		Child child2 = parent2.getChildren().get(child.getFirstName()); // causes SQLGrammarException because of wrong condition: 	where child0_.PARENT_ID=? and child0_.null=?
		assertNotNull(child2);
		session2.close();
	}
}

