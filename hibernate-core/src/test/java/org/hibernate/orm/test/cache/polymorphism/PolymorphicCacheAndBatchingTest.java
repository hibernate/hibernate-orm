/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.polymorphism;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: when batch fetching a single id this might not happen since in some cases
 * we use a single ID entity loader that does not read the result from the PC but
 * directly from the result of the query.
 * <p>
 * With multi-load, however, the problem is always apparent.
 *
 * @author Marco Belladelli
 */
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16565" )
public class PolymorphicCacheAndBatchingTest extends PolymorphicCacheTest {
	@Test
	public void testMultiLoad(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();

		assertThat( cache.containsEntity( CachedItem1.class, 1 ) ).isTrue();
		assertThat( cache.containsEntity( CachedItem2.class, 2 ) ).isTrue();

		// test accessing the wrong class by id with a cache-hit
		scope.inTransaction( session -> {
			final List<CachedItem2> resultList = session.byMultipleIds( CachedItem2.class )
					.with( CacheMode.NORMAL )
					.enableSessionCheck( true )
					.multiLoad( 1, 2 );
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ) ).isNull();
			assertThat( resultList.get( 1 ).getName() ).isEqualTo( "name 2" );
		} );

		// test accessing the wrong class by id with no cache-hit
		cache.evictEntityData();
		scope.inTransaction( (session) -> {
			final List<CachedItem2> resultList = session.byMultipleIds( CachedItem2.class )
					.with( CacheMode.NORMAL )
					.enableSessionCheck( true )
					.multiLoad( 1, 2 );
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ) ).isNull();
			assertThat( resultList.get( 1 ).getName() ).isEqualTo( "name 2" );
		} );
	}
}
