/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cacheable.api;

import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.cache.CachingRegionFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class JpaCacheApiUsageTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Order.class };
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
//		options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		options.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
//		options.put( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );
		options.put( org.hibernate.jpa.AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.ALL );
	}

	@Test
	public void testEviction() {
		// first create an Order
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Order( 1, 500 ) );
		em.getTransaction().commit();
		em.close();

		assertTrue( entityManagerFactory().getCache().contains( Order.class, 1 ) );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		assertTrue( entityManagerFactory().getCache().contains( Order.class, 1 ) );
		em.createQuery( "delete Order" ).executeUpdate();
		em.getTransaction().commit();
		em.close();

		assertFalse( entityManagerFactory().getCache().contains( Order.class, 1 ) );
	}
}
