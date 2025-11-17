/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

/**
 * Describes the data layout used for storing an object into the query cache.
 * <p>
 * The query cache can either store the full state of an entity or collection,
 * or store only the identifier and discriminator if needed. Both approaches have pros and cons,
 * but ultimately it's advisable to us a shallow layout if the chances are high,
 * that the object will be contained in the second level cache.
 *
 * @see QueryCacheLayout
 * @since 6.5
 */
@Incubating
public enum CacheLayout {
	/**
	 * Uses either {@link #SHALLOW} or {@link #FULL},
	 * depending on whether an entity or collection is second level cacheable.
	 */
	AUTO,

	/**
	 * Uses a cache layout that only stores the identifier of the entity or collection.
	 * This is useful when the chances for the object being part of the second level cache are very high.
	 */
	SHALLOW,

	/**
	 * Like {@link #SHALLOW} but will also store the entity discriminator.
	 */
	SHALLOW_WITH_DISCRIMINATOR,

	/**
	 * Stores the full state into the query cache.
	 * This is useful when the chances for the object being part of the second level cache are very low.
	 */
	FULL
}
