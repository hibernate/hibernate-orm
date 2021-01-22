/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cacheable.api;

import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;

import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.NotImplementedYet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
// TODO Convert to annotation based testing? Setting the CachingRegionFactory as below leads to a CNFE
//@DomainModel(
//		annotatedClasses = Order.class
//)
//@ServiceRegistry(
//		settings = {
//				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "CachingRegionFactory.class"),
//				@Setting(name = AvailableSettings.JPA_SHARED_CACHE_MODE, value = "ALL")
//		}
//)
//@SessionFactory
public class JpaCacheApiUsageTest /*extends BaseEntityManagerFunctionalTestCase*/ {
//	@Override
//	protected Class<?>[] getAnnotatedClasses() {
//		return new Class[] { Order.class };
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	protected void addConfigOptions(Map options) {
////		options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
//		options.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
////		options.put( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );
//		options.put( org.hibernate.jpa.AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.ALL );
//	}

	@Test
	@NotImplementedYet(reason = "BulkOperationCleanupAction is never created", expectedVersion = "6.0")
	public void testEviction() {
		throw new NotYetImplementedFor6Exception();
		// first create an Order
//		EntityManager em = getOrCreateEntityManager();
//		em.getTransaction().begin();
//		em.persist( new Order( 1, 500 ) );
//		em.getTransaction().commit();
//		em.close();
//
//		assertTrue( entityManagerFactory().getCache().contains( Order.class, 1 ) );
//
//		em = getOrCreateEntityManager();
//		em.getTransaction().begin();
//		assertTrue( entityManagerFactory().getCache().contains( Order.class, 1 ) );
//		em.createQuery( "delete Order" ).executeUpdate();
//		em.getTransaction().commit();
//		em.close();
//
//		assertFalse( entityManagerFactory().getCache().contains( Order.class, 1 ) );
	}

//	@Test
//	public void testEviction(SessionFactoryScope scope) {
//		scope.inTransaction(
//				session -> {
//					session.getTransaction().begin();
//					session.persist( new Order( 1, 500 ) );
//					session.getTransaction().commit();
//					session.close();
//				}
//		);
//
//		scope.inSession(
//				session -> {
//					assertTrue( session.getEntityManagerFactory().getCache().contains( Order.class, 1 ) );
//				}
//		);
//
//		scope.inTransaction(
//				session -> {
//					session.getTransaction().begin();
//					assertTrue( session.getEntityManagerFactory().getCache().contains( Order.class, 1 ) );
//					session.createQuery( "delete Order" ).executeUpdate();
//					session.getTransaction().commit();
//					session.close();
//				}
//		);
//
//		scope.inSession(
//				session -> {
//					assertFalse( session.getEntityManagerFactory().getCache().contains( Order.class, 1 ) );
//				}
//		);
//	}
}
