//$Id: CollectionCacheEntry.java 6838 2005-05-20 19:50:07Z oneovthafew $
package org.hibernate.cache.entry;

import java.io.Serializable;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.util.ArrayHelper;

/**
 * @author Gavin King
 */
public class CollectionCacheEntry implements Serializable {

	private final Serializable state;
	
	public Serializable[] getState() {
		//TODO: assumes all collections disassemble to an array!
		return (Serializable[]) state;
	}

	public CollectionCacheEntry(PersistentCollection collection, CollectionPersister persister) {
		this.state = collection.disassemble(persister);
	}
	
	CollectionCacheEntry(Serializable state) {
		this.state = state;
	}
	
	public void assemble(
		final PersistentCollection collection, 
		final CollectionPersister persister,
		final Object owner
	) {
		collection.initializeFromCache(persister, state, owner);
		collection.afterInitialize();
	}
	
	public String toString() {
		return "CollectionCacheEntry" + ArrayHelper.toString( getState() );
	}

}
