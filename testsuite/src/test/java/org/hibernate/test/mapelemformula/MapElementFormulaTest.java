//$Id: MapElementFormulaTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.mapelemformula;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class MapElementFormulaTest extends FunctionalTestCase {
	
	public MapElementFormulaTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "mapelemformula/UserGroup.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MapElementFormulaTest.class );
	}

	public void testManyToManyFormula() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("users");
		g.getUsers().put("Gavin", gavin);
		g.getUsers().put("Turin", turin);
		s.persist(g);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 2 );
		g.getUsers().remove("Turin");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 1 );
		s.delete(g);
		s.delete( g.getUsers().get("Gavin") );
		s.delete( s.get(User.class, "turin") );
		t.commit();
		s.close();
	}

}

