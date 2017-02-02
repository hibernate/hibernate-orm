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
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Cacheable representation of persistent collections
 *
 * @author Gavin King
 */
public class CollectionCacheEntry implements Serializable {
	private final Serializable state;

	/**
	 * Constructs a CollectionCacheEntry
	 *
	 * @param collection The persistent collection instance
	 * @param persister The collection persister
	 */
	public CollectionCacheEntry(PersistentCollection collection, CollectionPersister persister) {
		this.state = collection.disassemble( persister );
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
	 *
	 * @param collection The persistent collection instance being assembled
	 * @param persister The collection persister
	 * @param owner The collection owner instance
	 */
	public void assemble(
			final PersistentCollection collection,
			final CollectionPersister persister,
			final Object owner) {
		collection.initializeFromCache( persister, state, owner );
		collection.afterInitialize();
	}

	@Override
	public String toString() {
		return "CollectionCacheEntry" + ArrayHelper.toString( getState() );
	}

}
