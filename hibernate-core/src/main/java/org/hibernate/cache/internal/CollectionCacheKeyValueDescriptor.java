/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.collection.spi.PersistentCollection;

public class CollectionCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	public static final CollectionCacheKeyValueDescriptor INSTANCE = new CollectionCacheKeyValueDescriptor();

	@Override
	public int getHashCode(Object key) {
		return key.hashCode();
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		return key1 == key2
				|| ( key1 instanceof PersistentCollection && isEqual( (PersistentCollection<?>) key1, key2 ) )
				|| ( key2 instanceof PersistentCollection && isEqual( (PersistentCollection<?>) key2, key1 ) );
	}

	private boolean isEqual(PersistentCollection<?> x, Object y) {
		return x.wasInitialized() && ( x.isWrapper( y ) || x.isDirectlyProvidedCollection( y ) );
	}
}
