/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cacheable.cachemodes;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.internal.AbstractQueryImpl;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class SharedCacheModesTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Test
	public void testEntityManagerCacheModes() {

		EntityManager em;
		Session session;

		em = getOrCreateEntityManager();
		session = ( (HibernateEntityManager) em ).getSession();

		// defaults...
		assertEquals( CacheStoreMode.USE, em.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheRetrieveMode.USE, em.getProperties().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) );
		assertEquals( CacheMode.NORMAL, session.getCacheMode() );

		// overrides...
		em.setProperty( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
		assertEquals( CacheStoreMode.REFRESH, em.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.REFRESH, session.getCacheMode() );

		em.setProperty( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
		assertEquals( CacheStoreMode.BYPASS, em.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.GET, session.getCacheMode() );

		em.setProperty( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS );
		assertEquals( CacheRetrieveMode.BYPASS, em.getProperties().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) );
		assertEquals( CacheMode.IGNORE, session.getCacheMode() );

		em.setProperty( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
		assertEquals( CacheStoreMode.USE, em.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.PUT, session.getCacheMode() );

		em.setProperty( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
		assertEquals( CacheStoreMode.REFRESH, em.getProperties().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.REFRESH, session.getCacheMode() );
	}

	@Test
	public void testQueryCacheModes() {
		EntityManager em = getOrCreateEntityManager();
		Query jpaQuery = em.createQuery( "from SimpleEntity" );
		AbstractQueryImpl hibQuery = (AbstractQueryImpl) ( (HibernateQuery) jpaQuery ).getHibernateQuery();

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
		assertEquals( CacheStoreMode.USE, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.NORMAL, hibQuery.getCacheMode() );

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
		assertEquals( CacheStoreMode.BYPASS, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.GET, hibQuery.getCacheMode() );

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.REFRESH );
		assertEquals( CacheStoreMode.REFRESH, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.REFRESH, hibQuery.getCacheMode() );

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS );
		assertEquals( CacheRetrieveMode.BYPASS, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) );
		assertEquals( CacheStoreMode.REFRESH, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.REFRESH, hibQuery.getCacheMode() );

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS );
		assertEquals( CacheRetrieveMode.BYPASS, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) );
		assertEquals( CacheStoreMode.BYPASS, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.IGNORE, hibQuery.getCacheMode() );

		jpaQuery.setHint( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheStoreMode.USE );
		assertEquals( CacheRetrieveMode.BYPASS, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) );
		assertEquals( CacheStoreMode.USE, jpaQuery.getHints().get( AvailableSettings.SHARED_CACHE_STORE_MODE ) );
		assertEquals( CacheMode.PUT, hibQuery.getCacheMode() );
	}

}
