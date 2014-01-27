/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.idbag;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewMetamodel( message = "<idbag> not supported" )
public class IdBagTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idbag/UserGroup.hbm.xml" };
	}

	@Test
	public void testUpdateIdBag() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin");
		Group admins = new Group("admins");
		Group plebs = new Group("plebs");
		Group moderators = new Group("moderators");
		Group banned = new Group("banned");
		gavin.getGroups().add(plebs);
		//gavin.getGroups().add(moderators);
		s.persist(gavin);
		s.persist(plebs);
		s.persist(admins);
		s.persist(moderators);
		s.persist(banned);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.createCriteria(User.class).uniqueResult();
		admins = (Group) s.load(Group.class, "admins");
		plebs = (Group) s.load(Group.class, "plebs");
		banned = (Group) s.load(Group.class, "banned");
		gavin.getGroups().add(admins);
		gavin.getGroups().remove(plebs);
		//gavin.getGroups().add(banned);

		s.delete(plebs);
		s.delete(banned);
		s.delete(moderators);
		s.delete(admins);
		s.delete(gavin);
		
		t.commit();
		s.close();		
	}

	@Test
	public void testJoin() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin");
		Group admins = new Group("admins");
		Group plebs = new Group("plebs");
		gavin.getGroups().add(plebs);
		gavin.getGroups().add(admins);
		s.persist(gavin);
		s.persist(plebs);
		s.persist(admins);
		
		List l = s.createQuery("from User u join u.groups g").list();
		assertEquals( l.size(), 2 );
		s.clear();
		
		gavin = (User) s.createQuery("from User u join fetch u.groups").uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( gavin.getGroups().size(), 2 );
		assertEquals( ( (Group) gavin.getGroups().get(0) ).getName(), "admins" );
		
		s.delete( gavin.getGroups().get(0) );
		s.delete( gavin.getGroups().get(1) );
		s.delete(gavin);
		
		t.commit();
		s.close();		
	}

}

