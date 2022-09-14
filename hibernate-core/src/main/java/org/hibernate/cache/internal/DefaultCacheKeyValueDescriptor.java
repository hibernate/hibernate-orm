/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.util.Objects;

public class DefaultCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	public static final DefaultCacheKeyValueDescriptor INSTANCE = new DefaultCacheKeyValueDescriptor();

	@Override
	public int getHashCode(Object key) {
		return key.hashCode();
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		return Objects.equals( key1, key2 );
	}
}
