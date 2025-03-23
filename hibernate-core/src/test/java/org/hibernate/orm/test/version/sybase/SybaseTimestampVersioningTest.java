/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;

import jakarta.persistence.OptimisticLockException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Implementation of VersionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect( SybaseASEDialect.class )
public class SybaseTimestampVersioningTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "version/sybase/User.hbm.xml" };
	}

	@Test
	public void testLocking() {
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

			User user1 = s1.get( User.class, steve.getId() );
			User user2 = s2.get( User.class, steve.getId() );

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
		s.remove( s.getReference( User.class, steve.getId() ) );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCollectionVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Group admin = new Group( "admin" );
		s.persist( admin );
		t.commit();
		s.close();

		byte[] steveTimestamp = steve.getTimestamp();

		sleep();

		s = openSession();
		t = s.beginTransaction();
		steve = s.get( User.class, steve.getId() );
		admin = s.get( Group.class, admin.getId() );
		steve.getGroups().add( admin );
		admin.getUsers().add( steve );
		t.commit();
		s.close();

		// Hibernate used to increment the version here,
		// when the collections changed, but now doesn't
		// that's OK, because the only reason this worked
		// in H5 was due to a bug (it used to go and ask
		// for getdate() from the database, even though
		// it wasn't planning on doing anything with it,
		// and then issue a spurious 'update' statement)
//		assertFalse(
//				"owner version not incremented",
//				PrimitiveByteArrayJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() )
//		);

		steveTimestamp = steve.getTimestamp();

		sleep();

		s = openSession();
		t = s.beginTransaction();
		steve = s.get( User.class, steve.getId() );
		steve.getGroups().clear();
		t.commit();
		s.close();

		// Hibernate used to increment the version here,
		// when the collections changed, but now doesn't
		// that's OK, because the only reason this worked
		// in H5 was due to a bug (it used to go and ask
		// for getdate() from the database, even though
		// it wasn't planning on doing anything with it,
		// and then issue a spurious 'update' statement)
// 		assertFalse(
//				"owner version not incremented",
//				PrimitiveByteArrayJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() )
//		);

		sleep();

		s = openSession();
		t = s.beginTransaction();
		s.remove( s.getReference( User.class, steve.getId() ) );
		s.remove( s.getReference( Group.class, admin.getId() ) );
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

		sleep();

		s = openSession();
		t = s.beginTransaction();
		steve = s.get( User.class, steve.getId() );
		perm = s.get( Permission.class, perm.getId() );
		steve.getPermissions().add( perm );
		t.commit();
		s.close();

		assertTrue(
				"owner version was incremented",
				PrimitiveByteArrayJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() )
		);

		sleep();

		s = openSession();
		t = s.beginTransaction();
		steve = s.get( User.class, steve.getId() );
		steve.getPermissions().clear();
		t.commit();
		s.close();

		assertTrue(
				"owner version was incremented",
				PrimitiveByteArrayJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() )
		);

		sleep();

		s = openSession();
		t = s.beginTransaction();
		s.remove( s.getReference( User.class, steve.getId() ) );
		s.remove( s.getReference( Permission.class, perm.getId() ) );
		t.commit();
		s.close();
	}

	private static void sleep() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException ignored) {
		}
	}

	@Test
	@JiraKey( value = "HHH-10413" )
	public void testComparableTimestamps() {
		final BasicType<?> versionType = sessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(User.class.getName())
				.getVersionType();
		assertTrue( versionType.getJavaTypeDescriptor() instanceof PrimitiveByteArrayJavaType );
		assertTrue( versionType.getJdbcType() instanceof VarbinaryJdbcType );

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
			user = s.merge( user );
			s.getTransaction().commit();
			s.close();

			assertTrue( versionType.compare( previousTimestamp, user.getTimestamp() ) < 0 );
			previousTimestamp = user.getTimestamp();
		}

		s = openSession();
		s.getTransaction().begin();
		s.remove( user );
		s.getTransaction().commit();
		s.close();
	}
}
