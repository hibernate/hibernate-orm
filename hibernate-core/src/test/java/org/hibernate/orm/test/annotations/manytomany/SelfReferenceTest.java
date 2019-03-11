/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class SelfReferenceTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testSelf() {
		Friend friend = new Friend();
		Friend sndF = new Friend();
		inTransaction(
				session -> {
					friend.setName( "Starsky" );
					sndF.setName( "Hutch" );
					Set frnds = new HashSet();
					frnds.add( sndF );
					friend.setFriends( frnds );
					//Starsky is a friend of Hutch but hutch is not
					session.persist( friend );
				}
		);

		inTransaction(
				session -> {
					Friend f = session.load( Friend.class, friend.getId() );
					assertNotNull( f );
					assertNotNull( f.getFriends() );
					assertEquals( 1, f.getFriends().size() );
					Friend fromDb2ndFrnd = f.getFriends().iterator().next();
					assertEquals( fromDb2ndFrnd.getId(), sndF.getId() );
					assertEquals( 0, fromDb2ndFrnd.getFriends().size() );
				}
		);
	}


	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	protected void cleanupTestData() {
		inTransaction(
				session -> {
					List<Friend> friends = session.createQuery( "from Friend" ).list();
					friends.forEach( friend -> session.delete( friend ) );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Friend.class
		};
	}

}
