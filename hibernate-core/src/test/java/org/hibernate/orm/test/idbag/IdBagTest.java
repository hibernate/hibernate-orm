/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.idbag;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
public class IdBagTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHbmMappingFiles() {
		return new String[] { "idbag/UserGroup.hbm.xml" };
	}

	@Test
	public void testUpdateIdBag() {
		Group moderators = new Group( "moderators" );
		inTransaction(
				session -> {
					User gavin = new User( "gavin" );
					Group admins = new Group( "admins" );
					Group plebs = new Group( "plebs" );
					Group banned = new Group( "banned" );
					gavin.getGroups().add( plebs );
					//gavin.getGroups().add(moderators);
					session.persist( gavin );
					session.persist( plebs );
					session.persist( admins );
					session.persist( moderators );
					session.persist( banned );
				}
		);


		inTransaction(
				session -> {
					// todo (6.0) : when criteria will be implementeduse the commented code to retrieve the User
//					User gavin = (User) session.createCriteria( User.class ).uniqueResult();
					User gavin = (User) session.createQuery( "from User" ).uniqueResult();
					Group admins = session.load( Group.class, "admins" );
					Group plebs = session.load( Group.class, "plebs" );
					Group banned = session.load( Group.class, "banned" );
					gavin.getGroups().add( admins );
					gavin.getGroups().remove( plebs );
					//gavin.getGroups().add(banned);

					session.delete( plebs );
					session.delete( banned );
					session.delete( moderators );
					session.delete( admins );
					session.delete( gavin );
				}
		);
	}

	@Test
	@FailureExpected("auto flush before query execution not yet implemented")
	public void testJoin() {
		inTransaction(
				session -> {
					User gavin = new User( "gavin" );
					Group admins = new Group( "admins" );
					Group plebs = new Group( "plebs" );
					gavin.getGroups().add( plebs );
					gavin.getGroups().add( admins );
					session.persist( gavin );
					session.persist( plebs );
					session.persist( admins );

					List l = session.createQuery( "from User u join u.groups g" ).list();
					assertEquals( l.size(), 2 );
					session.clear();

					gavin = (User) session.createQuery( "from User u join fetch u.groups" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( gavin.getGroups().size(), 2 );
					assertEquals( ( (Group) gavin.getGroups().get( 0 ) ).getName(), "admins" );

					session.delete( gavin.getGroups().get( 0 ) );
					session.delete( gavin.getGroups().get( 1 ) );
					session.delete( gavin );
				}
		);
	}

}

