/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class ManyToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomany/UserGroup.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	@Test
	public void testManyToManyWithFormula() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "jboss");
		Group seam = new Group("seam", "jboss");
		Group hb = new Group("hibernate", "jboss");
		gavin.getGroups().add(seam);
		gavin.getGroups().add(hb);
		seam.getUsers().add(gavin);
		hb.getUsers().add(gavin);
		s.persist(gavin);
		s.persist(seam);
		s.persist(hb);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.get(User.class, gavin);
		assertFalse( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( 2, gavin.getGroups().size() );
		hb = (Group) s.get(Group.class, hb);
		assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
		assertEquals( 1, hb.getUsers().size() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.createCriteria(User.class)
			.setFetchMode("groups", FetchMode.JOIN)
			.uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( 2, gavin.getGroups().size() );
		Group group = (Group) gavin.getGroups().iterator().next();
		assertFalse( Hibernate.isInitialized( group.getUsers() ) );
		assertEquals( 1, group.getUsers().size() );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.createCriteria(User.class)
			.setFetchMode("groups", FetchMode.JOIN)
			.setFetchMode("groups.users", FetchMode.JOIN)
			.uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( 2, gavin.getGroups().size() );
		group = (Group) gavin.getGroups().iterator().next();
		assertTrue( Hibernate.isInitialized( group.getUsers() ) );
		assertEquals( 1, group.getUsers().size() );
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.createQuery("from User u join fetch u.groups g join fetch g.users").uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( 2, gavin.getGroups().size() );
		group = (Group) gavin.getGroups().iterator().next();
		assertTrue( Hibernate.isInitialized( group.getUsers() ) );
		assertEquals( 1, group.getUsers().size() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.get(User.class, gavin);
		hb = (Group) s.get(Group.class, hb);
		gavin.getGroups().remove(hb);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.get(User.class, gavin);
		assertEquals( gavin.getGroups().size(), 1 );
		hb = (Group) s.get(Group.class, hb);
		assertEquals( hb.getUsers().size(), 0 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete(gavin);
		s.flush();
		s.createQuery("delete from Group").executeUpdate();
		t.commit();
		s.close();
	}
}

