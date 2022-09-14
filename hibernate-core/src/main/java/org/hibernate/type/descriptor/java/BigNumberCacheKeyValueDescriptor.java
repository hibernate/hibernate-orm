/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.cache.internal.CacheKeyValueDescriptor;

final class BigNumberCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	static final BigNumberCacheKeyValueDescriptor INSTANCE = new BigNumberCacheKeyValueDescriptor();

	@Override
	public int getHashCode(Object key) {
		return ( (Number) key ).intValue();
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		//noinspection unchecked
		return ( key1 == key2 ) || ( key1 != null && key2 != null && ( (Comparable<Object>) key1 ).compareTo( key2 ) == 0 );
	}
}
