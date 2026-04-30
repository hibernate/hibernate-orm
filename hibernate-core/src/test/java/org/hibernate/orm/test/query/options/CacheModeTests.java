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
	public void testJpaCacheModesClearRefreshSession(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final var query = session.createQuery( "select c from Contact c" )
					.setCacheMode( CacheMode.REFRESH_SESSION );

			assertEquals( CacheMode.REFRESH_SESSION, query.getCacheMode() );

			query.setCacheRetrieveMode( CacheRetrieveMode.BYPASS );

			assertEquals( CacheMode.REFRESH, query.getCacheMode() );
			assertEquals( CacheRetrieveMode.BYPASS, query.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.REFRESH, query.getCacheStoreMode() );
		} );
	}
}
