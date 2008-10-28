package org.hibernate.cfg.annotations;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;

/**
 * Bind a bag.
 *
 * @author Matthew Inger
 */
public class BagBinder extends CollectionBinder {

	public BagBinder() {
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Bag( persistentClass );
	}
}
