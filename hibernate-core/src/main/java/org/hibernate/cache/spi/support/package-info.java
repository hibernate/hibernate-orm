/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package provides a framework intended to reduce the work needed to implement
 * a caching provider. It takes care of most of the "grunt work" associated with the
 * implementation, leaving the integrator to implement the interfaces
 * {@link org.hibernate.cache.spi.support.StorageAccess} and
 * {@link org.hibernate.cache.spi.support.DomainDataStorageAccess}.
 * <p>
 * A typical integration would provide:
 * <ol>
 * <li>a custom {@code StorageAccess} or {@code DomainDataStorageAccess}, along with
 * <li>a custom {@link org.hibernate.cache.spi.support.RegionFactoryTemplate}.
 * </ol>
 * <p>
 * The preferred way to register these implementations to Hibernate is via a custom
 * {@link org.hibernate.boot.registry.selector.StrategyRegistrationProvider}.
 * <p>
 * Examples of using this support package to implement a caching provider include:
 * <ul>
 * <li>{@code org.hibernate.testing.cache.CachingRegionFactory} in {@code hibernate-testing}, and
 * <li>{@code org.hibernate.cache.jcache.internal.JCacheRegionFactory} in {@code hibernate-jcache}.
 * </ul>
 */
package org.hibernate.cache.spi.support;
