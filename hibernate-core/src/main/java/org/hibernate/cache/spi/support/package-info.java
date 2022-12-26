/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package intended for simplifying the worked needed to implement a caching provider.
 * Centers around the concept of {@link org.hibernate.cache.spi.support.StorageAccess}
 * and {@link org.hibernate.cache.spi.support.DomainDataStorageAccess} to implement
 * most of the "grunt work" associated with the implementation.
 * <p>
 * A typical integration would just implement:
 * <ol>
 * <li>a custom {@code StorageAccess}/{@code DomainDataStorageAccess}, along with
 * <li>a custom {@link org.hibernate.cache.spi.support.RegionFactoryTemplate}, in
 *     particular:
 *     <ul>
 *     <li>{@link org.hibernate.cache.spi.support.RegionFactoryTemplate#createDomainDataStorageAccess}
 *     <li>{@link org.hibernate.cache.spi.support.RegionFactoryTemplate#createQueryResultsRegionStorageAccess}
 *     <li>{@link org.hibernate.cache.spi.support.RegionFactoryTemplate#createTimestampsRegionStorageAccess}
 *     </ul>
 * </ol>
 * <p>
 * Voila! Functioning cache provider.
 * <p>
 * The preferred approach to "provide an integration" is through a custom
 * {@link org.hibernate.boot.registry.selector.StrategyRegistrationProvider}.
 * <p>
 * Both {@code hibernate-testing} ({@code org.hibernate.testing.cache.CachingRegionFactory})
 * and {@code hibernate-jcache} ({@code org.hibernate.cache.jcache.internal.JCacheRegionFactory})
 * provide examples of using this support package to implement a caching
 * provider.
 */
package org.hibernate.cache.spi.support;
