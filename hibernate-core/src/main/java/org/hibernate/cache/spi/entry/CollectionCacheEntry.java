/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Cacheable representation of persistent collections
 *
 * @author Gavin King
 */
public class CollectionCacheEntry implements Serializable {
	private final Serializable state;

	/**
	 * Constructs a CollectionCacheEntry
	 */
	@SuppressWarnings("unchecked")
	public CollectionCacheEntry(PersistentCollection collection, PersistentCollectionDescriptor collectionDescriptor) {
		this.state = collection.disassemble( collectionDescriptor );
	}

	CollectionCacheEntry(Serializable state) {
		this.state = state;
	}

	/**
	 * Retrieve the cached collection state.
	 *
	 * @return The cached collection state.
	 */
	public Serializable[] getState() {
		//TODO: assumes all collections disassemble to an array!
		return (Serializable[]) state;
	}

	/**
	 * Assembles the collection from the cached state.
	 */
	@SuppressWarnings("unchecked")
	public void assemble(
			final PersistentCollection collection,
			final PersistentCollectionDescriptor collectionDescriptor,
			final Object owner) {
		collection.initializeFromCache( state, owner, collectionDescriptor );
		collection.afterInitialize();
	}

	@Override
	public String toString() {
		return "CollectionCacheEntry" + ArrayHelper.toString( getState() );
	}

}
