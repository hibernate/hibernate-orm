/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Identifies policies for managing concurrent access to the shared
 * second-level cache.
 * <p>
 * A second-level cache is shared between all concurrent active
 * sessions created by a given session factory. The cache shares
 * state between transactions, while bypassing the database's
 * locking or multi-version concurrency control. This tends to
 * undermine the ACID properties of transaction processing, which
 * are only guaranteed when all sharing of data is mediated by the
 * database.
 * <p>
 * Of course, as a general rule, the only sort of data that really
 * belongs in a second-level cache is data that is both:
 * <ul>
 * <li>read extremely frequently, and
 * <li>written infrequently.
 * </ul>
 * <p>
 * When an entity or collection is marked {@linkplain Cache cacheable},
 * it must indicate the policy which governs concurrent access to its
 * second-level cache, by selecting a {@code CacheConcurrencyStrategy}
 * appropriate to the expected patterns of data access. The most
 * important consideration is the frequency of updates which mutate
 * the state of the cached entity or collection.
 * <p>
 * For example, if an entity is immutable, {@link #READ_ONLY} is the
 * most appropriate policy, and the entity should be annotated
 * {@code @Cache(usage=READ_ONLY)}.
 *
 * @author Emmanuel Bernard
 *
 * @see AccessType The corresponding SPI.
 */
public enum CacheConcurrencyStrategy {
	/**
	 * Indicates that no concurrency strategy is specified, and
	 * that a default strategy should be used.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 */
	NONE,

	/**
	 * Read-only access to the shared second-level cache.
	 * <p>
	 * Indicates that the cached object is immutable, and is
	 * never updated. If an entity with this cache concurrency
	 * is updated, an exception is thrown. This is the simplest,
	 * safest, and best-performing cache concurrency strategy.
	 * It's particularly suitable for so-called "reference" data.
	 *
	 * @see AccessType#READ_ONLY
	 */
	READ_ONLY,

	/**
	 * Read/write access to the shared second-level cache with no
	 * locking.
	 * <p>
	 * Indicates that the cached object is sometimes updated, but
	 * that it is <em>extremely</em> unlikely that two transactions
	 * will attempt to update the same item of data at the same
	 * time. This strategy does not use locks. When an item is
	 * updated, the cache is invalidated both before and after
	 * completion of the updating transaction. But without locking,
	 * it's impossible to completely rule out the possibility of a
	 * second transaction storing or retrieving stale data in or
	 * from the cache during the completion process of the first
	 * transaction.
	 * <p>
	 * This concurrency strategy is not compatible with
	 * serializable transaction isolation.
	 *
	 * @see AccessType#NONSTRICT_READ_WRITE
	 */
	NONSTRICT_READ_WRITE,

	/**
	 * Read/write access to the shared second-level cache using
	 * soft locks.
	 * <p>
	 * Indicates a non-vanishing likelihood that two concurrent
	 * transactions attempt to update the same item of data
	 * simultaneously. This strategy uses "soft" locks to prevent
	 * concurrent transactions from retrieving or storing a stale
	 * item from or in the cache during the transaction completion
	 * process. A soft lock is simply a marker entry placed in the
	 * cache while the updating transaction completes.
	 * <ul>
	 * <li>A second transaction may not read the item from the
	 *     cache while the soft lock is present, and instead
	 *     simply proceeds to read the item directly from the
	 *     database, exactly as if a regular cache miss had
	 *     occurred.
	 * <li>Similarly, the soft lock also prevents this second
	 *     transaction from storing a stale item to the cache
	 *     when it returns from its round trip to the database
	 *     with something that might not quite be the latest
	 *     version.
	 * </ul>
	 * <p>
	 * This concurrency strategy is not compatible with
	 * serializable transaction isolation.
	 *
	 * @see AccessType#READ_WRITE
	 */
	READ_WRITE,

	/**
	 * Transactional access to the shared second-level cache.
	 * <p>
	 * Indicates that concurrent writes are common, and the only
	 * way to maintain synchronization between the second-level
	 * cache and the database is via the use of a fully
	 * transactional cache provider. In this case, the cache and
	 * the database must cooperate via JTA or the XA protocol,
	 * and Hibernate itself takes on little responsibility for
	 * maintaining the integrity of the cache.
	 *
	 * @see AccessType#TRANSACTIONAL
	 */
	TRANSACTIONAL;

	/**
	 * Get the {@link AccessType} corresponding to this concurrency strategy.
	 *
	 * @return The corresponding concurrency strategy. Note that this will
	 *         return {@code null} for {@link #NONE}.
	 */
	public AccessType toAccessType() {
		return switch (this) {
			case NONE -> null;
			case READ_ONLY -> AccessType.READ_ONLY;
			case NONSTRICT_READ_WRITE -> AccessType.NONSTRICT_READ_WRITE;
			case READ_WRITE -> AccessType.READ_WRITE;
			case TRANSACTIONAL -> AccessType.TRANSACTIONAL;
		};
	}

	/**
	 * Conversion from {@link AccessType} to {@link CacheConcurrencyStrategy}.
	 *
	 * @param accessType The access type to convert
	 *
	 * @return The corresponding enum value. {@link #NONE} is returned by
	 *         default if unable to recognize the {@code accessType} or
	 *         if the {@code accessType} is {@code null}.
	 */
	public static CacheConcurrencyStrategy fromAccessType(AccessType accessType) {
		if ( accessType == null ) {
			return NONE;
		}
		else {
			return switch (accessType) {
				case READ_ONLY -> READ_ONLY;
				case READ_WRITE -> READ_WRITE;
				case NONSTRICT_READ_WRITE -> NONSTRICT_READ_WRITE;
				case TRANSACTIONAL -> TRANSACTIONAL;
			};
		}
	}

	/**
	 * Parse an external representation.
	 *
	 * @param name The external representation
	 *
	 * @return The corresponding enum value, or {@code null} if no match was found.
	 */
	public static CacheConcurrencyStrategy parse(String name) {
		for ( CacheConcurrencyStrategy strategy : values() ) {
			if ( strategy.isMatch( name) ) {
				return strategy;
			}
		}
		return null;
	}

	private boolean isMatch(String name) {
		final AccessType accessType = toAccessType();
		return accessType != null && accessType.getExternalName().equalsIgnoreCase( name )
			|| name().equalsIgnoreCase( name );
	}
}
