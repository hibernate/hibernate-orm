/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomanyassociationclass;
import java.util.HashSet;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Abstract class for tests on many-to-many association using an association class.
 *
 * @author Gail Badner
 */
public abstract class AbstractManyToManyAssociationClassTest extends BaseCoreFunctionalTestCase {
	private User user;
	private Group group;
	private Membership membership;

	public abstract Membership createMembership(String name);

	@Override
	protected void prepareTest() {
		Session s = openSession();
		s.beginTransaction();
		user = new User( "user" );
		group = new Group( "group" );
		s.save( user );
		s.save( group );
		membership = createMembership( "membership");
		addMembership( user, group, membership );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void cleanupTest() {
		if ( sessionFactory() != null ) {
			Session s = openSession();
			s.beginTransaction();
			s.createQuery( "delete from " + membership.getClass().getName() );
			s.createQuery( "delete from User" );
			s.createQuery( "delete from Group" );
			s.getTransaction().commit();
			s.close();
		}
	}

	public User getUser() {
		return user;
	}

	public Group getGroup() {
		return group;
	}

	public Membership getMembership() {
		return membership;
	}

	@Test
	public void testRemoveAndAddSameElement() {
		deleteMembership( user, group, membership );
		addMembership( user, group, membership );

		Session s = openSession();
		s.beginTransaction();
		s.merge( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = ( User ) s.get( User.class, user.getId() );
		group = ( Group ) s.get( Group.class, group.getId() );
		membership = ( Membership ) s.get( membership.getClass(), membership.getId() );
		assertEquals( "user", user.getName() );
		assertEquals( "group", group.getName() );
		assertEquals( "membership", membership.getName() );
		assertEquals( 1, user.getMemberships().size() );
		assertEquals( 1, group.getMemberships().size() );
		assertSame( membership, user.getMemberships().iterator().next() );
		assertSame( membership, group.getMemberships().iterator().next() );
		assertSame( user, membership.getUser() );
		assertSame( group, membership.getGroup() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testRemoveAndAddEqualElement() {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		addMembership( user, group, membership );

		Session s = openSession();
		s.beginTransaction();
		s.merge( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = ( User ) s.get( User.class, user.getId() );
		group = ( Group ) s.get( Group.class, group.getId() );
		membership = ( Membership ) s.get( membership.getClass(), membership.getId() );
		assertEquals( "user", user.getName() );
		assertEquals( "group", group.getName() );
		assertEquals( "membership", membership.getName() );
		assertEquals( 1, user.getMemberships().size() );
		assertEquals( 1, group.getMemberships().size() );
		assertSame( membership, user.getMemberships().iterator().next() );
		assertSame( membership, group.getMemberships().iterator().next() );
		assertSame( user, membership.getUser() );
		assertSame( group, membership.getGroup() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testRemoveAndAddEqualCollection() {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		user.setMemberships( new HashSet() );
		group.setMemberships( new HashSet() );
		addMembership( user, group, membership );

		Session s = openSession();
		s.beginTransaction();
		s.merge( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = ( User ) s.get( User.class, user.getId() );
		group = ( Group ) s.get( Group.class, group.getId() );
		membership = ( Membership ) s.get( membership.getClass(), membership.getId() );
		assertEquals( "user", user.getName() );
		assertEquals( "group", group.getName() );
		assertEquals( "membership", membership.getName() );
		assertEquals( 1, user.getMemberships().size() );
		assertEquals( 1, group.getMemberships().size() );
		assertSame( membership, user.getMemberships().iterator().next() );
		assertSame( membership, group.getMemberships().iterator().next() );
		assertSame( user, membership.getUser() );
		assertSame( group, membership.getGroup() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testRemoveAndAddSameElementNonKeyModified() {
		deleteMembership( user, group, membership );
		addMembership( user, group, membership );
		membership.setName( "membership1" );

		Session s = openSession();
		s.beginTransaction();
		s.merge( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = ( User ) s.get( User.class, user.getId() );
		group = ( Group ) s.get( Group.class, group.getId() );
		membership = ( Membership ) s.get( membership.getClass(), membership.getId() );
		assertEquals( "user", user.getName() );
		assertEquals( "group", group.getName() );
		assertEquals( "membership1", membership.getName() );
		assertEquals( 1, user.getMemberships().size() );
		assertEquals( 1, group.getMemberships().size() );
		assertSame( membership, user.getMemberships().iterator().next() );
		assertSame( membership, group.getMemberships().iterator().next() );
		assertSame( user, membership.getUser() );
		assertSame( group, membership.getGroup() );
		s.getTransaction().commit();
		s.close();

	}

	@Test
	public void testRemoveAndAddEqualElementNonKeyModified() {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		addMembership( user, group, membership );
		membership.setName( "membership1" );

		Session s = openSession();
		s.beginTransaction();
		s.merge( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = ( User ) s.get( User.class, user.getId() );
		group = ( Group ) s.get( Group.class, group.getId() );
		membership = ( Membership ) s.get( membership.getClass(), membership.getId() );
		assertEquals( "user", user.getName() );
		assertEquals( "group", group.getName() );
		assertEquals( "membership1", membership.getName() );
		assertEquals( 1, user.getMemberships().size() );
		assertEquals( 1, group.getMemberships().size() );
		assertSame( membership, user.getMemberships().iterator().next() );
		assertSame( membership, group.getMemberships().iterator().next() );
		assertSame( user, membership.getUser() );
		assertSame( group, membership.getGroup() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testDeleteDetached() {
		Session s = openSession();
		s.beginTransaction();
		s.delete( user );
		s.delete( group );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		assertNull( s.get( User.class, user.getId() ) );
		assertNull( s.get( Group.class , group.getId() ) );
		assertNull( s.get( membership.getClass(), membership.getId() ) );
		s.getTransaction().commit();
		s.close();
	}

	public void deleteMembership(User u, Group g, Membership ug) {
		if ( u == null || g == null ) {
			throw new IllegalArgumentException();
		}
		u.getMemberships().remove( ug );
		g.getMemberships().remove( ug );
		ug.setUser( null );
		ug.setGroup( null );
	}

	public void addMembership(User u, Group g, Membership ug) {
		if ( u == null || g == null ) {
			throw new IllegalArgumentException();
		}
		ug.setUser( u );
		ug.setGroup( g );
		u.getMemberships().add( ug );
		g.getMemberships().add( ug );
	}
}
