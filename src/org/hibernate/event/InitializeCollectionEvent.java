//$Id$
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs when a collection wants to be
 * initialized
 * 
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractEvent {
	
	private final PersistentCollection collection;

	public InitializeCollectionEvent(PersistentCollection collection, EventSource source) {
		super(source);
		this.collection = collection;
	}
	
	public PersistentCollection getCollection() {
		return collection;
	}
}
