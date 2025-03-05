/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache;

import java.io.Serializable;

import org.hibernate.cache.spi.QueryResultsCache;

/**
 * A builder that generates a Serializable Object to be used as a key into the {@linkplain QueryResultsCache
 * query results cache}.
 */

public interface MutableCacheKeyBuilder extends Serializable {

	void addValue(Object value);


	void addHashCode(int hashCode);

	/**
	 *  creates an Object to be used as a key into the {@linkplain QueryResultsCache
	 *  query results cache}.
	 */
	Serializable build();

}
