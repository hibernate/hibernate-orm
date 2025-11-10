/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// @author Steve Ebersole
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(SybaseASEDialect.class)
@DomainModel(xmlMappings = "org/hibernate/orm/test/version/sybase/User.hbm.xml")
@SessionFactory
public class SybaseTimestampVersioningTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testLocking(SessionFactoryScope factoryScope) {
		// First, create the needed row...
		factoryScope.inTransaction( (s) -> {
			var steve = new User( 1, "steve" );
			s.persist( steve );
		} );

		// next open two sessions, and try to update from each "simultaneously"...
		try {
			factoryScope.inTransaction( (s1) -> {
				var steve1 = s1.find( User.class, 1 );

				factoryScope.inTransaction( (s2) -> {
					var steve2 = s2.find( User.class, 1 );
					steve2.setUsername( "steve-e" );
				} );

				// this one should cause a failure during flush
				steve1.setUsername( "se" );
			} );
			Assertions.fail( "Should have thrown an OptimisticLockException" );
		}
		catch (OptimisticLockException expected) {
		}
	}

	@Test
	public void testCollectionVersion(SessionFactoryScope factoryScope) {
		var first = factoryScope.fromTransaction( (s) -> {
			var steve = new User( 1, "steve" );
			s.persist( steve );
			var admin = new Group( 1, "admin" );
			s.persist( admin );
			return steve;
		} );

		sleep();

		var second = factoryScope.fromTransaction( (s) -> {
			var steve = s.find( User.class, 1 );
			var admin = s.find( Group.class, 1 );
			steve.getGroups().add( admin );
			admin.getUsers().add( steve );
			return steve;
		} );

		// since this collection is mapped as excluded from
		// optimistic locking, the version should not change
		Assertions.assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( first.getTimestamp(), second.getTimestamp() ),
				"owner version unexpectedly incremented" );

		sleep();

		var third = factoryScope.fromTransaction( (s) -> {
			var steve = s.find( User.class, 1 );
			steve.getGroups().clear();
			return steve;
		} );

		// as per discussion before, the version should not change
		Assertions.assertTrue( PrimitiveByteArrayJavaType.INSTANCE.areEqual( second.getTimestamp(), third.getTimestamp() ),
				"owner version unexpectedly incremented" );
	}


	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCollectionNoVersion(SessionFactoryScope factoryScope) {
		var first = factoryScope.fromTransaction( (s) -> {
			User steve = new User( 1, "steve" );
			s.persist( steve );
			Permission perm = new Permission( 1, "silly", "user", "rw" );
			s.persist( perm );
			return steve;
		} );

		sleep();

		var second = factoryScope.fromTransaction( (s) -> {
			var steve = s.find( User.class, 1 );
			var perm = s.find( Permission.class, 1 );
			steve.getPermissions().add( perm );
			return steve;
		} );

		// since this collection *is* included in optimistic locking,
		// this should trigger an increment of the version
		Assertions.assertTrue(
				PrimitiveByteArrayJavaType.INSTANCE.areEqual( first.getTimestamp(), second.getTimestamp() ),
				"owner version was incremented" );

		sleep();

		var third = factoryScope.fromTransaction( (s) -> {
			var steve = s.find( User.class, 1 );
			steve.getPermissions().clear();
			return steve;
		} );

		Assertions.assertTrue(
				PrimitiveByteArrayJavaType.INSTANCE.areEqual( second.getTimestamp(), third.getTimestamp() ),
				"owner version was incremented" );
	}

	private static void sleep() {
		sleep( 200 );
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ignored) {
		}
	}

	@Test
	@JiraKey( value = "HHH-10413" )
	public void testComparableTimestamps(SessionFactoryScope factoryScope) {
		final BasicType<?> versionType = factoryScope
				.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(User.class.getName())
				.getVersionType();
		Assertions.assertInstanceOf( PrimitiveByteArrayJavaType.class, versionType.getJavaTypeDescriptor() );
		Assertions.assertInstanceOf( VarbinaryJdbcType.class, versionType.getJdbcType() );

		var first = factoryScope.fromTransaction( (s) -> {
			var user = new User( 1, "n" );
			s.persist( user );
			return user;
		} );

		sleep( 2000 );

		var second = factoryScope.fromTransaction( (s) -> {
			var u = s.find( User.class, 1 );
			u.setUsername( "x" );
			return u;
		} );

		Assertions.assertTrue( versionType.compare( first.getTimestamp(), second.getTimestamp() ) < 0 );
	}
}
