/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.flush;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.orm.transaction.TransactionUtil.inTransaction;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-15045")
public class DirtyFlushTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Profile.class };
	}

	@Before
	public void setUp() {
		inTransaction( getOrCreateEntityManager(), em -> {
			final User user = new User();
			user.id = 1;
			user.version = 1;

			final Profile profile = new Profile();
			profile.id = 1;
			profile.version = 1;

			em.persist( user );
			em.persist( profile );
		} );
	}

	@Test
	public void testDirtyFlushNotHappened() {
		inTransaction( getOrCreateEntityManager(), em -> {
			final User user = em.find( User.class, 1 );
			assertEquals( 1, user.version );

			final Profile profile = em.find( Profile.class, 1 );
			assertEquals( 1, profile.version );

			profile.user = user;
			user.profile = profile;

			em.persist( profile );
		} );

		inTransaction( getOrCreateEntityManager(), em -> {
			final Profile profile = em.find( Profile.class, 1 );
			assertEquals( 2, profile.version );

			final User user = em.find( User.class, 1 );
			assertEquals(
					"without fixing, the version will be bumped due to erroneous dirty flushing",
					1,
					user.version
			);
		} );
	}

	@After
	public void tearDown() {
		inTransaction( getOrCreateEntityManager(), em -> {
			em.createQuery( "delete from Profile" ).executeUpdate();
			em.createQuery( "delete from User" ).executeUpdate();
		} );
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User {
		@Id
		int id;

		@Version
		int version;

		@OneToOne(mappedBy = "user")
		Profile profile;
	}

	@Entity(name = "Profile")
	@Table(name = "PROFILE_TABLE")
	public static class Profile {
		@Id
		int id;

		@Version
		int version;
		@OneToOne // internally Hibernate will use `@ManyToOne` for this field
		User user;
	}
}
