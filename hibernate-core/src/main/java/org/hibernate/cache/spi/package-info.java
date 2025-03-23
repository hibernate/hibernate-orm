/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines the integration aspect of Hibernate's second-level caching, allowing
 * a "caching backend" to be plugged in as a cache provider.
 * <p>
 * {@link org.hibernate.cache.spi.RegionFactory} is the main integration contract
 * that defines how Hibernate interacts with the provider. Its main contract is
 * the generation of {@link org.hibernate.cache.spi.Region} references with some
 * requested intent (what will be stored there).
 * <p>
 * A provider will integrate with Hibernate by implementing either:
 * <ol>
 * <li>the contracts in {@link org.hibernate.cache.spi},
 * <li>the contracts in {@link org.hibernate.cache.spi.support}, or
 * <li>a mix of the above.
 * </ol>
 * The first approach allows for more control over the setup, but also requires
 * more work. The second approach minimizes the work needed to integrate with a
 * cache provider: the integrator is only required to implement the
 * contracts {@link org.hibernate.cache.spi.support.StorageAccess} and
 * {@link org.hibernate.cache.spi.support.DomainDataStorageAccess}, which are
 * basic read/write abstractions of the underlying cache. That is to say,
 * {@code org.hibernate.cache.spi.support} comes with a nearly complete
 * implementation, except for these "storage access" objects.
 * <p>
 * Alternatively, providers may integrate with Hibernate via Hibernate's support
 * for JCache, which is defined by the {@code hibernate-jcache} module. No custom
 * code is necessary, just an implementation of JCache, properly registered via
 * the JCache spec.
 */
package org.hibernate.cache.spi;
