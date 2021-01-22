/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cacheable.cachemodes;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = { SimpleEntity.class }
)
public class SharedCacheModesTest {

	@Test
	public void testEntityManagerCacheModes(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					// defaults...
					assertEquals( CacheStoreMode.USE, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheRetrieveMode.USE, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ) );
					assertEquals( CacheMode.NORMAL, session.getCacheMode() );

					// overrides...
					session.setProperty( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
					assertEquals( CacheStoreMode.REFRESH, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.REFRESH, session.getCacheMode() );

					session.setProperty( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
					assertEquals( CacheStoreMode.BYPASS, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.GET, session.getCacheMode() );

					session.setProperty( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS );
					assertEquals(CacheRetrieveMode.BYPASS, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ) );
					assertEquals( CacheMode.IGNORE, session.getCacheMode() );

					session.setProperty( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
					assertEquals( CacheStoreMode.USE, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.PUT, session.getCacheMode() );

					session.setProperty( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
					assertEquals( CacheStoreMode.REFRESH, session.getProperties().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.REFRESH, session.getCacheMode() );
				}
		);
	}

	@Test
	public void testQueryCacheModes(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					org.hibernate.query.Query query = session.createQuery( "from SimpleEntity" );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
					assertEquals( CacheStoreMode.USE, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.NORMAL, query.getCacheMode() );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
					assertEquals( CacheStoreMode.BYPASS, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.GET, query.getCacheMode() );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
					assertEquals( CacheStoreMode.REFRESH, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.REFRESH, query.getCacheMode() );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS );
					assertEquals( CacheRetrieveMode.BYPASS, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ) );
					assertEquals( CacheStoreMode.REFRESH, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.REFRESH, query.getCacheMode() );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
					assertEquals( CacheRetrieveMode.BYPASS, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ) );
					assertEquals( CacheStoreMode.BYPASS, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.IGNORE, query.getCacheMode() );

					query.setHint( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
					assertEquals( CacheRetrieveMode.BYPASS, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ) );
					assertEquals( CacheStoreMode.USE, query.getHints().get( AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ) );
					assertEquals( CacheMode.PUT, query.getCacheMode() );
				}
		);
	}
}
