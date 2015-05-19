/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapelemformula;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
public class MapElementFormulaTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "mapelemformula/UserGroup.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
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

