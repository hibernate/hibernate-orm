//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs before a collection is recreated
 *
 * @author Gail Badner
 */
public class PreCollectionRecreateEvent extends AbstractCollectionEvent {

	public PreCollectionRecreateEvent(PersistentCollection collection, EventSource source) {
		super(collection, source, collection.getOwner());
	}
}
