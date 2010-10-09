// $Id: DbVersionTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.version.db;

import java.sql.Timestamp;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of DbVersionTest.
 *
 * @author Steve Ebersole
 */
public class DbVersionTest extends FunctionalTestCase {
	public DbVersionTest(String x) {
		super( x );
	}

	
	public String[] getMappings() {
		return new String[] { "version/db/User.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DbVersionTest.class );
	}

	public void testCollectionVersion() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Group admin = new Group( "admin" );
		s.persist( admin );
		t.commit();
		s.close();

		Timestamp steveTimestamp = steve.getTimestamp();

		// For dialects (Oracle8 for example) which do not return "true
		// timestamps" sleep for a bit to allow the db date-time increment...
		Thread.sleep( 1500 );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		admin = ( Group ) s.get( Group.class, admin.getId() );
		steve.getGroups().add( admin );
		admin.getUsers().add( steve );
		t.commit();
		s.close();

		assertFalse( "owner version not incremented", Hibernate.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		steveTimestamp = steve.getTimestamp();
		Thread.sleep( 1500 );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getGroups().clear();
		t.commit();
		s.close();

		assertFalse( "owner version not incremented", Hibernate.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Group.class, admin.getId() ) );
		t.commit();
		s.close();
	}


	public void testCollectionNoVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Permission perm = new Permission( "silly", "user", "rw" );
		s.persist( perm );
		t.commit();
		s.close();

		Timestamp steveTimestamp = ( Timestamp ) steve.getTimestamp();

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		perm = ( Permission ) s.get( Permission.class, perm.getId() );
		steve.getPermissions().add( perm );
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", Hibernate.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getPermissions().clear();
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", Hibernate.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Permission.class, perm.getId() ) );
		t.commit();
		s.close();
	}
}