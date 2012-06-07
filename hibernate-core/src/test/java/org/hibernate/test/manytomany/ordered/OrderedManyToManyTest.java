/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.manytomany.ordered;
import java.util.List;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class OrderedManyToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomany/ordered/UserGroup.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	@Test
	public void testManyToManyOrdering() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User( "gavin", "jboss" );
		User steve = new User( "steve", "jboss" );
		User max = new User( "max", "jboss" );
		User emmanuel = new User( "emmanuel", "jboss" );
		s.persist( gavin );
		s.persist( steve );
		s.persist( max );
		s.persist( emmanuel );
		Group hibernate = new Group( "hibernate", "jboss" );
		hibernate.addUser( gavin );
		hibernate.addUser( steve );
		hibernate.addUser( max );
		hibernate.addUser( emmanuel );
		s.persist( hibernate );
		t.commit();
		s.close();

		// delayed collection load...
		s = openSession();
		t = s.beginTransaction();
		hibernate = ( Group ) s.get( Group.class, hibernate.getId() );
		assertFalse( Hibernate.isInitialized( hibernate.getUsers() ) );
		assertEquals( 4, hibernate.getUsers().size() );
		assertOrdering( hibernate.getUsers() );
		t.commit();
		s.close();

		// HQL (non eager)
		s = openSession();
		t = s.beginTransaction();
		hibernate = ( Group ) s.createQuery( "from Group" ).uniqueResult();
		assertFalse( Hibernate.isInitialized( hibernate.getUsers() ) );
		assertEquals( 4, hibernate.getUsers().size() );
		assertOrdering( hibernate.getUsers() );
		t.commit();
		s.close();

		// HQL (eager)
		s = openSession();
		t = s.beginTransaction();
		hibernate = ( Group ) s.createQuery( "from Group g inner join fetch g.users" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( hibernate.getUsers() ) );
		assertEquals( 4, hibernate.getUsers().size() );
		assertOrdering( hibernate.getUsers() );
		t.commit();
		s.close();

		// criteria load (forced eager fetch)
		s = openSession();
		t = s.beginTransaction();
		Criteria criteria = s.createCriteria( Group.class );
		criteria.setFetchMode( "users", FetchMode.JOIN );
		hibernate = ( Group ) criteria.uniqueResult();
		assertTrue( Hibernate.isInitialized( hibernate.getUsers() ) );
		assertEquals( 4, hibernate.getUsers().size() );
		assertOrdering( hibernate.getUsers() );
		t.commit();
		s.close();

		// criteria load (forced non eager fetch)
		s = openSession();
		t = s.beginTransaction();
		criteria = s.createCriteria( Group.class );
		criteria.setFetchMode( "users", FetchMode.SELECT );
		hibernate = ( Group ) criteria.uniqueResult();
		assertFalse( Hibernate.isInitialized( hibernate.getUsers() ) );
		assertEquals( 4, hibernate.getUsers().size() );
		assertOrdering( hibernate.getUsers() );
		t.commit();
		s.close();

		// clean up
		s = openSession();
		t = s.beginTransaction();
		s.delete( gavin );
		s.delete( steve );
		s.delete( max );
		s.delete( emmanuel );
		s.delete( hibernate );
		t.commit();
		s.close();
	}

	private void assertOrdering(List users) {
		User user = extractUser( users, 0 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "emmanuel" ) );
		user = extractUser( users, 1 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "gavin" ) );
		user = extractUser( users, 2 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "max" ) );
		user = extractUser( users, 3 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "steve" ) );
	}

	private User extractUser(List users, int position) {
		return ( User ) users.get( position );
	}

}
