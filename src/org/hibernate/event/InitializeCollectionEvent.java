//$Id$
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs when a collection wants to be
 * initialized
 * 
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractCollectionEvent {

	public InitializeCollectionEvent(PersistentCollection collection, EventSource source) {
		super(collection, source, getLoadedOwner( collection, source ) );
	}
}
