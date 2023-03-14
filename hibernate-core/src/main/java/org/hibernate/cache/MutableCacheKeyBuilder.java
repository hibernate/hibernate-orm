/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
