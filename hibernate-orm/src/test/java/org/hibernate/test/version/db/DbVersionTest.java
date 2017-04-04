/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.version.db;

import java.sql.Timestamp;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.StandardBasicTypes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class DbVersionTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "version/db/User.hbm.xml" };
	}

	@Test
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

		assertFalse( "owner version not incremented", StandardBasicTypes.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		steveTimestamp = steve.getTimestamp();
		Thread.sleep( 1500 );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getGroups().clear();
		t.commit();
		s.close();

		assertFalse( "owner version not incremented", StandardBasicTypes.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Group.class, admin.getId() ) );
		t.commit();
		s.close();
	}

	@Test
	public void testCollectionNoVersion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User steve = new User( "steve" );
		s.persist( steve );
		Permission perm = new Permission( "silly", "user", "rw" );
		s.persist( perm );
		t.commit();
		s.close();

		Timestamp steveTimestamp = steve.getTimestamp();

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		perm = ( Permission ) s.get( Permission.class, perm.getId() );
		steve.getPermissions().add( perm );
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", StandardBasicTypes.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getPermissions().clear();
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", StandardBasicTypes.TIMESTAMP.isEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.delete( s.load( User.class, steve.getId() ) );
		s.delete( s.load( Permission.class, perm.getId() ) );
		t.commit();
		s.close();
	}
}
