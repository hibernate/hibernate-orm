/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.cache;

import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@DomainModel( annotatedClasses = { MyEntity.class, ExtendedEntity.class } )
@SessionFactory
public class InheritedNaturalIdCacheTest {

	@Test
	public void testProperLoadingByNaturalId(SessionFactoryScope scope) {
		// load the entities properly by natural-id
		scope.inTransaction(
				(session) -> {
					final MyEntity base = session.bySimpleNaturalId( MyEntity.class ).load( "base" );
					assertThat( base ).isNotNull();

					final ExtendedEntity extended = session.bySimpleNaturalId( ExtendedEntity.class ).load( "extended" );
					assertThat( extended ).isNotNull();
				}
		);
	}

	@Test
	public void testLoadWrongClassById(SessionFactoryScope scope) {
		clearCaches( scope );

		// try to load `MyEntity#1` as an ExtendedEntity
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.byId( ExtendedEntity.class ).load( 1 );
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	public void testLoadWrongClassByIdFromCache(SessionFactoryScope scope) {
		clearCaches( scope );

		// load `MyEntity#1` into the cache
		scope.inTransaction( (session) -> {
			final MyEntity loaded = session.byId( MyEntity.class ).load( 1 );
			assertThat( loaded ).isNotNull();
		} );

		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( MyEntity.class, 1 ) ).isTrue();

		// now try to access it as an ExtendedEntity
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.byId( ExtendedEntity.class ).load( 1 );
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	public void testLoadWrongClassByNaturalId(SessionFactoryScope scope) {
		clearCaches( scope );

		// try to load `MyEntity#1` as an ExtendedEntity
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.bySimpleNaturalId( ExtendedEntity.class ).load( "base" );
			assertThat( loaded ).isNull();
		} );
	}

	@Test
	public void testLoadWrongClassByNaturalIdFromCache(SessionFactoryScope scope) {
		clearCaches( scope );

		// load `MyEntity#1` into the cache
		scope.inTransaction( (session) -> {
			final MyEntity loaded = session.bySimpleNaturalId( MyEntity.class ).load( "base" );
			assertThat( loaded ).isNotNull();
		} );

		final CacheImplementor cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( MyEntity.class, 1 ) ).isTrue();

		// now try to access it as an ExtendedEntity
		scope.inTransaction( (session) -> {
			final ExtendedEntity loaded = session.bySimpleNaturalId( ExtendedEntity.class ).load( "base" );
			assertThat( loaded ).isNull();
		} );
	}


	private void clearCaches(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final CacheImplementor cache = sessionFactory.getCache();

		cache.evictEntityData( MyEntity.class );
		cache.evictEntityData( ExtendedEntity.class );

		cache.evictNaturalIdData( MyEntity.class );
		cache.evictNaturalIdData( ExtendedEntity.class );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		// create the data:
		//		MyEntity#1
		//		ExtendedEntity#2
		scope.inTransaction(
				(session) -> {
					session.persist( new MyEntity( 1, "base" ) );
					session.persist( new ExtendedEntity( 2, "extended", "ext" ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}
}
