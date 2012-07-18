/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
