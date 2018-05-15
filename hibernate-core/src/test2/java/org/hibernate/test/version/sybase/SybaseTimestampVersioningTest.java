/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.version.sybase;

import javax.persistence.OptimisticLockException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.BinaryType;
import org.hibernate.type.RowVersionType;
import org.hibernate.type.VersionType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Implementation of VersionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect( SybaseASE15Dialect.class )
public class SybaseTimestampVersioningTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "version/sybase/User.hbm.xml" };
	}

	@Test
	public void testLocking() throws Throwable {
		// First, create the needed row...
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		t.commit();
		s.close();

		// next open two sessions, and try to update from each "simultaneously"...
		Session s1 = null;
		Session s2 = null;
		Transaction t1 = null;
		Transaction t2 = null;
		try {
			s1 = sessionFactory().openSession();
			t1 = s1.beginTransaction();
			s2 = sessionFactory().openSession();
			t2 = s2.beginTransaction();

			User user1 = ( User ) s1.get( User.class, steve.getId() );
			User user2 = ( User ) s2.get( User.class, steve.getId() );

			user1.setUsername( "se" );
			t1.commit();
			t1 = null;

			user2.setUsername( "steve-e" );
			try {
				t2.commit();
				fail( "optimistic lock check did not fail" );
			}
			catch( OptimisticLockException e ) {
				// expected...
				try {
					t2.rollback();
				}
				catch( Throwable ignore ) {
				}
			}
		}
		catch( Throwable error ) {
			if ( t1 != null ) {
				try {
					t1.rollback();
				}
				catch( Throwable ignore ) {
				}
			}
			if ( t2 != null ) {
				try {
					t2.rollback();
				}
				catch( Throwable ignore ) {
				}
			}
			throw error;
		}
		finally {
			if ( s1 != null ) {
				try {
					s1.close();
				}
				catch( Throwable ignore ) {
				}
			}
			if ( s2 != null ) {
				try {
					s2.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}

		// lastly, clean up...
		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCollectionVersion() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Group admin = new Group( "admin" );
		s.persist( admin );
		t.commit();
		s.close();

		byte[] steveTimestamp = steve.getTimestamp();

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		admin = ( Group ) s.get( Group.class, admin.getId() );
		steve.getGroups().add( admin );
		admin.getUsers().add( steve );
		t.commit();
		s.close();

		assertFalse(
				"owner version not incremented", BinaryType.INSTANCE.isEqual(
				steveTimestamp, steve.getTimestamp()
		)
		);

		steveTimestamp = steve.getTimestamp();

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getGroups().clear();
		t.commit();
		s.close();

		assertFalse(
				"owner version not incremented", BinaryType.INSTANCE.isEqual(
				steveTimestamp, steve.getTimestamp()
		)
		);

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Group.class, admin.getId() ) );
		t.commit();
		s.close();
	}


	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCollectionNoVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Permission perm = new Permission( "silly", "user", "rw" );
		s.persist( perm );
		t.commit();
		s.close();

		byte[] steveTimestamp = steve.getTimestamp();

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		perm = ( Permission ) s.get( Permission.class, perm.getId() );
		steve.getPermissions().add( perm );
		t.commit();
		s.close();

		assertTrue(
				"owner version was incremented", BinaryType.INSTANCE.isEqual(
				steveTimestamp, steve.getTimestamp()
		)
		);

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getPermissions().clear();
		t.commit();
		s.close();

		assertTrue(
				"owner version was incremented", BinaryType.INSTANCE.isEqual(
				steveTimestamp, steve.getTimestamp()
		)
		);

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Permission.class, perm.getId() ) );
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10413" )
	public void testComparableTimestamps() {
		final VersionType versionType =
				sessionFactory().getEntityPersister( User.class.getName() ).getVersionType();
		assertSame( RowVersionType.INSTANCE, versionType );

		Session s = openSession();
		s.getTransaction().begin();
		User user = new User();
		user.setUsername( "n" );
		s.persist( user );
		s.getTransaction().commit();
		s.close();

		byte[] previousTimestamp = user.getTimestamp();
		for ( int i = 0 ; i < 20 ; i++ ) {
			try {
				Thread.sleep(1000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			s = openSession();
			s.getTransaction().begin();
			user.setUsername( "n" + i );
			user = (User) s.merge( user );
			s.getTransaction().commit();
			s.close();

			assertTrue( versionType.compare( previousTimestamp, user.getTimestamp() ) < 0 );
			previousTimestamp = user.getTimestamp();
		}

		s = openSession();
		s.getTransaction().begin();
		s.delete( user );
		s.getTransaction().commit();
		s.close();
	}
}
