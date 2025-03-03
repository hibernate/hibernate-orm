/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.db;

import java.sql.Timestamp;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;

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

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
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

		assertFalse( "owner version not incremented", JdbcTimestampJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() ) );

		steveTimestamp = steve.getTimestamp();
		Thread.sleep( 1500 );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getGroups().clear();
		t.commit();
		s.close();

		assertFalse( "owner version not incremented", JdbcTimestampJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.remove( s.getReference( User.class, steve.getId() ) );
		s.remove( s.getReference( Group.class, admin.getId() ) );
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
		if ( getDialect() instanceof SybaseDialect ) {
			// Sybase has 1/300th sec precision, but not for the `getdate()` function which we use for DB generation
			steveTimestamp = new Timestamp( steveTimestamp.getTime() );
		}

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		perm = ( Permission ) s.get( Permission.class, perm.getId() );
		steve.getPermissions().add( perm );
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", JdbcTimestampJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		steve = ( User ) s.get( User.class, steve.getId() );
		steve.getPermissions().clear();
		t.commit();
		s.close();

		assertTrue( "owner version was incremented", JdbcTimestampJavaType.INSTANCE.areEqual( steveTimestamp, steve.getTimestamp() ) );

		s = openSession();
		t = s.beginTransaction();
		s.remove( s.getReference( User.class, steve.getId() ) );
		s.remove( s.getReference( Permission.class, perm.getId() ) );
		t.commit();
		s.close();
	}
}
