/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.db;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/version/db/User.hbm.xml")
@SessionFactory
public class DbVersionTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testCollectionVersion(SessionFactoryScope factoryScope) throws Exception {
		final var tsJavaType = JdbcTimestampJavaType.INSTANCE;
		final MutableObject<User> steveRef = new MutableObject<>();

		factoryScope.inTransaction( (session) -> {
			var steve = new User( 1, "steve" );
			session.persist( steve );
			var admin = new Group( 1, "admin" );
			session.persist( admin );

			steveRef.set( steve );
		} );

		Timestamp steveTimestamp = steveRef.get().getTimestamp();

		// For dialects (Oracle8 for example) which do not return "true
		// timestamps" sleep for a bit to allow the db date-time increment...
		Thread.sleep( 1500 );

		factoryScope.inTransaction( (session) -> {
			var steve = session.find( User.class, 1 );
			var admin = session.find( Group.class, 1 );
			steve.getGroups().add( admin );
			admin.getUsers().add( steve );
			steveRef.set( steve );
		} );

		Assertions.assertFalse( tsJavaType.areEqual( steveTimestamp, steveRef.get().getTimestamp() ),
				"owner version not incremented" );

		steveTimestamp = steveRef.get().getTimestamp();
		Thread.sleep( 1500 );

		factoryScope.inTransaction( (session) -> {
			var steve = session.find( User.class, 1 );
			steve.getGroups().clear();
			steveRef.set( steve );
		} );

		Assertions.assertFalse( tsJavaType.areEqual( steveTimestamp, steveRef.get().getTimestamp() ),
				"owner version not incremented" );
	}

	@Test
	public void testCollectionNoVersion(SessionFactoryScope factoryScope) throws Exception {
		final var dialect = factoryScope.getSessionFactory().getJdbcServices().getDialect();
		final var tsJavaType = JdbcTimestampJavaType.INSTANCE;
		final MutableObject<User> steveRef = new MutableObject<>();

		factoryScope.inTransaction( (session) -> {
			var steve = new User( 1, "steve" );
			session.persist( steve );
			var perm = new Permission( 1, "silly", "user", "rw" );
			session.persist( perm );

			steveRef.set( steve );
		} );

		var steveTimestamp = determineTimestamp( steveRef, dialect );

		factoryScope.inTransaction( (session) -> {
			var steve = session.find( User.class, 1 );
			var perm = session.find( Permission.class, 1 );
			steve.getPermissions().add( perm );

			steveRef.set( steve );
		} );

		Assertions.assertTrue( tsJavaType.areEqual( steveTimestamp, steveRef.get().getTimestamp() ),
				"owner version was incremented" );

		steveTimestamp = determineTimestamp( steveRef, dialect );

		factoryScope.inTransaction( (session) -> {
			var steve = session.find( User.class, 1 );
			steve.getPermissions().clear();

			steveRef.set( steve );
		} );

		Assertions.assertTrue( tsJavaType.areEqual( steveTimestamp, steveRef.get().getTimestamp() ),
				"owner version was incremented" );
	}

	private Timestamp determineTimestamp(MutableObject<User> steveRef, Dialect dialect) {
		var timestamp = steveRef.get().getTimestamp();

		if ( dialect instanceof SybaseDialect ) {
			// Sybase has 1/300th sec precision, but not for the `getdate()` function which we use for DB generation
			timestamp = new Timestamp( timestamp.getTime() );
		}

		return timestamp;
	}
}
