/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package intended for simplifying the worked needed to implement
 * a caching provider.  Centers around the concept of
 * {@link org.hibernate.cache.spi.support.StorageAccess} and
 * {@link org.hibernate.cache.spi.support.DomainDataStorageAccess}
 * too implement most of the "grunt work" associated with the
 * implementation.
 *
 * Most integrations would just:
 *
 * 		1. implement a custom StorageAccess/DomainDataStorageAccess
 * 		2. implement a custom RegionFactoryTemplate, implementing specifically:
 * 			a. `RegionFactoryTemplate#createDomainDataStorageAccess`
 * 			b. `RegionFactoryTemplate#createQueryResultsRegionStorageAccess`
 * 			c. `RegionFactoryTemplate#createTimestampsRegionStorageAccess`
 *
 * Voila.. functioning cache provider
 *
 * The preferred approach to "provide a integration" is through a custom
 * {@link org.hibernate.boot.registry.selector.StrategyRegistrationProvider}
 *
 * Both `hibernate-testing` (`org.hibernate.testing.cache.CachingRegionFactory`)
 * and `hibernate-jcache` (`org.hibernate.cache.jcache.internal.JCacheRegionFactory`)
 * provide examples of using this support package to implement a caching
 * provider.
 */
package org.hibernate.cache.spi.support;
