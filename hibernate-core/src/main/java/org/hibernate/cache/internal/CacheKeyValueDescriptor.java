/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

/**
 * Used to handle hash-code and equality check for cache key values
 *
 * @author Andrea Boriero
 */
public interface CacheKeyValueDescriptor extends Serializable {
	int getHashCode(Object key);
	boolean isEqual(Object key1, Object key2);
}
