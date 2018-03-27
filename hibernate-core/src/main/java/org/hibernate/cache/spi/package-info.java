/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Defines the integration aspect of Hibernate's second-level
 * caching allowing "caching back ends" to be plugged in as
 * a caching provider.
 *
 * {@link org.hibernate.cache.spi.RegionFactory} is the main
 * integration contract that defines how Hibernate accesses
 * the provider.  It's main contract is the generation of
 * {@link org.hibernate.cache.spi.Region} references with the
 * requested intent (what will be stored there).
 *
 * Generally a provider will integrate with Hibernate by:
 *
 * 		1. implementing the contracts in {@link org.hibernate.cache.spi}
 * 		2. implementing the contracts in {@link org.hibernate.cache.spi.support}
 * 		3. a mix of (1) and (2)
 *
 * The first approach allows for more control of the set up, but also requires more
 * to implement.  The second approach tries to minimize the amount of work needed
 * to integrate with caching providers to basically the
 * {@link org.hibernate.cache.spi.support.StorageAccess} and
 * {@link org.hibernate.cache.spi.support.DomainDataStorageAccess} contracts which
 * are basic read/write type abstractions of the underlying "cache" object - it
 * is a nearly complete implementation aside from providing the proper "storage
 * access" objects.
 *
 * Note: providers may also integrate with Hibernate via
 * Hibernate's JCache support as defined by the `hibernate-jcache`
 * module - no code involved aside from being a JCache implementation
 * properly registered via the JCache spec.
 */
package org.hibernate.cache.spi;
