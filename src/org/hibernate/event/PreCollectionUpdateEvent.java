//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs before a collection is updated
 *
 * @author Gail Badner
 */
public class PreCollectionUpdateEvent extends AbstractCollectionEvent {

	public PreCollectionUpdateEvent(PersistentCollection collection, EventSource source) {
		super(collection, source, getLoadedOwner( collection, source ));
	}
}
