/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomanyassociationclass;

import java.util.HashSet;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Abstract class for tests on many-to-many association using an association class.
 *
 * @author Gail Badner
 */
@SessionFactory
public abstract class AbstractManyToManyAssociationClassTest {
	private User user;
	private Group group;
	private Membership membership;

	public abstract Membership createMembership(String name);

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					user = new User( "user" );
					group = new Group( "group" );
					session.persist( user );
					session.persist( group );
					membership = createMembership( "membership" );
					addMembership( user, group, membership );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
	public void testRemoveAndAddSameElement(SessionFactoryScope scope) {
		deleteMembership( user, group, membership );
		addMembership( user, group, membership );

		scope.inTransaction(
				session ->
						session.merge( user )
		);

		scope.inTransaction(
				session -> {
					user = session.get( User.class, user.getId() );
					group = session.get( Group.class, group.getId() );
					membership = session.get( membership.getClass(), membership.getId() );
					assertEquals( "user", user.getName() );
					assertEquals( "group", group.getName() );
					assertEquals( "membership", membership.getName() );
					assertEquals( 1, user.getMemberships().size() );
					assertEquals( 1, group.getMemberships().size() );
					assertSame( membership, user.getMemberships().iterator().next() );
					assertSame( membership, group.getMemberships().iterator().next() );
					assertSame( user, membership.getUser() );
					assertSame( group, membership.getGroup() );
				}
		);
	}

	@Test
	public void testRemoveAndAddEqualElement(SessionFactoryScope scope) {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		addMembership( user, group, membership );

		scope.inTransaction(
				session ->
						session.merge( user )
		);

		scope.inTransaction(
				session -> {
					user = session.get( User.class, user.getId() );
					group = session.get( Group.class, group.getId() );
					membership = session.get( membership.getClass(), membership.getId() );
					assertEquals( "user", user.getName() );
					assertEquals( "group", group.getName() );
					assertEquals( "membership", membership.getName() );
					assertEquals( 1, user.getMemberships().size() );
					assertEquals( 1, group.getMemberships().size() );
					assertSame( membership, user.getMemberships().iterator().next() );
					assertSame( membership, group.getMemberships().iterator().next() );
					assertSame( user, membership.getUser() );
					assertSame( group, membership.getGroup() );
				}
		);
	}

	@Test
	public void testRemoveAndAddEqualCollection(SessionFactoryScope scope) {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		user.setMemberships( new HashSet<>() );
		group.setMemberships( new HashSet<>() );
		addMembership( user, group, membership );

		scope.inTransaction(
				session ->
						session.merge( user )
		);

		scope.inTransaction(
				session -> {
					user = session.get( User.class, user.getId() );
					group = session.get( Group.class, group.getId() );
					membership = session.get( membership.getClass(), membership.getId() );
					assertEquals( "user", user.getName() );
					assertEquals( "group", group.getName() );
					assertEquals( "membership", membership.getName() );
					assertEquals( 1, user.getMemberships().size() );
					assertEquals( 1, group.getMemberships().size() );
					assertSame( membership, user.getMemberships().iterator().next() );
					assertSame( membership, group.getMemberships().iterator().next() );
					assertSame( user, membership.getUser() );
					assertSame( group, membership.getGroup() );
				}
		);
	}

	@Test
	public void testRemoveAndAddSameElementNonKeyModified(SessionFactoryScope scope) {
		deleteMembership( user, group, membership );
		addMembership( user, group, membership );
		membership.setName( "membership1" );

		scope.inTransaction(
				session ->
						session.merge( user )
		);

		scope.inTransaction(
				session -> {
					user = session.get( User.class, user.getId() );
					group = session.get( Group.class, group.getId() );
					membership = session.get( membership.getClass(), membership.getId() );
					assertEquals( "user", user.getName() );
					assertEquals( "group", group.getName() );
					assertEquals( "membership1", membership.getName() );
					assertEquals( 1, user.getMemberships().size() );
					assertEquals( 1, group.getMemberships().size() );
					assertSame( membership, user.getMemberships().iterator().next() );
					assertSame( membership, group.getMemberships().iterator().next() );
					assertSame( user, membership.getUser() );
					assertSame( group, membership.getGroup() );
				}
		);
	}

	@Test
	public void testRemoveAndAddEqualElementNonKeyModified(SessionFactoryScope scope) {
		deleteMembership( user, group, membership );
		membership = createMembership( "membership" );
		addMembership( user, group, membership );
		membership.setName( "membership1" );

		scope.inTransaction(
				session ->
						session.merge( user )
		);

		scope.inTransaction(
				session -> {
					user = session.get( User.class, user.getId() );
					group = session.get( Group.class, group.getId() );
					membership = session.get( membership.getClass(), membership.getId() );
					assertEquals( "user", user.getName() );
					assertEquals( "group", group.getName() );
					assertEquals( "membership1", membership.getName() );
					assertEquals( 1, user.getMemberships().size() );
					assertEquals( 1, group.getMemberships().size() );
					assertSame( membership, user.getMemberships().iterator().next() );
					assertSame( membership, group.getMemberships().iterator().next() );
					assertSame( user, membership.getUser() );
					assertSame( group, membership.getGroup() );
				}
		);
	}

	@Test
	public void testDeleteDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.remove( user );
					session.remove( group );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( User.class, user.getId() ) );
					assertNull( session.get( Group.class, group.getId() ) );
					assertNull( session.get( membership.getClass(), membership.getId() ) );
				}
		);
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
