/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
