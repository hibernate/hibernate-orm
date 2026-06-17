/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.options;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.CacheMode;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class CacheModeTests {
	@Test
	public void testNullCacheMode(SessionFactoryScope scope) {
		// tests passing null as CacheMode
		scope.inTransaction( (session) -> {
			session.createQuery( "select c from Contact c" )
					.setCacheMode( null )
					.list();
		});
	}

	@Test
	public void testCacheModeGettersUseInheritedSessionCacheMode(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.GET );

			final var query = session.createQuery( "select c from Contact c" );

			assertEquals( CacheMode.GET, query.getCacheMode() );
			assertEquals( CacheRetrieveMode.USE, query.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.BYPASS, query.getCacheStoreMode() );
		} );
	}

	@Test
	public void testSetJpaCacheRetrieveModeOnQueryInheritingSessionCacheMode(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.NORMAL );

			final var query = session.createQuery( "select c from Contact c" );
			query.setCacheRetrieveMode( CacheRetrieveMode.BYPASS );

			assertEquals( CacheMode.PUT, query.getCacheMode() );
			assertEquals( CacheRetrieveMode.BYPASS, query.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.USE, query.getCacheStoreMode() );
		} );
	}

	@Test
	public void testSetJpaCacheStoreModeOnQueryInheritingSessionCacheMode(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.NORMAL );

			final var query = session.createQuery( "select c from Contact c" );
			query.setCacheStoreMode( CacheStoreMode.BYPASS );

			assertEquals( CacheMode.GET, query.getCacheMode() );
			assertEquals( CacheRetrieveMode.USE, query.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.BYPASS, query.getCacheStoreMode() );
		} );
	}
}
