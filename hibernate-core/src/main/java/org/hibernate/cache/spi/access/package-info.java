/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines contracts for transactional and concurrent access to cached
 * {@linkplain org.hibernate.cache.spi.access.EntityDataAccess entity} and
 * {@linkplain org.hibernate.cache.spi.access.CollectionDataAccess collection} data.
 * <p>
 * Transactions pass in a timestamp indicating transaction start time which is then used to protect against concurrent
 * access. Exactly how that happens is based on the actual access-strategy implementation used.
 * <p>
 * Two different implementation patterns are provided for:
 * <ul>
 * <li>
 *     A transaction-aware cache implementation might be wrapped by a <em>synchronous</em> access strategy,
 *     where updates to the cache are written to the cache inside the transaction.
 * </li>
 * <li>
 *     A non-transaction-aware cache would be wrapped by an <em>asynchronous</em> access strategy, where items
 *     are merely "soft locked" during the transaction and then updated during the "after transaction completion"
 *     phase. The soft lock is not an actual lock on the database row, it only prevents access to the cached
 *     representation of the item.
 * </li>
 * </ul>
 * <p>
 * The <em>asynchronous</em> access strategies are:
 * {@linkplain org.hibernate.cache.spi.access.AccessType#READ_ONLY read-only},
 * {@linkplain org.hibernate.cache.spi.access.AccessType#READ_WRITE read-write} and
 * {@linkplain org.hibernate.cache.spi.access.AccessType#NONSTRICT_READ_WRITE nonstrict-read-write}.
 * The only <em>synchronous</em> access strategy is
 * {@linkplain org.hibernate.cache.spi.access.AccessType#TRANSACTIONAL transactional}.
 * <p>
 * Note that:
 * <ul>
 * <li>for an <em>asynchronous</em> cache, cache invalidation must be a two-step process ("lock" then "unlock", or
 *     "lock" then "after update"), since this is the only way to guarantee consistency with the database for a
 *     non-transactional cache implementation, but
 * <li>for a <em>synchronous</em> cache, cache invalidation may be performed in a single operation ("evict" or "update").
 * </ul>
 * Hence, the contracts {@link org.hibernate.cache.spi.access.EntityDataAccess} and
 * {@link org.hibernate.cache.spi.access.CollectionDataAccess} define a three-step process to allow for both models
 * (see the individual contracts for details).
 * <p>
 * Note that query result caching does not go through an access strategy; those caches are managed directly against
 * the underlying {@link org.hibernate.cache.spi.QueryResultsRegion}.
 */
package org.hibernate.cache.spi.access;
