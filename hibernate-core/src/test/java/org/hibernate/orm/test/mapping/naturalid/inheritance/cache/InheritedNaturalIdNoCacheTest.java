/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.cache;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ) )
@DomainModel( annotatedClasses = { MyEntity.class, ExtendedEntity.class } )
@SessionFactory( )
public class InheritedNaturalIdNoCacheTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.persist( new MyEntity( 1, "base" ) );
					session.persist( new ExtendedEntity( 2, "extended", "ext" ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoadRoot(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final MyEntity myEntity = session
					.byNaturalId( MyEntity.class )
					.using( "uid", "base" )
					.load();
			assertThat( myEntity ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			final MyEntity myEntity = session
					.bySimpleNaturalId( MyEntity.class )
					.load( "base" );
			assertThat( myEntity ).isNotNull();
		} );
	}

	@Test
	public void testLoadSubclass(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final ExtendedEntity extendedEntity = session
					.byNaturalId( ExtendedEntity.class )
					.using( "uid", "extended" )
					.load();
			assertThat( extendedEntity ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			final ExtendedEntity extendedEntity = session
					.bySimpleNaturalId( ExtendedEntity.class )
					.load( "extended" );
			assertThat( extendedEntity ).isNotNull();
		} );

		scope.inTransaction( (session) -> {
			final MyEntity myEntity = session
					.bySimpleNaturalId( MyEntity.class )
					.load( "extended" );
			assertThat( myEntity ).isNotNull();
		} );
	}

	@Test
	public void testLoadWrongClassById(SessionFactoryScope scope) {
		// try to access the root (base) entity as subclass (extended)
		//		- the outcome is different here depending on whether:
		//			1) caching is enabled && the natural-id resolution is cached -> WrongClassException
		//			2) otherwise -> return null
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.find( ExtendedEntity.class, 1 );
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	public void testLoadWrongClassByNaturalId(SessionFactoryScope scope) {
		// try to access the root (base) entity as subclass (extended)
		//		- the outcome is different here depending on whether:
		//			1) caching is enabled && the natural-id resolution is cached -> WrongClassException
		//			2) otherwise -> return null
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.bySimpleNaturalId( ExtendedEntity.class ).load( "base" );
			assertThat( loaded ).isNull();
		} );
	}

}
